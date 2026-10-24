package com.tokenengine.quality.evaluation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * The fields of the gateway's {@code UsageRecorded} event this service needs. Unknown fields are ignored so
 * the gateway can add data without breaking this consumer.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record UsageEvent(Usage usage, BigDecimal costUsd, BigDecimal costWithoutCacheUsd, boolean priced) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Usage(
            UUID requestId,
            Instant startedAt,
            String developer,
            String sessionId,
            long sessionRequestCount,
            String model,
            int status,
            boolean complete,
            boolean optimised,
            String optimiserStrategy,
            long inputTokens,
            long outputTokens,
            long cacheWrite5mTokens,
            long cacheWrite1hTokens,
            long cacheReadTokens) {
    }
}
