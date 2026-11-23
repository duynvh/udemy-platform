# T18: E2E setup

Phase 4 · Size S · Dependencies: T17

**Description:** Set up Playwright with an accessibility check. One smoke test opens the home page through the Vite dev server and checks the shell and the API status.

**Acceptance criteria:**
- [ ] Playwright config: Chromium and Firefox, base URL `http://localhost:5173`, `webServer` starts Vite; trace on first retry.
- [ ] Smoke test: home page shows header and "API: ok"; `@axe-core/playwright` reports no `serious` or `critical` violations.

**Verification:**
- [ ] With `podman compose`, backend, and BFF running: `npm --prefix frontend run e2e` passes

**Files:**
- `frontend/playwright.config.ts`
- `frontend/e2e/home.spec.ts`
- `frontend/package.json` (dev dependencies)
