-- One row per proxied Messages API call. Token counts are the provider's own numbers.
create table usage.request_usage
(
    id                          uuid primary key,
    started_at                  timestamptz    not null,
    latency_ms                  bigint         not null,
    developer                   varchar(100)   not null,
    session_id                  varchar(200),
    session_request_count       bigint         not null default 0,
    agent_id                    varchar(200),
    request_class               varchar(50),
    model                       varchar(100),
    status                      integer        not null,
    streamed                    boolean        not null,
    complete                    boolean        not null,
    client_aborted              boolean        not null,
    upstream_request_id         varchar(200),

    input_tokens                bigint         not null default 0,
    output_tokens               bigint         not null default 0,
    cache_write_5m_tokens       bigint         not null default 0,
    cache_write_1h_tokens       bigint         not null default 0,
    cache_read_tokens           bigint         not null default 0,

    cost_usd                    numeric(18, 8) not null default 0,
    cost_without_cache_usd      numeric(18, 8) not null default 0,
    priced                      boolean        not null default false,

    request_bytes_before        bigint         not null default 0,
    request_bytes_after         bigint         not null default 0,
    optimised                   boolean        not null default false,
    optimiser_strategy          varchar(100),
    duplicate_blocks            integer        not null default 0,
    duplicate_bytes             bigint         not null default 0,
    latest_turn_duplicate_bytes bigint         not null default 0
);

create index request_usage_started_at_idx on usage.request_usage (started_at);
create index request_usage_developer_started_at_idx on usage.request_usage (developer, started_at);
create index request_usage_session_idx on usage.request_usage (session_id);

-- Ready-made daily rollup for Grafana panels.
create view usage.daily_summary as
select date_trunc('day', started_at)                       as day,
       developer,
       model,
       count(*)                                            as requests,
       sum(input_tokens)                                   as input_tokens,
       sum(output_tokens)                                  as output_tokens,
       sum(cache_write_5m_tokens + cache_write_1h_tokens)  as cache_write_tokens,
       sum(cache_read_tokens)                              as cache_read_tokens,
       sum(cost_usd)                                       as cost_usd,
       sum(cost_without_cache_usd)                         as cost_without_cache_usd,
       sum(cost_without_cache_usd - cost_usd)              as cache_savings_usd,
       sum(latest_turn_duplicate_bytes) / 4                as est_wasted_tokens
from usage.request_usage
group by 1, 2, 3;
