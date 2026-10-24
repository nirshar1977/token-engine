package com.tokenengine.shared.internal;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** @param adminKey key required on the engine's admin API; empty means the admin API is closed */
@ConfigurationProperties("tokenengine.security")
record EngineSecurityProperties(String adminKey) {
}
