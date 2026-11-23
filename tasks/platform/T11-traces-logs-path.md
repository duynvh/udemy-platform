# T11: Traces and logs path

Phase 3 · Size M · Dependencies: T10

**Description:** Send backend traces with OTLP to Grafana Alloy, which forwards traces to Tempo and container logs to Loki. Link logs to traces in Grafana.

**Acceptance criteria:**
- [ ] Backend uses `micrometer-tracing-bridge-otel` + OTLP exporter to `http://localhost:4318`; sampling 1.0 locally.
- [ ] Backend logs are JSON (`logging.structured.format.console: ecs`) with `trace.id` and `span.id`.
- [ ] Alloy receives OTLP and sends traces to Tempo; Alloy reads container logs through the Podman API socket and sends them to Loki with label `service`. In the rootful Podman machine, mount `/run/podman/podman.sock` into the Alloy container as `/var/run/docker.sock`.
- [ ] Grafana data sources Tempo and Loki provisioned; Loki derived field on `trace.id` links to Tempo; Tempo "trace to logs" links to Loki.

**Verification:**
- [ ] Call `/api/v1/ping`; in Grafana Explore (Tempo) the trace exists; "Logs for this span" opens the matching Loki line (spec criterion 4, backend part)

**Files:**
- `backend/marketplace-api/pom.xml`, `backend/marketplace-api/src/main/resources/application.yml`
- `infra/compose.yaml`
- `infra/observability/alloy/config.alloy`
- `infra/observability/tempo.yaml`, `infra/observability/loki.yaml`
- `infra/observability/grafana/provisioning/datasources.yaml`
