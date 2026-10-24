-- One row per evaluated request. Owned by the quality service; the gateway never touches this schema.
create table quality.cache_observation
(
    request_id            uuid primary key,
    started_at            timestamptz,
    developer             varchar(100),
    session_id            varchar(200),
    session_request_count bigint           not null default 0,
    model                 varchar(100),
    optimised             boolean          not null default false,
    optimiser_strategy    varchar(100),
    prompt_tokens         bigint           not null default 0,
    cache_read_ratio      double precision not null default 0,
    suspected_cache_break boolean          not null default false,
    evaluated_at          timestamptz      not null default now()
);

create index cache_observation_started_at_idx on quality.cache_observation (started_at);
create index cache_observation_suspected_idx on quality.cache_observation (suspected_cache_break)
    where suspected_cache_break;
