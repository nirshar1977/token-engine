package com.tokenengine.gateway.internal;

import tools.jackson.databind.JsonNode;

/**
 * Collects usage from Anthropic Messages responses. Streams report usage in message_start and again,
 * cumulatively, in message_delta, so every field keeps the largest value seen.
 */
final class UsageAccumulator {

    private String model;
    private long input;
    private long output;
    private long cacheWrite5m;
    private long cacheWrite1h;
    private long cacheRead;
    private boolean complete;
    private String errorType;

    void model(JsonNode node) {
        if (node.isString() && !node.asString().isBlank()) {
            model = node.asString();
        }
    }

    void applyUsage(JsonNode usage) {
        if (!usage.isObject()) {
            return;
        }
        input = Math.max(input, longOf(usage, "input_tokens"));
        output = Math.max(output, longOf(usage, "output_tokens"));
        cacheRead = Math.max(cacheRead, longOf(usage, "cache_read_input_tokens"));

        JsonNode breakdown = usage.path("cache_creation");
        if (breakdown.isObject()) {
            cacheWrite5m = Math.max(cacheWrite5m, longOf(breakdown, "ephemeral_5m_input_tokens"));
            cacheWrite1h = Math.max(cacheWrite1h, longOf(breakdown, "ephemeral_1h_input_tokens"));
        } else {
            // Older responses only give the total; it is billed as a 5-minute write.
            cacheWrite5m = Math.max(cacheWrite5m, longOf(usage, "cache_creation_input_tokens"));
        }
    }

    void markComplete() {
        complete = true;
    }

    void error(JsonNode errorTypeNode) {
        errorType = errorTypeNode.isString() ? errorTypeNode.asString() : "unknown";
    }

    ObservedUsage snapshot() {
        return new ObservedUsage(model, input, output, cacheWrite5m, cacheWrite1h, cacheRead, complete, errorType);
    }

    private static long longOf(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isNumber() ? value.longValue() : 0L;
    }
}
