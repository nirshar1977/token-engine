package com.tokenengine.usage.api;

import com.tokenengine.shared.api.DeveloperId;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

/**
 * Everything the gateway observed about one Messages API call. Token counts come from the provider's own
 * {@code usage} block, so they are exact, not estimates.
 *
 * @param agentId      Claude Code subagent id (x-claude-code-agent-id), null for the main conversation
 * @param requestClass Claude Code request class (main, subagent, compaction, auxiliary...) when hint headers are on
 * @param complete     the full response was seen (message_stop for streams)
 * @param clientAborted the developer's client disconnected before the response finished
 */
@Builder
public record RequestUsage(
        UUID requestId,
        Instant startedAt,
        long latencyMs,
        DeveloperId developer,
        String sessionId,
        long sessionRequestCount,
        String agentId,
        String requestClass,
        String model,
        int status,
        boolean streamed,
        boolean complete,
        boolean clientAborted,
        String upstreamRequestId,
        long inputTokens,
        long outputTokens,
        long cacheWrite5mTokens,
        long cacheWrite1hTokens,
        long cacheReadTokens,
        long requestBytesBefore,
        long requestBytesAfter,
        boolean optimised,
        String optimiserStrategy,
        int duplicateBlocks,
        long duplicateBytes,
        long latestTurnDuplicateBytes) {
}
