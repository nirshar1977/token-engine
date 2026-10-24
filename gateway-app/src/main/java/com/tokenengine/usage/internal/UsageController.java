package com.tokenengine.usage.internal;

import com.tokenengine.usage.api.DeveloperUsageSummary;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Usage", description = "Token usage, cost and cache savings per developer")
@RestController
@RequestMapping("/engine/api/usage")
@RequiredArgsConstructor
class UsageController {

    private final JdbcClient jdbc;

    @Operation(summary = "Per-developer totals for the last N days, most expensive first")
    @GetMapping("/summary")
    List<DeveloperUsageSummary> summary(
            @Parameter(description = "Window size in days (1-90)") @RequestParam(defaultValue = "7") int days) {
        int window = Math.clamp(days, 1, 90);
        Instant since = Instant.now().minus(Duration.ofDays(window));
        return jdbc.sql("""
                        select developer,
                               count(*)                                              as requests,
                               coalesce(sum(input_tokens), 0)                        as input_tokens,
                               coalesce(sum(output_tokens), 0)                       as output_tokens,
                               coalesce(sum(cache_write_5m_tokens + cache_write_1h_tokens), 0) as cache_write_tokens,
                               coalesce(sum(cache_read_tokens), 0)                   as cache_read_tokens,
                               coalesce(sum(cost_usd), 0)                            as cost_usd,
                               coalesce(sum(cost_without_cache_usd), 0)              as cost_without_cache_usd,
                               coalesce(sum(latest_turn_duplicate_bytes), 0)         as latest_turn_duplicate_bytes
                          from usage.request_usage
                         where started_at >= :since
                         group by developer
                         order by cost_usd desc
                        """)
                .param("since", Timestamp.from(since))
                .query((rs, row) -> new DeveloperUsageSummary(
                        rs.getString("developer"),
                        rs.getLong("requests"),
                        rs.getLong("input_tokens"),
                        rs.getLong("output_tokens"),
                        rs.getLong("cache_write_tokens"),
                        rs.getLong("cache_read_tokens"),
                        rs.getBigDecimal("cost_usd"),
                        rs.getBigDecimal("cost_without_cache_usd"),
                        rs.getLong("latest_turn_duplicate_bytes")))
                .list();
    }
}
