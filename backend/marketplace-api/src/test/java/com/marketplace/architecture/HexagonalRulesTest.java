package com.marketplace.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

/**
 * Hexagonal Architecture rules for every module (docs/architecture.md §4.4).
 * allowEmptyShould(true): a rule stays active even before any module has classes in that layer.
 */
class HexagonalRulesTest {

    private static final JavaClasses PRODUCTION_CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.marketplace");

    @Test
    void domain_anyClass_doesNotDependOnFrameworks() {
        noClasses()
                .that()
                .resideInAPackage("..domain..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "org.springframework..",
                        "jakarta.persistence..",
                        "org.apache.kafka..",
                        "com.fasterxml.jackson..",
                        "tools.jackson..")
                .because("the domain must stay pure Java; frameworks belong in adapters")
                .allowEmptyShould(true)
                .check(PRODUCTION_CLASSES);
    }

    @Test
    void inboundAdapter_anyClass_doesNotDependOnOutboundAdapter() {
        noClasses()
                .that()
                .resideInAPackage("..adapter.in..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("..adapter.out..")
                .because("inbound adapters reach outbound adapters only through ports")
                .allowEmptyShould(true)
                .check(PRODUCTION_CLASSES);
    }

    @Test
    void webAdapter_anyClass_doesNotDependOnApplicationService() {
        noClasses()
                .that()
                .resideInAPackage("..adapter.in.web..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("..application.service..")
                .because("controllers call inbound ports (use cases), not service classes")
                .allowEmptyShould(true)
                .check(PRODUCTION_CLASSES);
    }
}
