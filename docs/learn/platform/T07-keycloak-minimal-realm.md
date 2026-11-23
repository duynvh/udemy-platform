# Task

T07 — Keycloak with minimal realm. Task file: [tasks/platform/T07-keycloak-minimal-realm.md](../../../tasks/platform/T07-keycloak-minimal-realm.md). Spec: [SPEC-identity.md](../../../spec/SPEC-identity.md) (Keycloak realm), [SPEC-bff-gateway.md](../../../spec/SPEC-bff-gateway.md) (login flow), [architecture.md §6](../../architecture.md) (security, ADR-007 BFF).

# What I changed

| File | What it does |
|---|---|
| `infra/compose.yaml` | Service `keycloak` (`quay.io/keycloak/keycloak:26.8.2`, dev mode, `--import-realm`), port `127.0.0.1:8180`, health check on the internal management port 9000, waits for a healthy PostgreSQL. PostgreSQL gets the Keycloak database variables and a second init script. |
| `infra/postgres/init-keycloak.sql` | Creates role `keycloak` and database `keycloak` owned by it; removes `CONNECT` for `PUBLIC`. |
| `infra/keycloak/marketplace-realm.json` | Realm `marketplace`: roles `student`, `instructor`, `admin`, `finance`; default role → `student`; confidential client `bff-gateway` (standard flow only, PKCE S256, one redirect URI, audience mapper `marketplace-api`); test user `student@local.test`. Secrets are `${...}` placeholders. |
| `infra/.env.example` | Keycloak database role, bootstrap admin, client secret, test user password. |

Deviation from the task file: the task said "add database `keycloak` to `init-schemas.sql`". I used a separate file `init-keycloak.sql` (runs as `20-...` after `10-...`). Reason: init scripts run only on an empty volume, and the T06 volume already existed. A separate file can be run once by hand on an existing volume without running the T06 statements a second time.

# The real problem

Every request in this system needs an identity: who is the user, and which roles does the user have. Building login, password storage, password reset, email verification, Google login, and MFA by hand is a large security risk (OWASP A07 "Authentication Failures").

Keycloak gives all of these, but only if it is configured correctly. The three classic mistakes are: a public client that gives tokens to the browser, tokens without an audience (any service accepts any token), and an issuer that changes between the browser and the backend (token validation fails).

# Why this solution

- **Confidential client + PKCE:** the BFF holds the client secret; the browser never gets a token (ADR-007). PKCE (S256) also protects the code exchange. Implicit flow and password grant ("direct access grants") are off.
- **Exact redirect URI:** only `http://localhost:8080/login/oauth2/code/keycloak`. Verified: a foreign redirect URI gets HTTP 400, so an attacker cannot send codes to their own site.
- **Audience mapper `marketplace-api`:** the backend accepts only tokens meant for it (`aud` claim). Verified in a real token: `aud = marketplace-api, account`.
- **Default role `student`:** every new user can buy courses without an extra step (SPEC-identity).
- **Fixed hostname `KC_HOSTNAME=http://localhost:8180` + dynamic back channel:** the browser and the backend see the same issuer `http://localhost:8180/realms/marketplace`, even when the backend calls Keycloak at `http://keycloak:8080` inside the container network.
- **Secrets as placeholders:** Keycloak replaces `${MARKETPLACE_BFF_CLIENT_SECRET}` with the environment variable during import. The realm file in Git has no secret.
- **Own database and role:** a bug in the marketplace backend cannot read or change Keycloak data. Verified: `permission denied for database "keycloak"`.
- **Raw HTTP health check with bash:** the image has no `curl`; bash can open TCP connections through `/dev/tcp`.

# Production shape

- `start --optimized` with a pre-built image (`kc.sh build`), HTTPS, a real hostname (`https://auth.example.com`), at least two nodes.
- Realm configuration as code, applied by a tool (for example keycloak-config-cli or Terraform), not by `--import-realm`: import only creates a missing realm and never updates an existing one.
- The client secret and admin password come from a secret manager; the bootstrap admin is replaced by named admin accounts with MFA.
- No test users. Brute-force protection, password policy, OTP for privileged roles, SMTP, and Google login come from the `identity` plan.
- Keycloak's database is backed up like any production database; sessions survive restarts.

# Other possible approaches

1. A public client with PKCE, tokens in the browser (single-page app pattern).
2. A hosted identity provider (Auth0, Amazon Cognito, Microsoft Entra ID).
3. Spring Authorization Server, built by us.
4. Keycloak with its embedded H2 database (dev mode default).

# Why I did not choose those alternatives

1. Tokens in JavaScript can be stolen by any XSS bug; the spec chose the BFF pattern (ADR-007).
2. Valid, but it costs money per user and moves user data to a third party; plan open question 1 keeps this decision open.
3. Much more work for features Keycloak already has (registration, reset, MFA, social login, admin UI).
4. H2 data is lost easily and behaves differently from production; PostgreSQL is already running.

# Key concepts to learn

- **Realm:** an isolated set of users, roles, and clients in Keycloak.
- **Confidential vs public client:** a confidential client can keep a secret (a server); a public client cannot (a browser or mobile app).
- **Authorization Code flow + PKCE:** the browser gets a one-time code; the server exchanges it, with the secret and the PKCE verifier, for tokens.
- **Claims `iss`, `aud`, `azp`:** who issued the token, for which API it is meant, which client asked for it.
- **Default role:** a composite role that Keycloak gives to every new user.
- **Front channel vs back channel:** URLs the browser uses vs URLs servers use; the issuer must be the same in both.
- **`--import-realm` strategy `IGNORE_EXISTING`:** an existing realm is never overwritten.

# Common mistakes

- Changing `marketplace-realm.json` and expecting Keycloak to update the realm. Import ignores an existing realm. Delete the realm in the admin console, or recreate the Keycloak database, then restart.
- Issuer mismatch: the browser gets tokens from `localhost:8180`, the backend validates against `keycloak:8080` → "invalid issuer". Fix with `KC_HOSTNAME` (done here).
- Wildcard redirect URIs (`http://localhost:8080/*` or `*`): an open redirect for authorization codes.
- Enabling "Direct access grants" for easy testing: it allows password login without the Keycloak page, so MFA and brute-force rules are bypassed.
- Environment variables named `KC_...` for your own placeholders: Keycloak reads `KC_` variables as its own options. Use another prefix (`MARKETPLACE_...`).
- Windows PowerShell 5.1 `Invoke-WebRequest -MaximumRedirection 0` hides the `Location` header of a 302; use `curl.exe -D -` to test login flows.

# Small example

Start and check:

```powershell
podman compose -f infra/compose.yaml up -d keycloak
curl.exe -s http://localhost:8180/realms/marketplace/.well-known/openid-configuration
# "issuer":"http://localhost:8180/realms/marketplace", ...
```

Access-token claims after a real login as `student@local.test` (verified):

```text
iss          : http://localhost:8180/realms/marketplace
aud          : marketplace-api, account
azp          : bff-gateway
realm roles  : default-roles-marketplace, student, offline_access, uma_authorization
expires in   : 300 s
```

Run the Keycloak database script once on an existing volume:

```powershell
podman exec marketplace_postgres_1 sh -c 'psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d marketplace -f /docker-entrypoint-initdb.d/20-init-keycloak.sql'
```

# How to think about this next time

- For each OAuth client ask: Can it keep a secret? Which flows does it really need? Which exact redirect URIs?
- For each token ask: Who must accept it (`aud`)? Which issuer will every validator see?
- Test the login with a real token and read the claims; then test what must fail (no PKCE, wrong secret, foreign redirect).
- For init scripts on persistent volumes: one file per change, safe to run by hand on old volumes, tested on a fresh volume.
