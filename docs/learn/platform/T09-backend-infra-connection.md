# Task

T09 — Backend connects to infrastructure. Task file: [tasks/platform/T09-backend-infra-connection.md](../../../tasks/platform/T09-backend-infra-connection.md). Spec: [SPEC-platform.md](../../../spec/SPEC-platform.md) (Testing Strategy, Local container engine), [architecture.md §5.1](../../architecture.md) (transactional outbox).

# What I changed

| File | What it does |
|---|---|
| `backend/marketplace-api/pom.xml` | Adds Spring Data JPA, Flyway (+ PostgreSQL support), PostgreSQL driver, Spring Kafka, Spring Data Redis, Spring Modulith JDBC (event registry); test: Spring Boot Testcontainers, Testcontainers PostgreSQL, Kafka, JUnit Jupiter. |
| `.../resources/application.yml` | Shared settings: default profile `local`, `open-in-view: false`, `ddl-auto: validate`, Flyway history in schema `flyway`, Modulith registry in schema `shared` without auto-created tables. |
| `.../resources/application-local.yml` | Profile `local`: imports `../../infra/.env` as properties; datasource as the least-privilege user; Kafka `SASL_PLAINTEXT` with user `marketplace`; Valkey password. |
| `.../db/migration/shared/V0__baseline.sql` | Creates schema `shared`. |
| `.../db/migration/shared/V1__event_publication.sql` | Spring Modulith v2 table `shared.event_publication` (copied from the Modulith 2.0.10 jar). |
| `.../test/.../TestcontainersConfiguration.java` | PostgreSQL (pgvector), Kafka, Valkey containers with `@ServiceConnection`; the same image versions as `infra/compose.yaml`. |
| `.../test/.../ApplicationStartupIT.java` | 5 integration tests: migrations applied, schema exists, registry table exists, Valkey answers `PONG`, Kafka has one broker. |
| `spec/SPEC-platform.md`, `CLAUDE.md`, task file | Corrected: rootless Podman works for Testcontainers; profiles, Flyway rules, one-shot job command. |

# The real problem

Up to T08 the backend was a web app without data. Every business module needs a database, events, and a cache, and it needs them in two places:

1. **Running the app** on the developer machine against the long-running Podman Compose stack, with real passwords.
2. **Automated tests** that must be repeatable on any machine and in CI, without a running stack and without shared data.

The hard parts are the details: where do passwords come from without being in Git? Who owns the database schema (Hibernate or migrations)? How does an event survive a crash between the database commit and the Kafka send?

# Why this solution

- **Two profiles, one source of passwords:** `local` imports `infra/.env` directly (`spring.config.import: optional:file:../../infra/.env[.properties]`), so compose and the backend read the same file and no password is in `application*.yml`. Tests use profile `test` and Testcontainers, which need no passwords.
- **Flyway owns the schema, Hibernate only validates:** `ddl-auto: validate` fails the start when an entity does not match the table, instead of changing the database silently.
- **History table in schema `flyway`, every migration names its schema:** modules own their schemas (`shared`, later `authoring`, ...); no table lands in `public` by accident. The application user created in T06 has `CREATE` on the database, so it can create these schemas, and it owns the tables (verified: `owner=marketplace`).
- **Event registry from Flyway, not from Modulith auto-DDL:** one tool changes the schema; the DDL is copied from the exact Modulith version and the file says so, for upgrades.
- **`@ServiceConnection`:** Spring Boot reads host and port from each container, so tests need no URL properties. Valkey uses `name = "redis"` because it speaks the Redis protocol.
- **Shared containers:** Spring caches the test context; all integration tests with the same configuration reuse the three containers (about 19 s for the first start, then free).
- **`open-in-view: false`:** no lazy database queries while JSON is written; each use case loads its data inside the transaction.

# Production shape

- No `.env` import: Kubernetes injects environment variables from a secret manager (External Secrets Operator). Environment variables with the same names already override the file.
- Kafka uses `SASL_SSL` with SCRAM or mTLS; Valkey uses TLS and an ACL user; PostgreSQL uses `sslmode=verify-full`.
- Flyway runs once per release (an init container or a separate job) with a migration user; the app runs with a user that has only DML rights.
- The event publication registry is monitored: incomplete publications older than a few minutes raise an alert (architecture §11.3 "Event delivery").
- Connection pools are sized for virtual threads: the pool, not the thread count, limits database load.

# Other possible approaches

1. `ddl-auto: update` (Hibernate creates and changes tables).
2. Spring Boot Docker Compose support (`spring-boot-docker-compose`) to start compose from the app.
3. Hard-coded local passwords as defaults in `application.yml`.
4. One shared test database (the compose stack) for integration tests.
5. Testcontainers `@Container` fields in each test class.

# Why I did not choose those alternatives

1. No review, no history, no rollback plan; it can drop or change data in ways nobody saw.
2. It adds a start-up side effect to the app and depends on the compose CLI; the team runs `podman compose` explicitly (decided in the Podman change).
3. Passwords in Git, even "local" ones, get copied; secret scanners (T20) would flag them.
4. Tests then depend on a running stack and on data from other runs; they are not repeatable.
5. Each class starts its own containers; `@ServiceConnection` beans in one configuration are shorter and are shared through context caching.

# Key concepts to learn

- **Spring profile:** a named set of configuration; `spring.profiles.default` applies only when no profile is active.
- **`spring.config.import` with `[.properties]`:** reads a file with another extension (here `.env`) as a properties file.
- **Flyway versioned migration:** `V<version>__<description>.sql`, applied once, in order, recorded in a history table; versions are global, also across folders.
- **Transactional outbox (Spring Modulith registry):** the event is stored in the same transaction as the business change and published after commit; a restart republishes incomplete events.
- **`@ServiceConnection`:** turns a Testcontainers container into Spring connection details (URL, user, password).
- **Test context caching:** Spring reuses an application context for tests with the same configuration in one JVM.
- **Compatible substitute image:** Testcontainers checks image names; a fully qualified name (`docker.io/...`) must be declared compatible with the short name it knows.

# Common mistakes

- Two modules with the same Flyway version (`V1__...` in `authoring` and `V1__...` in `catalog`): Flyway fails with "found more than one migration with version 1". Agree on a version scheme before the second module (for example `V<module-number>_<n>__...`).
- Unqualified table names in migrations: with `default-schema: flyway` the table lands in the `flyway` schema.
- Forgetting `spring.modulith.events.jdbc.schema`: Modulith then looks for `event_publication` in the default schema and fails at the first event.
- Running the jar from another folder: the relative `.env` path does not resolve, the import is optional, and the start fails with "Could not resolve placeholder 'MARKETPLACE_DB_USER'". Run from `backend/marketplace-api` or set the variables.
- Assuming rootful Podman is needed for Testcontainers: on this machine rootless works through `docker_engine`; test first, then change the machine.
- Asserting Flyway script names without the folder: Flyway reports `shared/V0__baseline.sql`.
- Believing "health UP" covers Kafka: Spring Boot has no Kafka health indicator; the local SASL settings are proven only when the first module sends an event.

# Small example

```java
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {
    @Bean
    @ServiceConnection(name = "redis")   // Valkey speaks the Redis protocol
    GenericContainer<?> valkeyContainer() {
        return new GenericContainer<>(DockerImageName.parse("docker.io/valkey/valkey:8.1.12-alpine"))
                .withExposedPorts(6379);
    }
}
```

```yaml
# application-local.yml
spring:
  config:
    import: optional:file:../../infra/.env[.properties]
  datasource:
    username: ${MARKETPLACE_DB_USER}
    password: ${MARKETPLACE_DB_PASSWORD}
```

Local run result: `health: UP | components: db=UP, redis=UP, ...`; Flyway "Successfully applied 2 migrations"; tables `flyway.flyway_schema_history` and `shared.event_publication` owned by `marketplace`.

# How to think about this next time

- Ask: "Where does each secret come from in each environment?" One source per environment, none in Git.
- Ask: "Which tool owns the schema?" Exactly one (Flyway); everything else validates.
- Write the integration test before the configuration: the RED run showed that containers, Valkey, and Kafka already worked, so the GREEN step was only migrations and settings.
- Note what a check does not prove (here: Kafka SASL), and close the gap in the task that first needs it.
