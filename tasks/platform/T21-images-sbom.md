# T21: Images and SBOM

Phase 5 · Size M · Dependencies: T20

**Description:** Add stages C6–C8: build container images, create an SBOM, scan images, and fail on high findings. Images are tagged with the commit SHA.

**Acceptance criteria:**
- [ ] Jib builds `marketplace-api` and `bff-gateway` images on a pinned distroless Java 25 base, user `nonroot`.
- [ ] `media-worker` uses a pinned base image that contains FFmpeg, non-root user.
- [ ] `frontend/Dockerfile`: multi-stage build, runtime `docker.io/nginxinc/nginx-unprivileged` (pinned). Podman and the CI Docker engine both read this file.
- [ ] CI builds the images with the Docker engine of the GitHub runner. The images are OCI images, so the result is the same as a local Podman build.
- [ ] CycloneDX SBOM for Maven (`cyclonedx-maven-plugin`) and npm; uploaded as CI artifacts.
- [ ] `trivy image --severity HIGH,CRITICAL --exit-code 1` on each image; images pushed only from `main`.

**Verification** (Maven in `backend/`):
- [ ] Local: `./mvnw -pl marketplace-api jib:dockerBuild -Djib.dockerClient.executable=podman` creates `marketplace/marketplace-api:dev`; `podman run --rm --entrypoint id marketplace/marketplace-api:dev` fails or shows a non-root uid
- [ ] `./mvnw cyclonedx:makeAggregateBom` writes `backend/target/bom.json`

**Files:**
- `backend/pom.xml` (Jib and CycloneDX plugin configuration)
- `backend/media-worker/pom.xml` (base image override)
- `frontend/Dockerfile`
- `.github/workflows/ci.yml`
