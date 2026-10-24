package com.tokenengine.quality.persistence;

import com.tokenengine.quality.evaluation.CacheHealthEvaluator.Observation;
import com.tokenengine.quality.evaluation.UsageEvent;
import java.sql.Timestamp;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ObservationRepository {

    private final JdbcClient jdbc;

    /** Idempotent: a redelivered event (same request id) is ignored. */
    public void save(UsageEvent.Usage u, Observation o) {
        jdbc.sql("""
                        insert into quality.cache_observation
                            (request_id, started_at, developer, session_id, session_request_count, model,
                             optimised, optimiser_strategy, prompt_tokens, cache_read_ratio, suspected_cache_break)
                        values (:requestId, :startedAt, :developer, :sessionId, :sessionRequestCount, :model,
                                :optimised, :strategy, :promptTokens, :ratio, :suspected)
                        on conflict (request_id) do nothing
                        """)
                .param("requestId", u.requestId())
                .param("startedAt", u.startedAt() == null ? null : Timestamp.from(u.startedAt()))
                .param("developer", u.developer())
                .param("sessionId", u.sessionId())
                .param("sessionRequestCount", u.sessionRequestCount())
                .param("model", u.model())
                .param("optimised", u.optimised())
                .param("strategy", u.optimiserStrategy())
                .param("promptTokens", o.promptTokens())
                .param("ratio", o.cacheReadRatio())
                .param("suspected", o.suspectedCacheBreak())
                .update();
    }
}
