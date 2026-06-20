package com.satset;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/**
 * Enforces the modular-monolith boundaries between feature modules
 * (identity, onboarding, catalog, transaction) at build time.
 */
class ModularityTest {

    static final ApplicationModules modules = ApplicationModules.of(OmnipAuthenticationServiceApplication.class);

    @Test
    void verifiesModularStructure() {
        modules.verify();
    }

    @Test
    void writeDocumentation() {
        // prints the detected module structure to the console for inspection
        modules.forEach(m -> System.out.println("MODULE: " + m.getDisplayName()));
    }
}
