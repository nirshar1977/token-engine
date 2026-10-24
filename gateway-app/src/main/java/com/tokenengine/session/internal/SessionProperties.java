package com.tokenengine.session.internal;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** @param ttl sliding expiry of a session; refreshed on every request */
@ConfigurationProperties("tokenengine.session")
record SessionProperties(@DefaultValue("2h") Duration ttl) {
}
