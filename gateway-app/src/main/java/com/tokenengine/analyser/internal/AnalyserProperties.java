package com.tokenengine.analyser.internal;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** @param minBlockBytes blocks smaller than this are ignored (short repeats are cheap and often legitimate) */
@ConfigurationProperties("tokenengine.analyser")
record AnalyserProperties(@DefaultValue("1024") int minBlockBytes) {
}
