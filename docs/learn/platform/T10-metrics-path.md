# Task

T10 — Metrics path. Task file: [tasks/platform/T10-metrics-path.md](../../../tasks/platform/T10-metrics-path.md). Spec: [SPEC-platform.md](../../../spec/SPEC-platform.md) (Observability baseline, Success criterion 5, Local container engine), [architecture.md §11](../../architecture.md) (signal flow, SLOs).

# What I changed

| File | What it does |
|---|---|
| `backend/marketplace-api/pom.xml` | Adds `micrometer-registry-prometheus` (runtime scope). |
| `.../resources/application.yml` | Percentile histogram for `http.server.requests`; common tag `application=marketplace-api`. |
| `.../test/.../PrometheusMetricsIT.java` | 2 tests: after a ping the endpoint has histogram buckets for `/api/v1/ping`; the API port does not expose `/actuator/prometheus`. |
| `.../test/.../ApplicationStartupIT.java` | Now `RANDOM_PORT`, so both integration tests share one context and one set of containers. |
| `infra/compose.yaml` | Services `prometheus` (v3.16.0, 15 days retention) and `grafana` (13.2.6, login required, no sign-up, no telemetry), both on `127.0.0.1`, with health checks and volumes. Prometheus has `extra_hosts: devhost:192.168.127.254`. |
| `infra/observability/prometheus.yml` | Scrape jobs `prometheus` and `backend` (`devhost:8091/actuator/prometheus`, label `service=marketplace-api`). |
| `infra/observability/grafana/provisioning/datasources/datasources.yaml` | Prometheus data source (uid `prometheus`), created at start. |
| `infra/.env.example` | `GRAFANA_ADMIN_PASSWORD`. |
| Podman machine (not in Git) | User-mode networking switched on (user decision). |
| `spec/SPEC-platform.md`, `CLAUDE.md` | Networking decision and the `devhost` rule. |

# The real problem

Without metrics, nobody knows whether the platform meets its targets (search p95 < 500 ms, 99.9% availability). Problems show up as user complaints, not as alerts.

Two technical problems appeared on the way:

1. Averages hide slow requests. A p95 needs histogram buckets; Spring Boot does not publish them by default.
2. On this machine Prometheus runs in the Podman WSL machine, but the backend runs on Windows. The container could not reach Windows: `host.containers.internal` pointed into the WSL VM (connection refused), and the Windows gateway address timed out (Hyper-V firewall). The WSL address also changes at each boot.

# Why this solution

- **Pull model (Prometheus scrapes):** the spec design; Prometheus knows when a target is down (`up == 0`), which a push model cannot show.
- **Histogram buckets for HTTP requests only:** `histogram_quantile(0.95, ...)` works across instances; buckets for every metric would multiply the number of series.
- **Common tag `application`:** one dashboard can show all services and filter by one.
- **Management port only:** verified by test: `/actuator/prometheus` returns 404 on the API port.
- **Grafana provisioning from files:** the data source exists after every reset; nothing is clicked by hand.
- **Grafana hardening:** no anonymous access (verified: API without login → 401), no self sign-up, no update or usage calls to the internet.
- **Podman user-mode networking + `devhost`:** gvproxy runs on Windows and maps `192.168.127.254` to Windows `localhost`. The address is fixed, needs no firewall rule and no admin rights, and the change is reversible (`--user-mode-networking=false`). The name `devhost` makes `prometheus.yml` readable and keeps one place to change.

# Production shape

- Prometheus finds targets through Kubernetes service discovery (or a `ServiceMonitor` with the Prometheus Operator), not static targets.
- Long-term storage with Mimir or Thanos (architecture §11.5: metrics kept 13 months, downsampled); high availability with two Prometheus replicas.
- Grafana uses single sign-on (Keycloak) with roles; dashboards and alerts are provisioned from Git.
- Alerts use SLO burn rates (architecture §11.3), routed to on-call.
- The `devhost` trick is not needed: the backend runs in the same cluster network as Prometheus.

# Other possible approaches

1. Push metrics with OTLP to Grafana Alloy, which writes to Prometheus.
2. A Windows firewall rule for the WSL subnet.
3. WSL mirrored networking in `%USERPROFILE%\.wslconfig`.
4. Run the backend in a container during development.
5. Micrometer default timers without histogram buckets.

# Why I did not choose those alternatives

1. It works without machine changes, but it differs from the spec (scrape), loses the `up` signal, and pulls T11 work into T10.
2. Needs admin rights, weakens the firewall, and the WSL subnet changes at boot.
3. Changes every WSL distribution on the machine and needs `wsl --shutdown`; more side effects.
4. Slower edit–run cycle; IDE debugging is harder. Images come in T21.
5. No percentiles that can be aggregated across instances; the SLOs need p95.

# Key concepts to learn

- **Pull vs push metrics:** Prometheus pulls from targets; OTLP pushes to a collector.
- **Histogram buckets:** counters per latency range (`le` = less or equal); `histogram_quantile` estimates percentiles from them.
- **`rate()`:** per-second increase over a window; it needs at least two samples, so a fresh series shows `NaN` (seen here: p95 = NaN right after 5 pings).
- **Cardinality:** the number of label combinations; each combination is a separate time series that costs memory.
- **Grafana provisioning:** YAML files for data sources and dashboards that Grafana loads at start.
- **Podman networking on WSL:** default (NAT) vs user-mode (gvproxy on Windows); `host.containers.internal` is the host of the network namespace, which in a rootless WSL machine is the VM, not Windows.

# Common mistakes

- Using `host.containers.internal` (or `host.docker.internal`) and assuming it means "my Windows PC" on a WSL-based machine.
- Exposing Actuator on the public port so Prometheus can reach it.
- Labels with high cardinality (user id, course id, full URL with ids). Spring uses URI templates (`/api/v1/courses/{id}`), keep it that way.
- Reading p95 right after a few requests: `rate()` needs two scrapes with an increase.
- Grafana with the default `admin/admin` or with anonymous access "for local work".
- Calling a podman-compose service by a name that is not in the same compose project network (`prometheus:9090` works only inside the project network).

# Small example

```yaml
# application.yml
management:
  metrics:
    distribution:
      percentiles-histogram:
        http.server.requests: true
    tags:
      application: ${spring.application.name}
```

```promql
# p95 latency per URI over 5 minutes
histogram_quantile(0.95, sum by (le, uri) (rate(http_server_requests_seconds_bucket{application="marketplace-api"}[5m])))
```

Verified end to end: target `backend` UP; query through Grafana `sum(http_server_requests_seconds_count{uri="/api/v1/ping"})` = 5.

# How to think about this next time

- Before you add a scrape target, ask: "From which network does the scraper connect, and what does this host name mean there?" Test it from inside the container (`podman exec ... wget`).
- Decide which metrics need percentiles before production; adding buckets later loses history.
- Prefer fixed, documented addresses (gvproxy `192.168.127.254`) over addresses that change at boot.
- Verify through the real user path (Grafana), not only the component (Prometheus).
