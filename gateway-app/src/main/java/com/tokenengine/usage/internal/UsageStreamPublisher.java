package com.tokenengine.usage.internal;

import com.tokenengine.usage.api.UsageRecorded;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Bridges the in-process {@link UsageRecorded} event to a Redis stream, which is how the separately deployed
 * quality service receives it. Runs asynchronously after the usage row is committed.
 * Swapping Redis for Kafka later only touches this class.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class UsageStreamPublisher {

    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;
    private final UsageStreamProperties properties;

    @ApplicationModuleListener
    void on(UsageRecorded event) {
        try {
            String payload = mapper.writeValueAsString(event);
            var stream = redis.opsForStream();
            stream.add(StreamRecords.string(Map.of("payload", payload)).withStreamKey(properties.streamKey()));
            stream.trim(properties.streamKey(), properties.maxLength(), true);
        } catch (RuntimeException e) {
            log.warn("Could not publish usage {} to Redis stream: {}", event.usage().requestId(), e.toString());
        }
    }
}
