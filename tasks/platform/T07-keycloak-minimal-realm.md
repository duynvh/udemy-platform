# T07: Keycloak with minimal realm

Phase 2 · Size S · Dependencies: T06

**Description:** Add Keycloak 26 to Compose. It uses its own PostgreSQL database and imports a minimal `marketplace` realm on start. The `identity` plan adds the full realm settings later.

**Acceptance criteria:**
- [ ] The Keycloak image name is fully qualified (`quay.io/keycloak/keycloak:26.x`).
- [ ] Keycloak runs on port 8180 with health enabled (`KC_HEALTH_ENABLED=true`) and stores data in PostgreSQL database `keycloak`.
- [ ] Realm `marketplace` imported with client `bff-gateway` (confidential, standard flow, PKCE S256, redirect `http://localhost:8080/login/oauth2/code/keycloak`), audience mapper `marketplace-api`, realm roles `student` (default), `instructor`, `admin`, `finance`.
- [ ] One test user `student@local.test` with role `student`; its password comes from `infra/.env`.

**Verification:**
- [ ] `podman compose -f infra/compose.yaml up -d keycloak` → healthy
- [ ] `curl http://localhost:8180/realms/marketplace/.well-known/openid-configuration` returns 200 with `issuer` = `http://localhost:8180/realms/marketplace`

**Files:**
- `infra/compose.yaml`
- `infra/keycloak/marketplace-realm.json`
- `infra/postgres/init-schemas.sql` (add database `keycloak`)
- `infra/.env.example`
