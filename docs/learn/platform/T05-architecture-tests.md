# Task

T05 — Architecture tests. Task file: [tasks/platform/T05-architecture-tests.md](../../../tasks/platform/T05-architecture-tests.md). Spec: [SPEC-platform.md](../../../spec/SPEC-platform.md) (Testing Strategy: Architecture level), [architecture.md §4.4](../../architecture.md) (rules enforced by ArchUnit), §3 (module rules).

# What I changed

| File | What it does |
|---|---|
| `backend/pom.xml` | Property `archunit.version` = 1.4.2 and a managed `archunit-junit5` dependency. |
| `backend/marketplace-api/pom.xml` | Test dependencies: `spring-modulith-starter-test`, `spring-modulith-docs`, `archunit-junit5`. |
| `.../architecture/ModularityTest.java` | `ApplicationModules.of(MarketplaceApplication.class).verify()` and the `Documenter` that writes `target/spring-modulith-docs` (AsciiDoc + PlantUML per module). |
| `.../architecture/HexagonalRulesTest.java` | Three ArchUnit rules: domain has no framework imports; inbound adapters do not use outbound adapters; web adapters do not use application services. Each rule has `because(...)` and `allowEmptyShould(true)`. |

# The real problem

Architecture rules written only in a document are broken within weeks. A developer adds `@Entity` to a domain class "to save time", or a controller calls a JPA repository directly. Each shortcut looks small; together they turn the modular monolith into a "big ball of mud" where nothing can be split out or tested in isolation.

Code review does not catch this reliably: a reviewer sees one diff, not the dependency graph.

# Why this solution

- **Rules as tests:** the build fails on a violation, on every machine and in CI. The rule and its reason live in the code.
- **Spring Modulith `verify()`:** it knows the module model (each direct sub-package of `com.marketplace` is a module) and checks two things ArchUnit alone does not know: no cycles between modules, and no access to another module's internal packages.
- **ArchUnit for the Hexagonal layers:** Modulith checks *between* modules; ArchUnit checks *inside* each module (domain, application, adapter).
- **`allowEmptyShould(true)`:** today there is no `adapter.out` or `application.service` package. Without this flag ArchUnit fails "rule did not match any classes". With it, the rule waits and starts to protect the first module that adds such a package.
- **Import without tests (`DO_NOT_INCLUDE_TESTS`):** test code may use Spring in any package (for example the test-only controller in T03); the rules apply to production code.
- **Same ArchUnit version as Modulith (1.4.2):** the newest 1.5.1 exists, but two versions on one classpath can give `NoSuchMethodError` inside Modulith.
- **Proof that the rule works:** a temporary `@Component` class in `shared.domain` made the test fail with the message `Class <...TemporaryViolation> is annotated with <org.springframework.stereotype.Component>`. Then the class was deleted. A rule that never fails proves nothing.

# Production shape

- CI runs these tests in the unit stage (`./mvnw test`, T19), so a pull request with a violation cannot be merged.
- `target/spring-modulith-docs` can be published as a build artifact: always-current module diagrams for new team members.
- When a module grows, add rules for it here (for example "only `checkout` may use the Stripe SDK"), not in a wiki.
- If a module must later become its own service, the clean boundaries make the split a move of packages, not a rewrite.

# Other possible approaches

1. One Maven module per business module (compile-time boundaries).
2. Java Platform Module System (`module-info.java`).
3. Only code review and documentation.
4. Only ArchUnit, without Spring Modulith.

# Why I did not choose those alternatives

1. Many Maven modules give hard boundaries but slow builds and much POM boilerplate; Modulith gives the same checks in one module with less cost (ADR-001 in architecture.md).
2. JPMS works badly with Spring reflection and many libraries; the effort is high for this team size.
3. Not enforced; see "The real problem".
4. ArchUnit can check package rules, but Modulith already understands module APIs, events, and named interfaces, and it adds integration-test support (`@ApplicationModuleTest`) and documentation.

# Key concepts to learn

- **Fitness function:** an automated test that checks an architecture property (here: dependency direction).
- **Spring Modulith application module:** a direct sub-package of the main application package; its base package is the public API, sub-packages are internal by default.
- **ArchUnit rule:** a fluent sentence `noClasses().that()...should()...` checked against imported bytecode.
- **`because(...)`:** the reason shown in the failure message; it teaches the next developer why the rule exists.
- **Dependency rule (Hexagonal / Clean Architecture):** source code dependencies point inward, toward the domain.

# Common mistakes

- Rules without a failing proof: a typo in a package pattern (`..domian..`) makes the rule match nothing and pass forever. `allowEmptyShould(true)` hides this, so test each new rule once with a deliberate violation.
- Importing test classes into the rule check: test helpers then cause false violations.
- The `shared` module trap (seen in the Modulith docs output: `module-shared`): when the first business module uses `com.marketplace.shared.domain.Money`, `verify()` will report access to an internal package of `shared`. Fix it then by marking `shared` as an open module (`@ApplicationModule(type = Type.OPEN)` in `com/marketplace/shared/package-info.java`, from `spring-modulith-api`), not by moving types.
- Two ArchUnit versions on the classpath (your explicit one and Modulith's transitive one).
- Adding exceptions to rules for "just this one class": the exception list grows until the rule means nothing.

# Small example

```java
@Test
void domain_anyClass_doesNotDependOnFrameworks() {
    noClasses().that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("org.springframework..", "jakarta.persistence..", "org.apache.kafka..",
                    "com.fasterxml.jackson..", "tools.jackson..")
            .because("the domain must stay pure Java; frameworks belong in adapters")
            .allowEmptyShould(true)
            .check(PRODUCTION_CLASSES);
}
```

Failure output with the deliberate violation:

```text
Rule 'no classes that reside in a package '..domain..' should depend on classes that reside in any package [...],
because the domain must stay pure Java; frameworks belong in adapters' was violated (1 times):
Class <com.marketplace.shared.domain.TemporaryViolation> is annotated with <org.springframework.stereotype.Component>
```

# How to think about this next time

- For every architecture rule in a document, ask: "Which test fails when someone breaks it?" If none, write one.
- Make each new rule fail once on purpose before you trust it.
- Put the reason in `because(...)`: the failure message is the documentation people actually read.
- Note: Spring Boot 4 uses Jackson 3 (`tools.jackson..`), so the domain rule lists both Jackson 2 and Jackson 3 packages.
