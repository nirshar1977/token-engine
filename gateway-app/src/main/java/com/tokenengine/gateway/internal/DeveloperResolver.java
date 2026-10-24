package com.tokenengine.gateway.internal;

import com.tokenengine.shared.api.DeveloperId;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Maps a developer's gateway key to a developer name. Keys live in configuration for the POC;
 * a database-backed version with key rotation is the natural next step.
 */
final class DeveloperResolver {

    private final Map<String, DeveloperId> byKey;
    private final boolean required;

    DeveloperResolver(String developerKeys, boolean required) {
        this.byKey = parse(developerKeys);
        this.required = required;
    }

    /** Empty when the request must be rejected. */
    Optional<DeveloperId> resolve(String key) {
        if (key != null) {
            DeveloperId developer = byKey.get(key.trim());
            if (developer != null) {
                return Optional.of(developer);
            }
        }
        return required ? Optional.empty() : Optional.of(DeveloperId.ANONYMOUS);
    }

    int size() {
        return byKey.size();
    }

    private static Map<String, DeveloperId> parse(String spec) {
        Map<String, DeveloperId> keys = new HashMap<>();
        if (spec == null || spec.isBlank()) {
            return keys;
        }
        for (String entry : spec.split(",")) {
            String trimmed = entry.trim();
            int colon = trimmed.indexOf(':');
            if (colon <= 0 || colon == trimmed.length() - 1) {
                throw new IllegalArgumentException(
                        "tokenengine.gateway.auth.developer-keys entries must look like name:key");
            }
            String name = trimmed.substring(0, colon).trim();
            String key = trimmed.substring(colon + 1).trim();
            if (keys.putIfAbsent(key, new DeveloperId(name)) != null) {
                throw new IllegalArgumentException("Duplicate developer key for " + name);
            }
        }
        return Map.copyOf(keys);
    }
}
