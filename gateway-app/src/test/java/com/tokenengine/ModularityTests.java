package com.tokenengine;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/** Fails the build when a module reaches into another module's internal package or modules form a cycle. */
class ModularityTests {

    private final ApplicationModules modules = ApplicationModules.of(TokenEngineApplication.class);

    @Test
    void verifiesModuleBoundaries() {
        modules.verify();
    }

    @Test
    void writesModuleDocumentation() {
        // Generates PlantUML module diagrams under target/spring-modulith-docs
        new Documenter(modules).writeDocumentation();
    }
}
