package com.tokenengine.quality.consumer;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * @param streamKey                 Redis stream written by the gateway
 * @param group                     consumer group; every quality-service instance joins it, so events are shared out
 * @param consumer                  this instance's name within the group (use the container hostname)
 * @param batchSize                 events read per poll
 * @param minCacheablePromptTokens  see {@code CacheHealthEvaluator}
 */
@ConfigurationProperties("quality")
record QualityProperties(
        @DefaultValue("tokenengine:usage") String streamKey,
        @DefaultValue("quality-service") String group,
        @DefaultValue("quality-1") String consumer,
        @DefaultValue("100") int batchSize,
        @DefaultValue("4096") long minCacheablePromptTokens) {
}
