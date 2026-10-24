package com.tokenengine.usage.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(schema = "usage", name = "request_usage")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class RequestUsageEntity {

    @Id
    private UUID id;

    private Instant startedAt;
    private long latencyMs;
    private String developer;
    private String sessionId;
    private long sessionRequestCount;
    private String agentId;
    private String requestClass;
    private String model;
    private int status;
    private boolean streamed;
    private boolean complete;
    private boolean clientAborted;
    private String upstreamRequestId;

    private long inputTokens;
    private long outputTokens;
    @Column(name = "cache_write_5m_tokens")
    private long cacheWrite5mTokens;
    @Column(name = "cache_write_1h_tokens")
    private long cacheWrite1hTokens;
    private long cacheReadTokens;

    private BigDecimal costUsd;
    private BigDecimal costWithoutCacheUsd;
    private boolean priced;

    private long requestBytesBefore;
    private long requestBytesAfter;
    private boolean optimised;
    private String optimiserStrategy;
    private int duplicateBlocks;
    private long duplicateBytes;
    private long latestTurnDuplicateBytes;
}
