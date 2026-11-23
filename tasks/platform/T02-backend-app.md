# T02: Backend application with ping endpoint

Phase 1 · Size M · Dependencies: T01

**Description:** Create the `marketplace-api` Maven module in `backend/marketplace-api` (artifact `marketplace-api`): a Spring Boot web app with virtual threads, Actuator on a separate management port, and `GET /api/v1/ping`. This is the walking skeleton path that later tasks make observable.

**Acceptance criteria:**
- [ ] `GET http://localhost:8090/api/v1/ping` returns 200 `{"status":"ok"}`.
- [ ] Actuator runs on port 8091 and exposes only `health`, `info`, `prometheus`; port 8090 returns 404 for `/actuator/**`.
- [ ] `spring.threads.virtual.enabled=true`.

**Verification** (run in `backend/`):
- [ ] `./mvnw -pl marketplace-api test` passes, including `ping_anyRequest_returnsOk` (`@WebMvcTest`)
- [ ] `./mvnw -pl marketplace-api spring-boot:run`, then `curl http://localhost:8091/actuator/health` returns `{"status":"UP"}`

**Files:**
- `backend/marketplace-api/pom.xml` (and module entry `marketplace-api` in `backend/pom.xml`)
- `backend/marketplace-api/src/main/java/com/marketplace/MarketplaceApplication.java`
- `backend/marketplace-api/src/main/java/com/marketplace/shared/adapter/in/web/PingController.java`
- `backend/marketplace-api/src/main/resources/application.yml`
- `backend/marketplace-api/src/test/java/com/marketplace/shared/adapter/in/web/PingControllerTest.java`
