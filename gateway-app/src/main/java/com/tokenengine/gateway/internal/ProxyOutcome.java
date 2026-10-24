package com.tokenengine.gateway.internal;

/**
 * What happened to one proxied request.
 *
 * @param upstreamReachable false when the provider couldn't be reached at all (the gateway answered 502 itself)
 * @param clientAborted     the developer's client went away before the response finished
 * @param upstreamRequestId the provider's own request id (Anthropic's request-id header), for support tickets
 */
record ProxyOutcome(
        int status,
        boolean streamed,
        boolean upstreamReachable,
        boolean clientAborted,
        String upstreamRequestId,
        long responseBytes,
        ObservedUsage usage) {
}
