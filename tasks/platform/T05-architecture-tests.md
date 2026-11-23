# T05: Architecture tests

Phase 1 · Size S · Dependencies: T02

**Description:** Add the tests that enforce Hexagonal Architecture and module boundaries. They pass now with no business module and protect every module added later.

**Acceptance criteria:**
- [ ] `ModularityTest`: `ApplicationModules.of(MarketplaceApplication.class).verify()`; it also writes module documentation to `target/spring-modulith-docs`.
- [ ] `HexagonalRulesTest` (ArchUnit, `allowEmptyShould(true)`): `..domain..` does not depend on `org.springframework..`, `jakarta.persistence..`, `org.apache.kafka..`, `com.fasterxml.jackson..`; `..adapter.in..` does not depend on `..adapter.out..`; `..adapter.in.web..` does not depend on `..application.service..`.
- [ ] A deliberate violation (temporary class importing Spring in `shared.domain`) makes the test fail; remove it after the check.

**Verification** (run in `backend/`):
- [ ] `./mvnw -pl marketplace-api test -Dtest='ModularityTest,HexagonalRulesTest'` passes
- [ ] Manual: the deliberate violation fails with a readable message

**Files:**
- `backend/marketplace-api/pom.xml` (add `spring-modulith-starter-test`, `archunit-junit5`)
- `backend/marketplace-api/src/test/java/com/marketplace/architecture/ModularityTest.java`
- `backend/marketplace-api/src/test/java/com/marketplace/architecture/HexagonalRulesTest.java`
