package com.tokenengine.gateway.internal;

import tools.jackson.databind.ObjectMapper;

/**
 * Watches response bytes on their way to the developer and extracts token usage.
 * A tap only reads: the bytes the developer receives are exactly what the provider sent.
 */
interface UsageTap {

    void accept(byte[] buffer, int offset, int length);

    /** Usage seen so far. Call once the response has ended (or was cut off). */
    ObservedUsage result();

    static UsageTap forContentType(String contentType, ObjectMapper mapper) {
        String type = contentType == null ? "" : contentType.toLowerCase();
        if (type.startsWith("text/event-stream")) {
            return new SseUsageTap(mapper);
        }
        if (type.startsWith("application/json")) {
            return new JsonUsageTap(mapper);
        }
        return NoopUsageTap.INSTANCE;
    }

    enum NoopUsageTap implements UsageTap {
        INSTANCE;

        @Override
        public void accept(byte[] buffer, int offset, int length) {
        }

        @Override
        public ObservedUsage result() {
            return ObservedUsage.NONE;
        }
    }
}
