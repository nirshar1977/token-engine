package com.tokenengine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Token Engine gateway: a modular monolith. Each direct sub-package is a Spring Modulith
 * application module (gateway, session, analyser, optimiser, usage, shared). Modules expose
 * only their {@code api} package and talk to each other through those types and events.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableAsync
public class TokenEngineApplication {

    public static void main(String[] args) {
        SpringApplication.run(TokenEngineApplication.class, args);
    }
}
