# T22: Load test setup

Phase 5 · Size S · Dependencies: T13

**Description:** Add a k6 service to `infra/compose.yaml` (profile `tools`, not started by default) with a smoke script for the ping path. Module plans add real load scripts later (catalog search, learning heartbeats).

**Acceptance criteria:**
- [ ] Compose service `k6` (fully qualified image `docker.io/grafana/k6`, pinned tag, profile `tools`) mounts `infra/k6` at `/scripts` and sends results to Prometheus remote write.
- [ ] `ping.js`: 50 virtual users for 1 minute against `http://bff-gateway:8080/api/v1/ping`; thresholds `http_req_failed < 1%`, `p(95) < 200 ms`.

**Verification:**
- [ ] `podman compose -f infra/compose.yaml --profile tools run --rm k6 run /scripts/ping.js` exits 0
- [ ] k6 metrics are visible in Grafana Explore (`k6_http_reqs_total`)

**Files:**
- `infra/compose.yaml`
- `infra/k6/ping.js`
