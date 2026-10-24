package com.tokenengine.quality;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Separate service that checks optimisation never costs quality. Fed by the gateway's usage events
 * through a Redis stream; owns the {@code quality} schema and shares no code with the gateway (the JSON event
 * is the contract), so it can be deployed and scaled on its own.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class QualityServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(QualityServiceApplication.class, args);
    }
}
