package com.tokenengine.usage.internal;

import com.tokenengine.usage.internal.PricingProperties.ModelPrice;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Prices a request from its exact token counts. Plain Java so it is trivial to test. */
final class CostCalculator {

    record Cost(BigDecimal actual, BigDecimal withoutCache, boolean priced) {
        static final Cost UNPRICED = new Cost(BigDecimal.ZERO, BigDecimal.ZERO, false);
    }

    private static final BigDecimal MILLION = BigDecimal.valueOf(1_000_000);
    private static final int SCALE = 8;

    private final List<ModelPrice> prices;

    CostCalculator(List<ModelPrice> prices) {
        // Longest prefix first, so the most specific entry wins.
        this.prices = prices.stream()
                .sorted(Comparator.comparingInt((ModelPrice p) -> p.modelPrefix().length()).reversed())
                .toList();
    }

    Cost cost(String model, long input, long output, long cacheWrite5m, long cacheWrite1h, long cacheRead) {
        Optional<ModelPrice> match = find(model);
        if (match.isEmpty()) {
            return Cost.UNPRICED;
        }
        ModelPrice p = match.get();
        long promptTokens = input + cacheWrite5m + cacheWrite1h + cacheRead;
        boolean longContext = p.longContextThreshold() > 0 && promptTokens > p.longContextThreshold()
                && p.longContextInput() != null && p.longContextOutput() != null;
        BigDecimal in = longContext ? p.longContextInput() : p.input();
        BigDecimal out = longContext ? p.longContextOutput() : p.output();

        BigDecimal actual = in.multiply(BigDecimal.valueOf(input))
                .add(in.multiply(p.cacheWrite5mMultiplier()).multiply(BigDecimal.valueOf(cacheWrite5m)))
                .add(in.multiply(p.cacheWrite1hMultiplier()).multiply(BigDecimal.valueOf(cacheWrite1h)))
                .add(in.multiply(p.cacheReadMultiplier()).multiply(BigDecimal.valueOf(cacheRead)))
                .add(out.multiply(BigDecimal.valueOf(output)));
        BigDecimal withoutCache = in.multiply(BigDecimal.valueOf(promptTokens))
                .add(out.multiply(BigDecimal.valueOf(output)));

        return new Cost(perMillion(actual), perMillion(withoutCache), true);
    }

    private Optional<ModelPrice> find(String model) {
        if (model == null || model.isBlank()) {
            return Optional.empty();
        }
        return prices.stream().filter(p -> model.startsWith(p.modelPrefix())).findFirst();
    }

    private static BigDecimal perMillion(BigDecimal value) {
        return value.divide(MILLION, SCALE, RoundingMode.HALF_UP);
    }
}
