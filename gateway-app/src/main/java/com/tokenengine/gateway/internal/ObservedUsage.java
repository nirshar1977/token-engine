package com.tokenengine.gateway.internal;

/**
 * Token usage read from the provider's response, as the provider reported it.
 *
 * @param complete  the whole response was seen (message_stop for streams, a parsed body for JSON)
 * @param errorType the provider's error type when the response was an error, otherwise null
 */
record ObservedUsage(
        String model,
        long inputTokens,
        long outputTokens,
        long cacheWrite5mTokens,
        long cacheWrite1hTokens,
        long cacheReadTokens,
        boolean complete,
        String errorType) {

    static final ObservedUsage NONE = new ObservedUsage(null, 0, 0, 0, 0, 0, false, null);
}
