# T12: Dashboards and exporters

Phase 3 · Size M · Dependencies: T10

**Description:** Add infrastructure exporters and the two starting Grafana dashboards: "Service overview" and "Kafka".

**Acceptance criteria:**
- [ ] `infra/compose.yaml` adds `postgres-exporter` and `kafka-exporter` (fully qualified image names); Prometheus scrapes both; Valkey metrics through `redis-exporter`.
- [ ] Dashboard "Service overview": request rate, error rate (5xx), p95 duration per service, JVM heap, live virtual threads, HikariCP active connections. Variable `service`.
- [ ] Dashboard "Kafka": messages in per topic, consumer lag per group.
- [ ] Dashboards load from files through Grafana provisioning (no manual import).

**Verification:**
- [ ] Restart Grafana; both dashboards are in folder "Marketplace" and show data for `backend` after 5 ping calls
- [ ] `http://localhost:9090/targets` shows all exporters UP

**Files:**
- `infra/compose.yaml`, `infra/observability/prometheus.yml`
- `infra/observability/grafana/provisioning/dashboards.yaml`
- `infra/observability/grafana/dashboards/service-overview.json`
- `infra/observability/grafana/dashboards/kafka.json`
