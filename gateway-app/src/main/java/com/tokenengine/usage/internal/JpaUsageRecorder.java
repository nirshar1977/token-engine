package com.tokenengine.usage.internal;

import com.tokenengine.usage.api.RequestUsage;
import com.tokenengine.usage.api.UsageRecorded;
import com.tokenengine.usage.api.UsageRecorder;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class JpaUsageRecorder implements UsageRecorder {

    private final RequestUsageRepository repository;
    private final ApplicationEventPublisher events;
    private final MeterRegistry meters;
    private final CostCalculator costs;

    JpaUsageRecorder(RequestUsageRepository repository, ApplicationEventPublisher events,
                     MeterRegistry meters, PricingProperties pricing) {
        this.repository = repository;
        this.events = events;
        this.meters = meters;
        this.costs = new CostCalculator(pricing.models());
    }

    @Override
    @Transactional
    public void record(RequestUsage u) {
        CostCalculator.Cost cost = costs.cost(u.model(), u.inputTokens(), u.outputTokens(),
                u.cacheWrite5mTokens(), u.cacheWrite1hTokens(), u.cacheReadTokens());

        repository.save(toEntity(u, cost));
        events.publishEvent(new UsageRecorded(u, cost.actual(), cost.withoutCache(), cost.priced()));
        countTokens(u, cost);
    }

    private void countTokens(RequestUsage u, CostCalculator.Cost cost) {
        String model = u.model() == null ? "unknown" : u.model();
        String developer = u.developer().value();
        meters.counter("tokenengine.tokens", "type", "input", "model", model, "developer", developer)
                .increment(u.inputTokens());
        meters.counter("tokenengine.tokens", "type", "output", "model", model, "developer", developer)
                .increment(u.outputTokens());
        meters.counter("tokenengine.tokens", "type", "cache_read", "model", model, "developer", developer)
                .increment(u.cacheReadTokens());
        meters.counter("tokenengine.tokens", "type", "cache_write", "model", model, "developer", developer)
                .increment(u.cacheWrite5mTokens() + u.cacheWrite1hTokens());
        meters.counter("tokenengine.cost.usd", "model", model, "developer", developer)
                .increment(cost.actual().doubleValue());
    }

    private static RequestUsageEntity toEntity(RequestUsage u, CostCalculator.Cost cost) {
        RequestUsageEntity e = new RequestUsageEntity();
        e.setId(u.requestId());
        e.setStartedAt(u.startedAt());
        e.setLatencyMs(u.latencyMs());
        e.setDeveloper(u.developer().value());
        e.setSessionId(u.sessionId());
        e.setSessionRequestCount(u.sessionRequestCount());
        e.setAgentId(u.agentId());
        e.setRequestClass(u.requestClass());
        e.setModel(u.model());
        e.setStatus(u.status());
        e.setStreamed(u.streamed());
        e.setComplete(u.complete());
        e.setClientAborted(u.clientAborted());
        e.setUpstreamRequestId(u.upstreamRequestId());
        e.setInputTokens(u.inputTokens());
        e.setOutputTokens(u.outputTokens());
        e.setCacheWrite5mTokens(u.cacheWrite5mTokens());
        e.setCacheWrite1hTokens(u.cacheWrite1hTokens());
        e.setCacheReadTokens(u.cacheReadTokens());
        e.setCostUsd(cost.actual());
        e.setCostWithoutCacheUsd(cost.withoutCache());
        e.setPriced(cost.priced());
        e.setRequestBytesBefore(u.requestBytesBefore());
        e.setRequestBytesAfter(u.requestBytesAfter());
        e.setOptimised(u.optimised());
        e.setOptimiserStrategy(u.optimiserStrategy());
        e.setDuplicateBlocks(u.duplicateBlocks());
        e.setDuplicateBytes(u.duplicateBytes());
        e.setLatestTurnDuplicateBytes(u.latestTurnDuplicateBytes());
        return e;
    }
}
