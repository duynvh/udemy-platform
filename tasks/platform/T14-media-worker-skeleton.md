# T14: Media worker skeleton

Phase 4 · Size S · Dependencies: T11

**Description:** Create the `media-worker` Maven module in `backend/media-worker`: a Spring Boot app without a web server for business traffic, with Actuator, observability, and a startup check that FFmpeg and ffprobe are available. Transcoding is in the `media` plan.

**Acceptance criteria:**
- [ ] App starts with `spring.main.web-application-type=none` for business; Actuator on port 8092 (`health`, `prometheus`).
- [ ] Health indicator `ffmpeg` is `UP` when `ffmpeg -version` exits 0, `DOWN` with the error otherwise.
- [ ] OTLP and JSON logs like the backend.

**Verification** (Maven in `backend/`):
- [ ] `./mvnw -pl media-worker test` passes: `health_ffmpegMissing_reportsDown` with a `FakeProcessRunner`
- [ ] With FFmpeg installed: `curl http://localhost:8092/actuator/health` shows `ffmpeg: UP`

**Files:**
- `backend/media-worker/pom.xml` (and module entry `media-worker` in `backend/pom.xml`)
- `backend/media-worker/src/main/java/com/marketplace/mediaworker/MediaWorkerApplication.java`
- `backend/media-worker/src/main/java/com/marketplace/mediaworker/FfmpegHealthIndicator.java`
- `backend/media-worker/src/main/resources/application.yml`
- `backend/media-worker/src/test/java/com/marketplace/mediaworker/FfmpegHealthIndicatorTest.java`
