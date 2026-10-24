package com.tokenengine.session.api;

import java.time.Instant;

/**
 * What the engine knows about the coding session a request belongs to.
 *
 * @param sessionId    stable id, from Claude Code's x-claude-code-session-id header when present
 * @param requestCount how many requests this session has sent, including the current one
 * @param startedAt    when the engine first saw the session
 * @param degraded     true when the session store was unavailable and this is a best-effort value
 */
public record SessionContext(String sessionId, long requestCount, Instant startedAt, boolean degraded) {
}
