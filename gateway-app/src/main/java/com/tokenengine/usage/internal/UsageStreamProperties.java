package com.tokenengine.usage.internal;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * @param streamKey  Redis stream the quality service consumes
 * @param maxLength  approximate cap on stream length, so Redis memory stays bounded
 */
@ConfigurationProperties("tokenengine.usage")
record UsageStreamProperties(
        @DefaultValue("tokenengine:usage") String streamKey,
        @DefaultValue("100000") long maxLength) {
}
