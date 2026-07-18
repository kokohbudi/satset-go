package com.satset;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/**
 * Enforces the modular-monolith boundaries between feature modules
 * (identity, onboarding, catalog, transaction, pricelist, wallet, accounting,
 * quickmenu) plus the leaf {@code digiflazz} module (allowedDependencies={"shared"})
 * at build time — cycles and disallowed dependencies fail the build.
 */
class ModularityTest {

    static final ApplicationModules modules = ApplicationModules.of(SatsetGoApplication.class);

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
