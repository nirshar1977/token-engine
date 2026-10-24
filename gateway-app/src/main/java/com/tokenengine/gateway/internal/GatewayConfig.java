package com.tokenengine.gateway.internal;

import java.net.http.HttpClient;
import java.util.Set;
import java.util.concurrent.Executors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Configuration
class GatewayConfig {

    /** One shared client; every call runs on its own virtual thread, so thousands of open streams are cheap. */
    @Bean
    HttpClient upstreamHttpClient(GatewayProperties properties) {
        return HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(properties.upstream().connectTimeout())
                .followRedirects(HttpClient.Redirect.NEVER)
                .executor(Executors.newVirtualThreadPerTaskExecutor())
                .build();
    }

    @Bean
    UpstreamProxy upstreamProxy(HttpClient upstreamHttpClient, GatewayProperties properties, ObjectMapper mapper) {
        return new UpstreamProxy(
                upstreamHttpClient,
                properties.upstream().baseUrl(),
                properties.upstream().responseHeadersTimeout(),
                Set.of(properties.auth().header()),
                mapper);
    }

    @Bean
    DeveloperResolver developerResolver(GatewayProperties properties) {
        DeveloperResolver resolver = new DeveloperResolver(properties.auth().developerKeys(), properties.auth().required());
        if (properties.auth().required() && resolver.size() == 0) {
            log.warn("No developer keys configured: every proxied request will be rejected. "
                    + "Set TOKENENGINE_DEVELOPER_KEYS=name:key,...");
        }
        return resolver;
    }
}
