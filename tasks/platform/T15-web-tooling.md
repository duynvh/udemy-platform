# T15: Web tooling

Phase 4 · Size M · Dependencies: none (can start after Checkpoint 1)

**Description:** Create the frontend project in `frontend/` with Vite, React 19, TypeScript strict, ESLint, Prettier, and Vitest. The Vite dev server proxies backend paths to the BFF.

**Acceptance criteria:**
- [ ] `tsconfig.json` has `strict: true`, `noUncheckedIndexedAccess: true`.
- [ ] ESLint flat config: `typescript-eslint` strict-type-checked, `react-hooks`, `jsx-a11y`, `no-explicit-any` as error; Prettier for formatting.
- [ ] Vite proxy: `/api`, `/bff`, `/oauth2`, `/login`, `/logout` → `http://localhost:8080`.
- [ ] Vitest with jsdom, Testing Library, and MSW set up in one setup file.

**Verification:**
- [ ] `npm --prefix frontend ci`, then `npm --prefix frontend run lint` and `npm --prefix frontend test` pass (one sample test)
- [ ] `npm --prefix frontend run build` creates `frontend/dist`

**Files:**
- `frontend/package.json` (scripts: dev, build, test, lint, api:generate, e2e)
- `frontend/vite.config.ts`
- `frontend/tsconfig.json`
- `frontend/eslint.config.js`
- `frontend/src/test/setup.ts`
