package com.marketplace.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketplace.MarketplaceApplication;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/** Module boundaries (Spring Modulith): no cycles, no access to another module's internal packages. */
class ModularityTest {

    private static final ApplicationModules MODULES = ApplicationModules.of(MarketplaceApplication.class);

    @Test
    void verify_currentModules_hasNoBoundaryViolations() {
        MODULES.verify();
    }

    @Test
    void writeDocumentation_currentModules_createsDocsFolder() {
        new Documenter(MODULES).writeDocumentation();

        assertThat(Files.isDirectory(Path.of("target", "spring-modulith-docs"))).isTrue();
    }
}
