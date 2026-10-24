package com.tokenengine.gateway.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/** Runs the proxy core against a fake provider on localhost: no Spring, no network. */
class UpstreamProxyTest {

    private HttpServer upstream;
    private UpstreamProxy proxy;
    private final Map<String, String> seenHeaders = new ConcurrentHashMap<>();
    private volatile byte[] seenBody;

    @BeforeEach
    void startFakeProvider() throws IOException {
        upstream = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        upstream.createContext("/v1/messages", exchange -> {
            exchange.getRequestHeaders().forEach((k, v) -> seenHeaders.put(k.toLowerCase(), v.getFirst()));
            seenBody = exchange.getRequestBody().readAllBytes();
            exchange.getResponseHeaders().add("content-type", "text/event-stream");
            exchange.getResponseHeaders().add("request-id", "req_upstream_1");
            exchange.sendResponseHeaders(200, 0); // chunked
            try (OutputStream out = exchange.getResponseBody()) {
                for (String event : UsageTapTest.STREAM.split("\n\n")) {
                    out.write((event + "\n\n").getBytes(StandardCharsets.UTF_8));
                    out.flush();
                }
            }
        });
        upstream.start();
        HttpClient client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
        proxy = new UpstreamProxy(client, URI.create("http://127.0.0.1:" + upstream.getAddress().getPort() + "/"),
                Duration.ofSeconds(10), Set.of("x-token-engine-key"), JsonMapper.builder().build());
    }

    @AfterEach
    void stop() {
        upstream.stop(0);
    }

    @Test
    void streamsResponseUnchangedAndReadsUsage() throws IOException {
        byte[] body = "{\"model\":\"claude-sonnet-4-6\",\"stream\":true}".getBytes(StandardCharsets.UTF_8);
        RecordingSink sink = new RecordingSink();

        ProxyOutcome outcome = proxy.forward(new InboundRequest("POST", "/v1/messages?beta=true", Map.of(
                "x-api-key", List.of("sk-test"),
                "anthropic-beta", List.of("interleaved-thinking-2025-05-14,context-management-2025-06-27"),
                "anthropic-version", List.of("2023-06-01"),
                "x-token-engine-key", List.of("secret-dev-key"),
                "accept-encoding", List.of("gzip"),
                "host", List.of("gateway.example.com")), body), sink);

        // Byte-for-byte passthrough of the stream
        assertThat(sink.body.toString(StandardCharsets.UTF_8)).isEqualTo(String.join("",
                java.util.Arrays.stream(UsageTapTest.STREAM.split("\n\n")).map(e -> e + "\n\n").toList()));
        assertThat(sink.status).isEqualTo(200);
        // The JDK client reports header names in lower case
        assertThat(sink.headers).containsKey("content-type");
        assertThat(sink.headers).containsKey("request-id");
        assertThat(sink.headers).doesNotContainKey("transfer-encoding");

        // Headers Claude Code relies on are forwarded; the gateway's own key is not
        assertThat(seenHeaders).containsEntry("anthropic-beta",
                "interleaved-thinking-2025-05-14,context-management-2025-06-27");
        assertThat(seenHeaders).containsEntry("anthropic-version", "2023-06-01");
        assertThat(seenHeaders).containsEntry("x-api-key", "sk-test");
        assertThat(seenHeaders).doesNotContainKey("x-token-engine-key");
        assertThat(seenHeaders).doesNotContainKey("accept-encoding");
        assertThat(seenBody).isEqualTo(body);

        assertThat(outcome.streamed()).isTrue();
        assertThat(outcome.upstreamRequestId()).isEqualTo("req_upstream_1");
        assertThat(outcome.usage().outputTokens()).isEqualTo(345);
        assertThat(outcome.usage().cacheReadTokens()).isEqualTo(30000);
        assertThat(outcome.usage().complete()).isTrue();
    }

    @Test
    void answersInAnthropicErrorShapeWhenProviderIsDown() throws IOException {
        upstream.stop(0);
        RecordingSink sink = new RecordingSink();

        ProxyOutcome outcome = proxy.forward(
                new InboundRequest("POST", "/v1/messages", Map.of(), new byte[0]), sink);

        assertThat(outcome.upstreamReachable()).isFalse();
        assertThat(sink.status).isEqualTo(502);
        assertThat(sink.headers).containsEntry("x-should-retry", List.of("true"));
        assertThat(sink.body.toString(StandardCharsets.UTF_8)).contains("\"type\":\"api_error\"");
    }

    static final class RecordingSink implements ResponseSink {
        int status;
        Map<String, List<String>> headers;
        final ByteArrayOutputStream body = new ByteArrayOutputStream();

        @Override
        public void start(int status, Map<String, List<String>> headers) {
            this.status = status;
            this.headers = headers;
        }

        @Override
        public OutputStream body() {
            return body;
        }
    }
}
