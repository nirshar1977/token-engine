package com.tokenengine.gateway.internal;

import com.tokenengine.analyser.api.ContextAnalyser;
import com.tokenengine.analyser.api.WasteReport;
import com.tokenengine.optimiser.api.OptimisationResult;
import com.tokenengine.optimiser.api.Optimiser;
import com.tokenengine.session.api.SessionContext;
import com.tokenengine.session.api.SessionService;
import com.tokenengine.shared.api.DeveloperId;
import com.tokenengine.usage.api.RequestUsage;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Entry point for all LLM traffic. Pipeline for {@code POST /v1/messages}:
 * authenticate developer → session → analyse → optimise → stream to provider → record usage (async).
 * Every other path under {@code /v1} is proxied as-is.
 */
@Slf4j
@Hidden
@RestController
class ProxyController {

    static final String REQUEST_ID_HEADER = "x-token-engine-request-id";
    private static final String SESSION_HEADER = "x-claude-code-session-id";
    private static final String AGENT_HEADER = "x-claude-code-agent-id";
    private static final String REQUEST_CLASS_HEADER = "x-claude-code-request-class";

    private final UpstreamProxy proxy;
    private final DeveloperResolver developers;
    private final String developerKeyHeader;
    private final SessionService sessions;
    private final ContextAnalyser analyser;
    private final Optimiser optimiser;
    private final AsyncUsageRecorder usage;

    ProxyController(UpstreamProxy proxy, DeveloperResolver developers, GatewayProperties properties,
                    SessionService sessions, ContextAnalyser analyser, Optimiser optimiser,
                    AsyncUsageRecorder usage) {
        this.proxy = proxy;
        this.developers = developers;
        this.developerKeyHeader = properties.auth().header();
        this.sessions = sessions;
        this.analyser = analyser;
        this.optimiser = optimiser;
        this.usage = usage;
    }

    @RequestMapping({"/v1/**", "/api/hello"})
    void proxy(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Instant startedAt = Instant.now();
        UUID requestId = UUID.randomUUID();
        response.setHeader(REQUEST_ID_HEADER, requestId.toString());

        Optional<DeveloperId> developer = developers.resolve(request.getHeader(developerKeyHeader));
        if (developer.isEmpty()) {
            writeAuthError(response);
            return;
        }

        byte[] body = request.getInputStream().readAllBytes();
        String path = request.getRequestURI();
        String pathAndQuery = request.getQueryString() == null ? path : path + "?" + request.getQueryString();
        boolean messagesCall = "POST".equals(request.getMethod()) && "/v1/messages".equals(path);

        SessionContext session = messagesCall
                ? sessions.touch(developer.get(), request.getHeader(SESSION_HEADER)) : null;
        WasteReport waste = messagesCall ? analyser.analyse(body) : WasteReport.none();
        OptimisationResult optimised = messagesCall ? optimise(body, waste) : OptimisationResult.unchanged(body, "none");

        ProxyOutcome outcome = proxy.forward(
                new InboundRequest(request.getMethod(), pathAndQuery, headersOf(request), optimised.body()),
                new ServletResponseSink(response));

        if (messagesCall) {
            usage.recordLater(toUsage(requestId, startedAt, developer.get(), session, request,
                    body.length, optimised, waste, outcome));
        }
    }

    private OptimisationResult optimise(byte[] body, WasteReport waste) {
        try {
            return optimiser.optimise(body, waste);
        } catch (RuntimeException e) {
            log.warn("Optimiser failed, sending the original request: {}", e.toString());
            return OptimisationResult.unchanged(body, "failed-open");
        }
    }

    private static RequestUsage toUsage(UUID requestId, Instant startedAt, DeveloperId developer,
                                        SessionContext session, HttpServletRequest request, int bytesBefore,
                                        OptimisationResult optimised, WasteReport waste, ProxyOutcome outcome) {
        ObservedUsage u = outcome.usage();
        return RequestUsage.builder()
                .requestId(requestId)
                .startedAt(startedAt)
                .latencyMs(Duration.between(startedAt, Instant.now()).toMillis())
                .developer(developer)
                .sessionId(session.sessionId())
                .sessionRequestCount(session.requestCount())
                .agentId(request.getHeader(AGENT_HEADER))
                .requestClass(request.getHeader(REQUEST_CLASS_HEADER))
                .model(u.model())
                .status(outcome.status())
                .streamed(outcome.streamed())
                .complete(u.complete())
                .clientAborted(outcome.clientAborted())
                .upstreamRequestId(outcome.upstreamRequestId())
                .inputTokens(u.inputTokens())
                .outputTokens(u.outputTokens())
                .cacheWrite5mTokens(u.cacheWrite5mTokens())
                .cacheWrite1hTokens(u.cacheWrite1hTokens())
                .cacheReadTokens(u.cacheReadTokens())
                .requestBytesBefore(bytesBefore)
                .requestBytesAfter(optimised.body().length)
                .optimised(optimised.applied())
                .optimiserStrategy(optimised.strategy())
                .duplicateBlocks(waste.duplicateBlocks())
                .duplicateBytes(waste.duplicateBytes())
                .latestTurnDuplicateBytes(waste.latestTurnDuplicateBytes())
                .build();
    }

    private static Map<String, List<String>> headersOf(HttpServletRequest request) {
        Map<String, List<String>> headers = new LinkedHashMap<>();
        for (String name : Collections.list(request.getHeaderNames())) {
            headers.put(name, Collections.list(request.getHeaders(name)));
        }
        return headers;
    }

    private void writeAuthError(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setHeader("x-should-retry", "false");
        response.getOutputStream().write(("{\"type\":\"error\",\"error\":{\"type\":\"authentication_error\","
                + "\"message\":\"token-engine: missing or unknown " + developerKeyHeader + " header\"}}")
                .getBytes(StandardCharsets.UTF_8));
    }
}
