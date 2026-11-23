# T10: Metrics path

Phase 3 · Size M · Dependencies: T09

**Description:** Make backend metrics visible in Grafana: Micrometer Prometheus registry in the backend, Prometheus and Grafana in `infra/compose.yaml` (fully qualified image names), Grafana data source provisioned.

**Acceptance criteria:**
- [ ] `http://localhost:8091/actuator/prometheus` returns metrics, including `http_server_requests_seconds_bucket` (percentile histogram on).
- [ ] Prometheus (port 9090) scrapes the backend management port every 15 s; target is `UP`.
- [ ] Grafana (port 3000) starts with a provisioned Prometheus data source; admin password from `infra/.env`; anonymous access off.

**Verification:**
- [ ] Call `/api/v1/ping` 5 times, then in Grafana Explore: `http_server_requests_seconds_count{uri="/api/v1/ping"}` ≥ 5 (spec criterion 5, backend part)
- [ ] `http://localhost:9090/targets` shows `backend` UP

**Files:**
- `backend/marketplace-api/pom.xml` (`micrometer-registry-prometheus`), `backend/marketplace-api/src/main/resources/application.yml`
- `infra/compose.yaml`
- `infra/observability/prometheus.yml`
- `infra/observability/grafana/provisioning/datasources.yaml`
