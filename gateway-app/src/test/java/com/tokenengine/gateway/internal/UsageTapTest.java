package com.tokenengine.gateway.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

class UsageTapTest {

    private final ObjectMapper mapper = JsonMapper.builder().build();

    static final String STREAM = """
            event: message_start
            data: {"type":"message_start","message":{"id":"msg_1","model":"claude-sonnet-4-6","usage":{"input_tokens":12,"cache_creation_input_tokens":2048,"cache_creation":{"ephemeral_5m_input_tokens":2048,"ephemeral_1h_input_tokens":0},"cache_read_input_tokens":30000,"output_tokens":1}}}

            event: ping
            data: {"type": "ping"}

            event: content_block_delta
            data: {"type":"content_block_delta","index":0,"delta":{"type":"text_delta","text":"héllo \\"message_start\\""}}

            event: message_delta
            data: {"type":"message_delta","delta":{"stop_reason":"end_turn"},"usage":{"output_tokens":345}}

            event: message_stop
            data: {"type":"message_stop"}

            """;

    @Test
    void readsUsageFromStreamSplitIntoAwkwardChunks() {
        byte[] bytes = STREAM.replace("\n", "\r\n").getBytes(StandardCharsets.UTF_8);
        UsageTap tap = UsageTap.forContentType("text/event-stream; charset=utf-8", mapper);
        for (int i = 0; i < bytes.length; i += 7) { // 7-byte chunks split lines and multi-byte characters
            tap.accept(bytes, i, Math.min(7, bytes.length - i));
        }

        ObservedUsage usage = tap.result();

        assertThat(usage.model()).isEqualTo("claude-sonnet-4-6");
        assertThat(usage.inputTokens()).isEqualTo(12);
        assertThat(usage.cacheWrite5mTokens()).isEqualTo(2048);
        assertThat(usage.cacheWrite1hTokens()).isZero();
        assertThat(usage.cacheReadTokens()).isEqualTo(30000);
        assertThat(usage.outputTokens()).isEqualTo(345);
        assertThat(usage.complete()).isTrue();
    }

    @Test
    void streamCutOffBeforeMessageStopIsIncomplete() {
        String cut = STREAM.substring(0, STREAM.indexOf("event: message_delta"));
        UsageTap tap = UsageTap.forContentType("text/event-stream", mapper);
        byte[] bytes = cut.getBytes(StandardCharsets.UTF_8);
        tap.accept(bytes, 0, bytes.length);

        ObservedUsage usage = tap.result();

        assertThat(usage.complete()).isFalse();
        assertThat(usage.inputTokens()).isEqualTo(12);
        assertThat(usage.outputTokens()).isEqualTo(1);
    }

    @Test
    void readsStreamedError() {
        String error = "event: error\ndata: {\"type\":\"error\",\"error\":{\"type\":\"overloaded_error\",\"message\":\"Overloaded\"}}\n\n";
        UsageTap tap = UsageTap.forContentType("text/event-stream", mapper);
        byte[] bytes = error.getBytes(StandardCharsets.UTF_8);
        tap.accept(bytes, 0, bytes.length);

        assertThat(tap.result().errorType()).isEqualTo("overloaded_error");
    }

    @Test
    void readsNonStreamedJsonResponse() {
        String json = """
                {"id":"msg_2","type":"message","model":"claude-opus-4-6","content":[],
                 "usage":{"input_tokens":100,"cache_creation_input_tokens":50,"cache_read_input_tokens":0,"output_tokens":20}}
                """;
        UsageTap tap = UsageTap.forContentType("application/json", mapper);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        tap.accept(bytes, 0, bytes.length);

        ObservedUsage usage = tap.result();

        assertThat(usage.model()).isEqualTo("claude-opus-4-6");
        assertThat(usage.inputTokens()).isEqualTo(100);
        assertThat(usage.cacheWrite5mTokens()).isEqualTo(50);
        assertThat(usage.outputTokens()).isEqualTo(20);
        assertThat(usage.complete()).isTrue();
    }

    @Test
    void ignoresOtherContentTypes() {
        assertThat(UsageTap.forContentType("text/html", mapper).result()).isEqualTo(ObservedUsage.NONE);
    }
}
