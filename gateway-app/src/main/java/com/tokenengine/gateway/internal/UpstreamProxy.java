package com.tokenengine.gateway.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import tools.jackson.databind.ObjectMapper;

/**
 * Streams a request to the provider and the response back, byte for byte, while a {@link UsageTap} reads usage.
 *
 * <p>Follows Claude Code's gateway compatibility guide: every {@code anthropic-*} and {@code x-claude-code-*}
 * header and the body are forwarded unchanged, error bodies are passed through untouched, and the response is
 * flushed chunk by chunk (buffering would stall Claude Code and strip the keep-alive pings).
 * Plain Java, so it runs on virtual threads and is testable without Spring.
 */
final class UpstreamProxy {

    /** Hop-by-hop headers, headers the JDK client sets itself, and accept-encoding so usage can be read. */
    private static final Set<String> NOT_FORWARDED_REQUEST = Set.of(
            "host", "connection", "keep-alive", "proxy-connection", "proxy-authorization", "te", "trailer",
            "transfer-encoding", "upgrade", "content-length", "expect", "accept-encoding", "http2-settings",
            "date", "from", "via", "warning");

    private static final Set<String> NOT_FORWARDED_RESPONSE = Set.of(
            "connection", "keep-alive", "proxy-authenticate", "te", "trailer", "transfer-encoding", "upgrade",
            "content-length");

    private static final int BUFFER_BYTES = 16 * 1024;

    private final HttpClient client;
    private final String baseUrl;
    private final Duration responseHeadersTimeout;
    private final Set<String> consumedHeaders;
    private final ObjectMapper mapper;

    /**
     * @param consumedHeaders headers meant for the gateway itself (like the developer key); never sent upstream
     */
    UpstreamProxy(HttpClient client, URI baseUrl, Duration responseHeadersTimeout,
                  Set<String> consumedHeaders, ObjectMapper mapper) {
        this.client = client;
        String base = baseUrl.toString();
        this.baseUrl = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
        this.responseHeadersTimeout = responseHeadersTimeout;
        this.consumedHeaders = consumedHeaders.stream()
                .map(h -> h.toLowerCase(Locale.ROOT)).collect(Collectors.toUnmodifiableSet());
        this.mapper = mapper;
    }

    ProxyOutcome forward(InboundRequest request, ResponseSink sink) throws IOException {
        HttpResponse<InputStream> response;
        try {
            response = client.send(toUpstream(request), HttpResponse.BodyHandlers.ofInputStream());
        } catch (IOException e) {
            return upstreamUnreachable(sink, "token-engine could not reach the provider: " + e.getClass().getSimpleName());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return upstreamUnreachable(sink, "token-engine was interrupted while calling the provider");
        }

        String contentType = response.headers().firstValue("content-type").orElse("");
        String upstreamRequestId = response.headers().firstValue("request-id").orElse(null);
        UsageTap tap = UsageTap.forContentType(contentType, mapper);
        boolean streamed = contentType.toLowerCase(Locale.ROOT).startsWith("text/event-stream");
        boolean clientAborted = false;
        long bytes = 0;

        try (InputStream in = response.body()) {
            OutputStream out;
            try {
                sink.start(response.statusCode(), responseHeaders(response.headers()));
                out = sink.body();
            } catch (IOException e) {
                return new ProxyOutcome(response.statusCode(), streamed, true, true, upstreamRequestId, 0, tap.result());
            }
            byte[] buffer = new byte[BUFFER_BYTES];
            int n;
            while ((n = in.read(buffer)) != -1) {
                tap.accept(buffer, 0, n);
                bytes += n;
                try {
                    out.write(buffer, 0, n);
                    out.flush();
                } catch (IOException e) {
                    // Developer hung up. Closing the upstream stream (try-with-resources) stops generation.
                    clientAborted = true;
                    break;
                }
            }
        } catch (IOException e) {
            // Provider connection broke mid-response. Headers are already sent, so just end the response;
            // Claude Code sees an incomplete stream and retries. Usage keeps whatever was seen.
        }
        return new ProxyOutcome(response.statusCode(), streamed, true, clientAborted, upstreamRequestId, bytes,
                tap.result());
    }

    HttpRequest toUpstream(InboundRequest request) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl + request.pathAndQuery()))
                .timeout(responseHeadersTimeout);
        request.headers().forEach((name, values) -> {
            if (forwardRequestHeader(name)) {
                values.forEach(value -> builder.header(name, value));
            }
        });
        HttpRequest.BodyPublisher body = request.body().length == 0
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofByteArray(request.body());
        return builder.method(request.method(), body).build();
    }

    private boolean forwardRequestHeader(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        return !lower.startsWith(":") && !NOT_FORWARDED_REQUEST.contains(lower) && !consumedHeaders.contains(lower);
    }

    private static Map<String, List<String>> responseHeaders(HttpHeaders headers) {
        Map<String, List<String>> out = new LinkedHashMap<>();
        headers.map().forEach((name, values) -> {
            String lower = name.toLowerCase(Locale.ROOT);
            if (!lower.startsWith(":") && !NOT_FORWARDED_RESPONSE.contains(lower)) {
                out.put(name, values);
            }
        });
        return out;
    }

    /** Answers in Anthropic's error shape so Claude Code treats it like any other retryable API error. */
    private ProxyOutcome upstreamUnreachable(ResponseSink sink, String message) throws IOException {
        String body = "{\"type\":\"error\",\"error\":{\"type\":\"api_error\",\"message\":"
                + mapper.writeValueAsString(message) + "}}";
        sink.start(502, Map.of(
                "content-type", List.of("application/json"),
                "x-should-retry", List.of("true")));
        try (OutputStream out = sink.body()) {
            out.write(body.getBytes(StandardCharsets.UTF_8));
        }
        return new ProxyOutcome(502, false, false, false, null, body.length(), ObservedUsage.NONE);
    }
}
