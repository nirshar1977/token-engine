package com.tokenengine.quality.evaluation;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CacheHealthEvaluatorTest {

    private final CacheHealthEvaluator evaluator = new CacheHealthEvaluator(4096);

    private static UsageEvent.Usage usage(long sessionRequests, long input, long cacheWrite, long cacheRead) {
        return new UsageEvent.Usage(UUID.randomUUID(), Instant.now(), "nir", "s1", sessionRequests,
                "claude-sonnet-4-6", 200, true, false, "pass-through", input, 100, cacheWrite, 0, cacheRead);
    }

    @Test
    void healthySessionReadsFromCache() {
        var o = evaluator.evaluate(usage(5, 500, 1_000, 48_500));

        assertThat(o.suspectedCacheBreak()).isFalse();
        assertThat(o.promptTokens()).isEqualTo(50_000);
        assertThat(o.cacheReadRatio()).isEqualTo(0.97);
    }

    @Test
    void bigPromptWithNoCacheReadsMidSessionIsFlagged() {
        assertThat(evaluator.evaluate(usage(5, 50_000, 0, 0)).suspectedCacheBreak()).isTrue();
    }

    @Test
    void firstRequestAndSmallPromptsAreNeverFlagged() {
        assertThat(evaluator.evaluate(usage(1, 50_000, 0, 0)).suspectedCacheBreak()).isFalse();
        assertThat(evaluator.evaluate(usage(5, 1_000, 0, 0)).suspectedCacheBreak()).isFalse();
    }
}
