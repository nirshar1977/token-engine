package com.tokenengine.usage.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.tokenengine.usage.internal.PricingProperties.ModelPrice;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class CostCalculatorTest {

    private static ModelPrice price(String prefix, String in, String out, String cacheRead) {
        return new ModelPrice(prefix, new BigDecimal(in), new BigDecimal(out), new BigDecimal(cacheRead),
                new BigDecimal("1.25"), new BigDecimal("2"), 0, null, null);
    }

    private final CostCalculator calculator = new CostCalculator(List.of(
            price("claude-opus-4", "15", "75", "0.1"),
            price("claude-opus-4-6", "5", "25", "0.1"),
            new ModelPrice("claude-haiku-5-5", new BigDecimal("0.10"), new BigDecimal("0.50"), new BigDecimal("0.1"),
                    new BigDecimal("1.25"), new BigDecimal("2"), 100_000, new BigDecimal("0.50"), new BigDecimal("2.50"))));

    @Test
    void longestPrefixWinsAndCachingIsPricedPerType() {
        // 1M of each token type on Opus 4.6 ($5 in / $25 out)
        CostCalculator.Cost cost = calculator.cost("claude-opus-4-6", 1_000_000, 1_000_000, 1_000_000, 1_000_000, 1_000_000);

        // input 5 + output 25 + 5m write 6.25 + 1h write 10 + read 0.5
        assertThat(cost.actual()).isEqualByComparingTo("46.75");
        // without caching every prompt token is full price: 4M * 5 + 25
        assertThat(cost.withoutCache()).isEqualByComparingTo("45");
        assertThat(cost.priced()).isTrue();
    }

    @Test
    void cacheReadsMakeLongSessionsCheap() {
        CostCalculator.Cost cost = calculator.cost("claude-opus-4-20250514", 1_000, 0, 0, 0, 99_000);

        assertThat(cost.actual()).isEqualByComparingTo("0.1635");      // 1k * 15 + 99k * 1.5
        assertThat(cost.withoutCache()).isEqualByComparingTo("1.5");    // 100k * 15
    }

    @Test
    void longContextTierAppliesAboveThreshold() {
        assertThat(calculator.cost("claude-haiku-5-5", 100_000, 0, 0, 0, 0).actual()).isEqualByComparingTo("0.01");
        assertThat(calculator.cost("claude-haiku-5-5", 200_000, 0, 0, 0, 0).actual()).isEqualByComparingTo("0.1");
    }

    @Test
    void unknownModelIsUnpriced() {
        assertThat(calculator.cost("gpt-x", 10, 10, 0, 0, 0)).isEqualTo(CostCalculator.Cost.UNPRICED);
        assertThat(calculator.cost(null, 10, 10, 0, 0, 0).priced()).isFalse();
    }
}
