package com.tokenengine.quality.evaluation;

/**
 * First quality check: is prompt caching still working? Inside a running session almost every request should
 * read most of its prompt from cache. A large prompt with zero cache reads mid-session usually means something
 * changed the prompt prefix, which is exactly the failure an optimiser must never cause.
 *
 * <p>Later checks (task success with and without optimisation, from benchmark runs) plug in next to this one.
 */
public final class CacheHealthEvaluator {

    /** Prompts smaller than this may be legitimately uncacheable, so they are never flagged. */
    private final long minCacheablePromptTokens;

    public CacheHealthEvaluator(long minCacheablePromptTokens) {
        this.minCacheablePromptTokens = minCacheablePromptTokens;
    }

    public Observation evaluate(UsageEvent.Usage u) {
        long promptTokens = u.inputTokens() + u.cacheWrite5mTokens() + u.cacheWrite1hTokens() + u.cacheReadTokens();
        double cacheReadRatio = promptTokens == 0 ? 0 : (double) u.cacheReadTokens() / promptTokens;
        boolean suspectedCacheBreak = u.status() == 200
                && u.sessionRequestCount() > 1
                && promptTokens >= minCacheablePromptTokens
                && u.cacheReadTokens() == 0;
        return new Observation(promptTokens, cacheReadRatio, suspectedCacheBreak);
    }

    public record Observation(long promptTokens, double cacheReadRatio, boolean suspectedCacheBreak) {
    }
}
