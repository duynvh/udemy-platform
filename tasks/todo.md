# Todo: platform

Plan: [plan.md](plan.md) · Spec: [SPEC-platform.md](../spec/SPEC-platform.md)

A task is done only when its learning note `docs/learn/platform/<task file name>` exists (template: [docs/learn/_template.md](../docs/learn/_template.md)).

## Phase 1: Build skeleton
- [x] [T01 Maven parent and wrapper](platform/T01-maven-parent.md) · note: [docs/learn/platform/T01-maven-parent.md](../docs/learn/platform/T01-maven-parent.md)
- [x] [T02 Backend application with ping endpoint](platform/T02-backend-app.md) · note: [docs/learn/platform/T02-backend-app.md](../docs/learn/platform/T02-backend-app.md)
- [x] [T03 Problem Details handler](platform/T03-problem-handler.md) · note: [docs/learn/platform/T03-problem-handler.md](../docs/learn/platform/T03-problem-handler.md)
- [x] [T04 Shared kernel types](platform/T04-shared-kernel-types.md) · note: [docs/learn/platform/T04-shared-kernel-types.md](../docs/learn/platform/T04-shared-kernel-types.md)
- [x] [T05 Architecture tests](platform/T05-architecture-tests.md) · note: [docs/learn/platform/T05-architecture-tests.md](../docs/learn/platform/T05-architecture-tests.md)
- [ ] **Checkpoint 1:** `./mvnw verify` passes · health `UP` · human review

## Phase 2: Infrastructure
- [x] [T06 Compose data stores](platform/T06-compose-data-stores.md) · note: [docs/learn/platform/T06-compose-data-stores.md](../docs/learn/platform/T06-compose-data-stores.md)
- [x] [T07 Keycloak with minimal realm](platform/T07-keycloak-minimal-realm.md) · note: [docs/learn/platform/T07-keycloak-minimal-realm.md](../docs/learn/platform/T07-keycloak-minimal-realm.md)
- [ ] [T08 RustFS buckets init](platform/T08-rustfs-init.md)
- [ ] [T09 Backend connects to infrastructure](platform/T09-backend-infra-connection.md)
- [ ] **Checkpoint 2:** all services healthy < 3 min · Flyway runs · Testcontainers IT passes · human review

## Phase 3: Observability
- [ ] [T10 Metrics path](platform/T10-metrics-path.md)
- [ ] [T11 Traces and logs path](platform/T11-traces-logs-path.md)
- [ ] [T12 Dashboards and exporters](platform/T12-dashboards-exporters.md)
- [ ] **Checkpoint 3:** metrics in Prometheus · trace in Tempo linked to log in Loki · human review

## Phase 4: Edge and web
- [ ] [T13 BFF skeleton](platform/T13-bff-skeleton.md)
- [ ] [T14 Media worker skeleton](platform/T14-media-worker-skeleton.md)
- [ ] [T15 Web tooling](platform/T15-web-tooling.md)
- [ ] [T16 Web app shell](platform/T16-web-app-shell.md)
- [ ] [T17 API client generation](platform/T17-api-client.md)
- [ ] [T18 E2E setup](platform/T18-e2e-setup.md)
- [ ] **Checkpoint 4:** one trace BFF → backend · web shell runs · web test and lint pass · human review

## Phase 5: CI and security
- [ ] [T19 CI build and test stages](platform/T19-ci-build-test.md)
- [ ] [T20 Security scans](platform/T20-security-scans.md)
- [ ] [T21 Images and SBOM](platform/T21-images-sbom.md)
- [ ] [T22 Load test setup](platform/T22-load-test-setup.md)
- [ ] **Checkpoint 5:** all 7 success criteria of SPEC-platform.md pass · human review
