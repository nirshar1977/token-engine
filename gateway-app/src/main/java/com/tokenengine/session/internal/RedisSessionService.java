package com.tokenengine.session.internal;

import com.tokenengine.session.api.SessionContext;
import com.tokenengine.session.api.SessionService;
import com.tokenengine.shared.api.DeveloperId;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Session state lives in Redis so the gateway stays stateless and can run as many copies as needed.
 * Key: {@code tokenengine:session:<developer>:<session>} (hash with count and startedAt).
 */
@Slf4j
@Service
@RequiredArgsConstructor
class RedisSessionService implements SessionService {

    private static final String DEFAULT_SESSION = "default";

    private final StringRedisTemplate redis;
    private final SessionProperties properties;

    @Override
    public SessionContext touch(DeveloperId developer, String sessionHint) {
        String sessionId = sessionHint == null || sessionHint.isBlank() ? DEFAULT_SESSION : sessionHint;
        String key = "tokenengine:session:" + developer.value() + ":" + sessionId;
        Instant now = Instant.now();
        try {
            var hash = redis.opsForHash();
            Long count = hash.increment(key, "count", 1);
            hash.putIfAbsent(key, "startedAt", now.toString());
            redis.expire(key, properties.ttl());
            Object started = hash.get(key, "startedAt");
            Instant startedAt = started == null ? now : Instant.parse(started.toString());
            return new SessionContext(sessionId, count == null ? 1 : count, startedAt, false);
        } catch (RuntimeException e) {
            log.warn("Session store unavailable, continuing without session context: {}", e.getMessage());
            return new SessionContext(sessionId, 0, now, true);
        }
    }
}
