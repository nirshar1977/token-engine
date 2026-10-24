package com.tokenengine.gateway.internal;

import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("tokenengine.gateway")
record GatewayProperties(@DefaultValue Upstream upstream, @DefaultValue Auth auth) {

    /**
     * @param baseUrl                the provider to forward to
     * @param responseHeadersTimeout how long to wait for the provider to start answering (streams may run longer)
     */
    record Upstream(
            @DefaultValue("https://api.anthropic.com") URI baseUrl,
            @DefaultValue("10s") Duration connectTimeout,
            @DefaultValue("10m") Duration responseHeadersTimeout) {
    }

    /**
     * @param header        header carrying the developer's gateway key (set via ANTHROPIC_CUSTOM_HEADERS)
     * @param required      reject requests without a known key; turn off only for local experiments
     * @param developerKeys comma-separated {@code name:key} pairs, e.g. {@code nir:k1,dana:k2}
     */
    record Auth(
            @DefaultValue("x-token-engine-key") String header,
            @DefaultValue("true") boolean required,
            @DefaultValue("") String developerKeys) {
    }
}
