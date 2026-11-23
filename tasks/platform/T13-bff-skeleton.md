# T13: BFF skeleton

Phase 4 · Size M · Dependencies: T11

**Description:** Create the `bff-gateway` Maven module in `backend/bff-gateway` with Spring Cloud Gateway Server Web MVC. It relays `/api/**` to the backend and has the same observability setup. No login yet (that is the `bff-gateway` module plan).

**Acceptance criteria:**
- [ ] `GET http://localhost:8080/api/v1/ping` returns the backend response.
- [ ] BFF exposes Actuator on port 8081 (`health`, `info`, `prometheus`); Prometheus scrapes it.
- [ ] BFF sends OTLP traces; W3C `traceparent` is passed to the backend, so BFF and backend spans share one trace.
- [ ] Security headers on every response: `X-Content-Type-Options: nosniff`, `Referrer-Policy: strict-origin-when-cross-origin`, `X-Frame-Options: DENY`.

**Verification** (Maven in `backend/`):
- [ ] `./mvnw -pl bff-gateway verify` passes: `ping_throughGateway_returnsBackendBody` (WireMock as backend), `anyResponse_hasSecurityHeaders`
- [ ] Manual: one ping through port 8080 shows one trace with 2 services in Tempo (spec criterion 4); Prometheus shows the BFF `http_server_requests_seconds_count` (spec criterion 5)

**Files:**
- `backend/bff-gateway/pom.xml` (and module entry `bff-gateway` in `backend/pom.xml`)
- `backend/bff-gateway/src/main/java/com/marketplace/bff/BffGatewayApplication.java`
- `backend/bff-gateway/src/main/resources/application.yml`
- `backend/bff-gateway/src/test/java/com/marketplace/bff/GatewayRouteIT.java`
- `infra/observability/prometheus.yml`
