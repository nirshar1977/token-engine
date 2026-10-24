package com.tokenengine.usage.internal;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Model prices in USD per million tokens. A model id matches the entry with the longest
 * {@code modelPrefix} it starts with, so {@code claude-opus-4-6} beats {@code claude-opus-4}.
 */
@ConfigurationProperties("tokenengine.pricing")
record PricingProperties(@DefaultValue List<ModelPrice> models) {

    /**
     * @param longContextThreshold prompt size (input + cache tokens) above which the long-context prices apply;
     *                             0 means the model has a single tier
     */
    record ModelPrice(
            String modelPrefix,
            BigDecimal input,
            BigDecimal output,
            @DefaultValue("0.1") BigDecimal cacheReadMultiplier,
            @DefaultValue("1.25") BigDecimal cacheWrite5mMultiplier,
            @DefaultValue("2") BigDecimal cacheWrite1hMultiplier,
            @DefaultValue("0") long longContextThreshold,
            BigDecimal longContextInput,
            BigDecimal longContextOutput) {
    }
}
