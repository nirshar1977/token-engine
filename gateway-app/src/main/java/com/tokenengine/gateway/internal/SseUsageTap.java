package com.tokenengine.gateway.internal;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Parses a server-sent event stream incrementally, chunk by chunk, without ever holding the whole response.
 * Only the few events that carry usage or errors are JSON-parsed; content deltas are skipped cheaply.
 */
final class SseUsageTap implements UsageTap {

    /** Usage events are tiny; a longer line is a content delta we don't need, so it is dropped. */
    private static final int MAX_LINE_BYTES = 1 << 20;

    private final ObjectMapper mapper;
    private final UsageAccumulator usage = new UsageAccumulator();
    private final ByteArrayOutputStream line = new ByteArrayOutputStream(256);
    private final StringBuilder data = new StringBuilder();
    private boolean lineTooLong;

    SseUsageTap(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void accept(byte[] buffer, int offset, int length) {
        for (int i = offset; i < offset + length; i++) {
            byte b = buffer[i];
            if (b == '\n') {
                endOfLine();
            } else if (!lineTooLong) {
                if (line.size() >= MAX_LINE_BYTES) {
                    lineTooLong = true;
                    line.reset();
                } else {
                    line.write(b);
                }
            }
        }
    }

    @Override
    public ObservedUsage result() {
        if (line.size() > 0) {
            endOfLine();
        }
        dispatch();
        return usage.snapshot();
    }

    private void endOfLine() {
        if (lineTooLong) {
            lineTooLong = false;
            line.reset();
            return;
        }
        String text = line.toString(StandardCharsets.UTF_8);
        line.reset();
        if (text.endsWith("\r")) {
            text = text.substring(0, text.length() - 1);
        }
        if (text.isEmpty()) {
            dispatch();
        } else if (text.startsWith("data:")) {
            String value = text.substring(5);
            if (value.startsWith(" ")) {
                value = value.substring(1);
            }
            if (!data.isEmpty()) {
                data.append('\n');
            }
            data.append(value);
        }
        // "event:", "id:", "retry:" and ":" comment lines carry nothing we need: the JSON has a "type" field.
    }

    private void dispatch() {
        if (data.isEmpty()) {
            return;
        }
        String payload = data.toString();
        data.setLength(0);
        if (!mayCarryUsage(payload)) {
            return;
        }
        JsonNode event;
        try {
            event = mapper.readTree(payload);
        } catch (Exception e) {
            return;
        }
        String type = event.path("type").isString() ? event.path("type").asString() : "";
        switch (type) {
            case "message_start" -> {
                JsonNode message = event.path("message");
                usage.model(message.path("model"));
                usage.applyUsage(message.path("usage"));
            }
            case "message_delta" -> usage.applyUsage(event.path("usage"));
            case "message_stop" -> usage.markComplete();
            case "error" -> usage.error(event.path("error").path("type"));
            default -> {
                // content_block_* and ping events: nothing to record
            }
        }
    }

    private static boolean mayCarryUsage(String payload) {
        return payload.contains("\"message_start\"")
                || payload.contains("\"message_delta\"")
                || payload.contains("\"message_stop\"")
                || payload.contains("\"error\"");
    }
}
