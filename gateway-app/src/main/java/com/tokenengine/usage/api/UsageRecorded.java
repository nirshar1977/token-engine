package com.tokenengine.usage.api;

import java.math.BigDecimal;

/**
 * Published after a request's usage is stored. Also forwarded to the Redis stream
 * {@code tokenengine:usage} as JSON for the quality service.
 *
 * @param costUsd             what the request actually cost, with prompt caching
 * @param costWithoutCacheUsd what it would have cost with every prompt token at the full input price
 * @param priced              false when the model had no price configured (costs are then zero)
 */
public record UsageRecorded(RequestUsage usage, BigDecimal costUsd, BigDecimal costWithoutCacheUsd, boolean priced) {
}
