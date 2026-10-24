package com.tokenengine.gateway.internal;

import java.util.List;
import java.util.Map;

/**
 * A request as received from the developer's tool.
 *
 * @param pathAndQuery raw path plus query string, e.g. {@code /v1/messages?beta=true}
 */
record InboundRequest(String method, String pathAndQuery, Map<String, List<String>> headers, byte[] body) {
}
