# T16: Web app shell

Phase 4 · Size M · Dependencies: T15

**Description:** Add the MUI theme, router, TanStack Query provider, and an app shell with a header and an empty home page.

**Acceptance criteria:**
- [ ] MUI theme with light and dark mode from the system setting; `CssBaseline`.
- [ ] Header: product name link to `/`, a search field placeholder, a "Log in" button (no action yet); skip link "Skip to content" as the first focusable element.
- [ ] Router with `/` (home) and a 404 page; QueryClient with retry 1 for queries.

**Verification:**
- [ ] `npm --prefix frontend test` passes: `AppShell_render_showsHeaderAndMain`, `AppShell_tabFirst_focusesSkipLink`
- [ ] `npm --prefix frontend run dev`, open `http://localhost:5173`: shell visible, no console errors (spec criterion 7)

**Files:**
- `frontend/src/main.tsx`
- `frontend/src/app/theme.ts`
- `frontend/src/app/AppShell.tsx`
- `frontend/src/app/router.tsx` (home and not-found pages inline as small components)
- `frontend/src/app/AppShell.test.tsx`
