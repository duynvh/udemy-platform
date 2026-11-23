# T06: Compose data stores

Phase 2 · Size M · Dependencies: none (can run in parallel with Phase 1)

**Description:** Create `infra/compose.yaml` with the data services: PostgreSQL (pgvector image), Valkey, Kafka (KRaft, single node), OpenSearch (single node, security plugin on), RustFS (`docker.io/rustfs/rustfs:1.0.6`, ports 9000 and 9001, volume `/data`, credentials from `RUSTFS_ACCESS_KEY` and `RUSTFS_SECRET_KEY`), Mailpit. All with health checks, pinned tags, passwords from `infra/.env`.

**Acceptance criteria:**
- [ ] Every image name is fully qualified (`docker.io/...`, `quay.io/...`). Podman can ask which registry to use for a short name.
- [ ] Every service has a pinned image tag, a `healthcheck`, a named volume, and the ports from the spec's port table.
- [ ] Every credential comes from `infra/.env`; `infra/.env.example` lists each variable with a dummy value. Valkey has `requirepass`; OpenSearch has the admin password set.
- [ ] Memory limits: OpenSearch and Kafka heap 512 MB each.
- [ ] Containers run as non-root where the image supports it (PostgreSQL, Valkey, RustFS, Mailpit).

**Verification** (Podman machine started, see SPEC-platform "Local container engine"):
- [ ] `copy infra\.env.example infra\.env`, then `podman compose -f infra/compose.yaml up -d`
- [ ] `podman compose -f infra/compose.yaml ps` shows all 6 services `healthy` within 3 minutes
- [ ] `podman compose -f infra/compose.yaml config` has no warnings

**Files:**
- `infra/compose.yaml`
- `infra/.env.example`
- `infra/postgres/init-schemas.sql` (creates database `marketplace` and the application user)
