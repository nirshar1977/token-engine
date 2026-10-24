package com.tokenengine.quality.consumer;

import com.tokenengine.quality.evaluation.CacheHealthEvaluator;
import com.tokenengine.quality.evaluation.UsageEvent;
import com.tokenengine.quality.persistence.ObservationRepository;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Reads usage events from the Redis stream with a consumer group (at-least-once delivery) and acknowledges
 * each event after it is stored. A bad event is logged and acknowledged so it can't block the stream.
 */
@Slf4j
@Component
class UsageStreamConsumer {

    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;
    private final ObservationRepository repository;
    private final QualityProperties properties;
    private final CacheHealthEvaluator evaluator;
    private volatile boolean groupReady;

    UsageStreamConsumer(StringRedisTemplate redis, ObjectMapper mapper, ObservationRepository repository,
                        QualityProperties properties) {
        this.redis = redis;
        this.mapper = mapper;
        this.repository = repository;
        this.properties = properties;
        this.evaluator = new CacheHealthEvaluator(properties.minCacheablePromptTokens());
    }

    @Scheduled(fixedDelayString = "${quality.poll-interval:1s}")
    void poll() {
        try {
            if (!ensureGroup()) {
                return;
            }
            List<MapRecord<String, Object, Object>> records = readBatch();
            if (records == null) {
                return;
            }
            for (MapRecord<String, Object, Object> record : records) {
                handle(record);
                redis.opsForStream().acknowledge(properties.streamKey(), properties.group(), record.getId());
            }
        } catch (DataAccessException e) {
            log.warn("Usage stream unavailable, will retry: {}", e.getMessage());
        }
    }

    // read(...) takes generic varargs (StreamOffset<K>...), which the interface can't mark @SafeVarargs.
    // A single offset is passed, so the generic array is never exposed.
    @SuppressWarnings("unchecked")
    private List<MapRecord<String, Object, Object>> readBatch() {
        return redis.opsForStream().read(
                Consumer.from(properties.group(), properties.consumer()),
                StreamReadOptions.empty().count(properties.batchSize()),
                StreamOffset.create(properties.streamKey(), ReadOffset.lastConsumed()));
    }

    private void handle(MapRecord<String, Object, Object> record) {
        try {
            Object payload = record.getValue().get("payload");
            UsageEvent event = mapper.readValue(String.valueOf(payload), UsageEvent.class);
            var observation = evaluator.evaluate(event.usage());
            repository.save(event.usage(), observation);
            if (observation.suspectedCacheBreak()) {
                log.warn("Possible prompt-cache break: developer={} session={} request={} promptTokens={}",
                        event.usage().developer(), event.usage().sessionId(), event.usage().requestId(),
                        observation.promptTokens());
            }
        } catch (RuntimeException e) {
            log.error("Skipping unreadable usage event {}: {}", record.getId(), e.toString());
        }
    }

    /** Creates the consumer group once the gateway has created the stream. */
    private boolean ensureGroup() {
        if (groupReady) {
            return true;
        }
        if (!Boolean.TRUE.equals(redis.hasKey(properties.streamKey()))) {
            return false;
        }
        try {
            redis.opsForStream().createGroup(properties.streamKey(), ReadOffset.from("0"), properties.group());
        } catch (DataAccessException e) {
            // Another instance (or an earlier run) already created the group.
            boolean exists = String.valueOf(e.getMessage()).contains("BUSYGROUP")
                    || String.valueOf(e.getMostSpecificCause().getMessage()).contains("BUSYGROUP");
            if (!exists) {
                throw e;
            }
        }
        groupReady = true;
        return true;
    }
}
