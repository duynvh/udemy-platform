# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project state

Online course marketplace (Udemy-style). The repository has only planning documents now; no code exists yet. Code starts with `tasks/platform/T01-maven-parent.md`.

| Path | Content |
|---|---|
| `docs/course-marketplace-plan.md` | Business plan: user stories (US-x.y), releases R1–R3, NFRs |
| `docs/architecture.md` | Solution architecture, Hexagonal CRUD reference (§9), security (§6), observability (§11) |
| `spec/capability-map.md` | Module ids, dependency direction, build order, cross-module decisions — the index of all specs |
| `spec/SPEC-<module>.md` | One spec per module. `SPEC-platform.md` defines code style, testing, boundaries for all modules |
| `tasks/plan.md`, `tasks/todo.md` | Plan and checklist for the current module (now: `platform`) |
| `tasks/<module>/Txx-*.md` | One file per task (user preference): acceptance criteria, verification, files |

## Workflow

Spec-driven, gated: spec → plan → tasks → implement, with human approval at each gate.
- Implement one task file at a time. Tick it in `tasks/todo.md` and stop at each checkpoint for review.
- After each task, write a learning note: `docs/learn/<module>/<task file name>` (example: `tasks/platform/T01-maven-parent.md` → `docs/learn/platform/T01-maven-parent.md`). Use all headings of `docs/learn/_template.md`, in the same order. Write about the real code of that task, not general theory. A task is not done until its note exists.
- A change to an approved spec gets a delta spec (`spec/SPEC-<change>.md`), not an edit of the original.
- R2 and R3 module specs are written only after Release 1 ships.

## Commands (defined in `spec/SPEC-platform.md`; available after the platform tasks)

Run Maven commands in `backend/`. On Windows use `mvnw.cmd` in place of `./mvnw`. The local container engine is Podman, not Docker.

```text
podman machine init --cpus 6 --memory 12288   # once; rootless works (Testcontainers verified in T09)
podman machine set --user-mode-networking      # once, machine stopped; containers reach Windows apps as "devhost"
podman machine start                           # stale "Currently starting"? podman machine stop, then start
podman compose -f infra/compose.yaml run --rm rustfs-init   # buckets; one-shot (never attached "up" for jobs)
podman compose -f infra/compose.yaml up -d          # local infrastructure (needs infra/.env from .env.example)
cd backend
./mvnw -pl marketplace-api spring-boot:run          # backend: API 8090, actuator 8091
./mvnw -pl bff-gateway spring-boot:run              # BFF: 8080, the only browser entry point
./mvnw test                                         # unit tests (*Test)
./mvnw verify                                       # + integration tests (*IT, Testcontainers) + JaCoCo gate
./mvnw -pl marketplace-api test -Dtest=CourseTest   # single test class
./mvnw -pl marketplace-api test -Dtest='CourseTest#updateDetails_notOwner_throwsCourseAccessDeniedException'
./mvnw spotless:apply                               # format Java (palantir-java-format)
cd ..
npm --prefix frontend run dev | test | lint | build | e2e
npm --prefix frontend run api:generate              # Orval client from backend/marketplace-api/target/openapi.json
npm --prefix frontend test -- src/path/File.test.tsx   # single Vitest file
```

- Testcontainers finds Podman through `npipe:////./pipe/docker_engine` with no configuration. Fully qualified images need `asCompatibleSubstituteFor(...)`. Shared containers: `backend/marketplace-api/src/test/java/com/marketplace/TestcontainersConfiguration.java`.
- Profiles: `local` (default) reads `infra/.env` through `spring.config.import` and uses Kafka SASL and the Valkey password; integration tests use `@ActiveProfiles("test")` with Testcontainers (no auth). Run the backend from `backend/marketplace-api` so the relative `.env` path works (`spring-boot:run` does this).
- Flyway: one folder per module under `db/migration/<module>`, history table in schema `flyway`, migrations always name their schema. Versions are global across modules.

## Architecture (big picture)

- **Deployables:** `backend/marketplace-api` (Spring Boot 4 modular monolith, Java 25), `backend/bff-gateway` (Spring Cloud Gateway MVC), `backend/media-worker` (FFmpeg transcode), `frontend` (React 19 + MUI 7 + Vite). `backend/pom.xml` is the Maven parent; `backend/.mvn/settings.xml` uses Maven Central only.
- **Browser auth is BFF only.** The browser holds an `HttpOnly` session cookie; tokens stay in the BFF and are relayed to the backend. Never put tokens in browser storage. Write requests need the `X-XSRF-TOKEN` header.
- **Backend modules** (`com.marketplace.<module>`): identity, authoring, media, catalog, admin, checkout, learning. Spring Modulith enforces boundaries (`ApplicationModules.verify()`).
- **Hexagonal per module:** `api/` (public contract for other modules), `domain/` (pure Java, no Spring/JPA/Kafka/Jackson — ArchUnit fails the build), `application/port/in|out` + `service/`, `adapter/in|out`. Reference implementation: `docs/architecture.md` §9.
- **Module communication:** only through another module's `api` package or Kafka events. Each module owns its PostgreSQL schema and never reads another module's tables. Events go through the Spring Modulith outbox (`@Externalized`) to Kafka topics `<module>.<aggregate>.v1`.
- **Dependency inversions to keep** (see `spec/capability-map.md`): categories belong to `authoring`; `checkout` declares `CourseConsumptionProvider` and `learning` implements it; `learning` checks access, then calls `media.api.PlaybackApi`; `catalog` builds its OpenSearch read model through `authoring.api.PublishedCourseQuery`.
- **Data stores:** PostgreSQL (truth), Valkey (cache, cart, sessions, rate limits), OpenSearch (search read model), RustFS (S3 API only; browser uploads directly with presigned URLs).
- **Observability:** OTLP → Grafana Alloy → Tempo (traces), Loki (logs), Prometheus (metrics), Grafana. Business metrics go through an outbound port (e.g. `CheckoutMetricsPort`), not Micrometer in the domain. No user ids or emails as metric labels.

## Project conventions not in the global rules

- Tests: domain/service tests use named fake classes (`FakeCourseRepository`), not Mockito stubs for ports. JaCoCo requires 80% line coverage on `domain` and `application` packages.
- Errors: RFC 9457 Problem Details; one handler per module for its domain exceptions, a global one in `shared`.
- Ask first before: adding a dependency, changing `infra/compose.yaml` ports, changing CI, changing an event contract in an `api` package, lowering a coverage or security threshold.
- Never edit `frontend/src/shared/api/generated/` (Orval output).
- Prices are integer minor units (`amountMinor`) from a fixed tier list; checkout always takes prices from the server, never from the browser.
- Mermaid diagrams in docs/specs: two levels, max 10 nodes, no dotted arrows (sequence diagrams use `->>` only).
