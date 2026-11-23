# T09: Backend connects to infrastructure

Phase 2 · Size M · Dependencies: T02, T06

**Description:** Connect the backend to PostgreSQL (with Flyway), Kafka, and Valkey. Add one shared Testcontainers configuration so that every integration test gets real services without `podman compose up`. Testcontainers runs on the local Podman machine.

**Acceptance criteria:**
- [ ] `application.yml` reads connection values from environment variables with local defaults (Compose ports).
- [ ] Flyway runs from `classpath:db/migration/{module}`; a baseline script `V0__baseline.sql` creates schema `shared`.
- [ ] `TestcontainersConfiguration` defines `@ServiceConnection` beans for PostgreSQL (`docker.io/pgvector/pgvector:pg18`), Kafka (`docker.io/apache/kafka`), Valkey; containers are reused across test classes.
- [ ] Testcontainers works with Podman. Result: rootless Podman works through `npipe:////./pipe/docker_engine` without `.testcontainers.properties`, and Ryuk runs. Fallback only if Ryuk fails: `TESTCONTAINERS_RYUK_DISABLED=true`.
- [ ] Spring Modulith event publication registry (JDBC) is on; its table is created by Flyway.

**Verification** (run in `backend/`):
- [ ] `./mvnw -pl marketplace-api verify` passes, including `ApplicationStartupIT.contextLoads_allContainers_startsAndMigrates`
- [ ] With `podman compose` up: `./mvnw -pl marketplace-api spring-boot:run` starts; log shows Flyway "Successfully applied"

**Files:**
- `backend/marketplace-api/pom.xml` (data-jpa, flyway, postgresql, kafka, data-redis, modulith-events-jdbc, testcontainers)
- `backend/marketplace-api/src/main/resources/application.yml`
- `backend/marketplace-api/src/main/resources/db/migration/shared/V0__baseline.sql`
- `backend/marketplace-api/src/test/java/com/marketplace/TestcontainersConfiguration.java`
- `backend/marketplace-api/src/test/java/com/marketplace/ApplicationStartupIT.java`
