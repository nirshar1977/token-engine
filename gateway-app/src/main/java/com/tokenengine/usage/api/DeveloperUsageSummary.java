package com.tokenengine.usage.api;

import java.math.BigDecimal;

/** Totals for one developer over a reporting window. */
public record DeveloperUsageSummary(
        String developer,
        long requests,
        long inputTokens,
        long outputTokens,
        long cacheWriteTokens,
        long cacheReadTokens,
        BigDecimal costUsd,
        BigDecimal costWithoutCacheUsd,
        long latestTurnDuplicateBytes) {
}
