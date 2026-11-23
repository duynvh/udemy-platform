# T17: API client generation

Phase 4 · Size M · Dependencies: T13, T16

**Description:** Generate the OpenAPI file from the backend and a typed client with TanStack Query hooks in the frontend (Orval). `frontend/orval.config.ts` reads `../backend/marketplace-api/target/openapi.json`. Use one custom fetcher that later adds the CSRF header.

**Acceptance criteria:**
- [ ] Backend serves `/v3/api-docs` through springdoc on the management port only (not public).
- [ ] `./mvnw -pl marketplace-api verify` (in `backend/`) writes `backend/marketplace-api/target/openapi.json` (springdoc Maven plugin).
- [ ] `npm --prefix frontend run api:generate` creates `frontend/src/shared/api/generated/` from that file. Each generated file starts with a "generated, do not edit" header.
- [ ] `apiFetch` sends `credentials: "include"` and, for write methods, `X-XSRF-TOKEN` from the `XSRF-TOKEN` cookie.
- [ ] Home page calls the generated `useGetPing()` hook and shows "API: ok".

**Verification:**
- [ ] `npm --prefix frontend test` passes: `apiFetch_postWithCookie_addsXsrfHeader`, `HomePage_pingOk_showsApiOk` (MSW)
- [ ] Manual: with backend and BFF running, the home page shows "API: ok"

**Files:**
- `backend/marketplace-api/pom.xml` (springdoc)
- `frontend/orval.config.ts`
- `frontend/src/shared/api/apiFetch.ts`
- `frontend/src/shared/api/apiFetch.test.ts`
- `frontend/src/app/router.tsx` (home page uses the hook)
