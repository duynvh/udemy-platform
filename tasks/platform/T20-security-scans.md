# T20: Security scans

Phase 5 · Size M · Dependencies: T19

**Description:** Add stage C5: static analysis (Semgrep), secret scan (Gitleaks), and dependency scan (Trivy filesystem scan for Maven and npm). A high or critical finding fails the build.

**Acceptance criteria:**
- [ ] Job `semgrep` with rule sets `p/java`, `p/typescript`, `p/owasp-top-ten`, `p/secrets`; SARIF uploaded to the pull request.
- [ ] Job `gitleaks` scans the full history; `.gitleaks.toml` allows only the dummy values in `infra/.env.example`.
- [ ] Job `dependency-scan`: `trivy fs --scanners vuln --severity HIGH,CRITICAL --exit-code 1 .`
- [ ] `.semgrepignore` excludes generated code (`frontend/src/shared/api/generated`).

**Verification:**
- [ ] Local (repository root): `podman run --rm -v "${PWD}:/repo" docker.io/zricethezav/gitleaks detect -s /repo --no-git` passes
- [ ] Planted fake secret (a test AWS key in a temp file) makes Gitleaks fail (spec criterion 6); remove it after the check

**Files:**
- `.github/workflows/ci.yml`
- `.gitleaks.toml`
- `.semgrepignore`
