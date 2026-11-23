# T19: CI build and test stages

Phase 5 · Size S · Dependencies: T18

**Description:** Create the GitHub Actions workflow with stages C1–C4 from the spec's CI diagram: checkout, format and lint, unit tests, integration tests.

**Acceptance criteria:**
- [ ] Triggers: push to `main`, every pull request.
- [ ] Jobs: `java-lint` (`spotless:check`), `web-lint`, `java-unit` (`./mvnw test`), `web-unit`, `java-integration` (`./mvnw verify`; Testcontainers uses the Docker engine of the GitHub runner).
- [ ] `ci.yml` stays at the repository root. Maven steps use `working-directory: backend`; npm steps use `working-directory: frontend`.
- [ ] Maven and npm caches; Java 25 (Temurin); Node 24.
- [ ] Workflow permissions `contents: read` by default; all third-party actions pinned to a commit SHA.

**Verification:**
- [ ] `actionlint .github/workflows/ci.yml` passes locally, or with Podman: `podman run --rm -v "${PWD}:/repo" -w /repo docker.io/rhysd/actionlint:latest`
- [ ] After `git init` and push: all jobs green on a pull request

**Files:**
- `.github/workflows/ci.yml`
