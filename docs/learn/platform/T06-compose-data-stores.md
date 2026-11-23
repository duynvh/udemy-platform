# Task

T06 — Compose data stores. Task file: [tasks/platform/T06-compose-data-stores.md](../../../tasks/platform/T06-compose-data-stores.md). Spec: [SPEC-platform.md](../../../spec/SPEC-platform.md) (Local container engine, Local ports, Security baseline, Success criterion 1).

# What I changed

| File | What it does |
|---|---|
| `infra/compose.yaml` | Six services with pinned, fully qualified images, health checks, named volumes, and ports bound to `127.0.0.1`: PostgreSQL 18 + pgvector 0.8.7, Valkey 8.1.12, Kafka 4.1.1 (KRaft, SASL/PLAIN), OpenSearch 3.10.0 (security plugin on), RustFS 1.0.6, Mailpit v1.34.0. |
| `infra/.env.example` | Every credential with a local dummy value. Copied to `infra/.env`, which Git ignores. |
| `infra/postgres/init-schemas.sql` | On first start: creates the application role `marketplace` with only `CONNECT` and `CREATE` on database `marketplace`. Reads the name and password from the environment with `\getenv`. |
| `spec/SPEC-platform.md` | Local container engine section: Podman 6 WSL, podman-compose 1.6, WSL memory, stale-state fix, Docker API pipe, `127.0.0.1` binding. |
| Docs (by a helper agent) | MinIO replaced by RustFS everywhere; T08 renamed to `T08-rustfs-init.md`. |
| Machine setup (not in Git) | `pip install --user podman-compose`; its `Scripts` folder added to the user `PATH`. |

# The real problem

Every module needs real infrastructure: PostgreSQL for data, Kafka for events, Valkey for cache, OpenSearch for search, object storage for files, and an SMTP server for emails. If each developer installs these by hand, versions and settings differ, and "works on my machine" bugs follow.

A second problem is that local setups are often insecure by habit: no passwords, ports open to the whole network, containers as root. These habits then move to shared environments. The spec requires passwords and non-root users also locally.

# Why this solution

- **One compose file, one command:** `podman compose -f infra/compose.yaml up -d` starts all six services in about 30 seconds.
- **Pinned, fully qualified images:** the same bits on every machine; Podman does not ask which registry a short name means.
- **Health checks with tools that exist in each image:** checked first (BusyBox `sh` checks only the first name in `command -v a b c`, so each tool was checked one by one). Kafka uses a bash TCP check instead of the Kafka CLI, because the CLI starts a second JVM every 5 seconds.
- **`${VAR:?message}`:** compose stops with a clear message when `infra/.env` misses a value, instead of starting a service without a password.
- **Least-privilege database role:** the backend uses `marketplace`, not the superuser. Verified: `create role` fails with "permission denied".
- **Kafka SASL on both client listeners:** `INTERNAL` (containers, `kafka:29092`) and `EXTERNAL` (host, `localhost:9092`). Verified: the right password creates a topic; a wrong one gets "Authentication failed". Auto-create of topics is off, so a typo in a topic name fails instead of creating a new topic.
- **RustFS instead of MinIO:** MinIO no longer publishes community images (Docker Hub returns 404). RustFS speaks the same S3 API, and the code uses only the S3 API.
- **`127.0.0.1` port binding:** other machines on the Wi-Fi cannot reach the database.
- **podman-compose:** the user's choice; it is a Python tool from the Podman project.

# Production shape

- No compose in production: managed services (Amazon RDS for PostgreSQL, Amazon MSK or Confluent for Kafka, ElastiCache/Valkey, Amazon OpenSearch Service, S3) or Kubernetes operators.
- Kafka uses SASL/SCRAM or mTLS with TLS encryption (`SASL_SSL`), not `SASL_PLAINTEXT`; ACLs limit each module to its own topics (architecture §6.3).
- Secrets come from a secret manager (Vault or OpenBao + External Secrets Operator), never from a `.env` file.
- OpenSearch uses real certificates, not the demo certificates.
- The same rules stay: pinned image versions, health checks, least-privilege users, no public ports for data stores.

# Other possible approaches

1. Docker Desktop with Docker Compose.
2. Testcontainers only, no compose stack.
3. Install the services directly on Windows.
4. A shared development server with the data stores.
5. `docker-compose` v2 binary as the compose provider for Podman.

# Why I did not choose those alternatives

1. The machine has Podman, not Docker; Docker Desktop also needs a license in larger companies.
2. Tests use Testcontainers (T09), but running the app and debugging by hand needs long-running services.
3. Different versions per developer, hard to reset, Windows-only differences.
4. Developers break each other's data; it needs a network connection; it costs money.
5. It is the more complete provider, but the user chose podman-compose. Keep this option if a compose feature (for example `depends_on: condition: service_healthy`) does not work.

# Key concepts to learn

- **Compose project:** a named group of services, networks, and volumes (`name: marketplace` → containers `marketplace_postgres_1`, ...).
- **Health check vs running:** "Up" means the process started; "healthy" means the check command passes (the service accepts work).
- **Named volume:** storage managed by Podman that survives `down` (removed only with `down -v`). If the mount folder exists in the image, the volume gets that folder's owner; otherwise root owns it and a non-root process cannot write.
- **KRaft:** Kafka without ZooKeeper; one process can be broker and controller.
- **SASL/PLAIN:** username/password authentication for Kafka; encrypt it with TLS outside local development.
- **`docker-entrypoint-initdb.d`:** scripts that the PostgreSQL image runs once on an empty data folder.
- **Rootless container:** "root" inside the container maps to an unprivileged user on the host (`HUSER 1000` in `podman top`).

# Common mistakes

- Short image names (`postgres:18`) with Podman: it asks which registry, or pulls from an unexpected one.
- Mounting `/var/lib/postgresql/data` with PostgreSQL 18 images: the data folder moved to a versioned sub-folder; mount `/var/lib/postgresql`.
- Editing `init-schemas.sql` and expecting a change: it runs only on an empty volume. Run `podman compose -f infra/compose.yaml down -v` to reset.
- Forgetting `$$` in compose health checks: `$VAR` is replaced by compose at parse time; `$$VAR` reaches the container shell.
- Passwords on the command line (`--requirepass`): they show in the process list (`podman top`). Acceptable for local Valkey, not for shared machines.
- Testing only "healthy": verify the security too (wrong password refused, app user without admin rights).
- `podman machine list` says "Currently starting" forever: the state is stale; `podman machine stop` then `start`.

# Small example

```powershell
copy infra\.env.example infra\.env
podman compose -f infra/compose.yaml up -d
podman ps --filter "label=io.podman.compose.project=marketplace" --format "{{.Names}} {{.Status}}"
# marketplace_postgres_1 Up 27 seconds (healthy)  ... 6 of 6 healthy
podman exec marketplace_valkey_1 valkey-cli ping
# NOAUTH Authentication required.
podman compose -f infra/compose.yaml down        # keep data
podman compose -f infra/compose.yaml down -v     # delete data, rerun init scripts
```

# How to think about this next time

- For each service ask four questions: Which exact version? How do I know it is ready (health check)? Who can reach it (port binding, password)? Which user runs it?
- Check the image before you write the health check: which tools does it have, which user runs it, which folders exist?
- Verify the negative case: a missing password must fail, not just the correct one work.
- When a vendor stops publishing images, depend on the protocol (S3 API), not the product.
