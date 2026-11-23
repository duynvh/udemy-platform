# T08: MinIO buckets init

Phase 2 · Size S · Dependencies: T06

**Description:** Add a one-shot `minio-init` service that uses the MinIO client (`mc`) to create buckets, set policies, CORS, and lifecycle rules from architecture §5.4 and SPEC-media.

**Acceptance criteria:**
- [ ] Buckets `uploads-raw`, `video-hls`, `attachments` are private; `public-assets` allows anonymous GET only.
- [ ] Lifecycle: `uploads-raw` aborts incomplete multipart uploads after 1 day.
- [ ] CORS on `uploads-raw` and `public-assets` allows `PUT` from `http://localhost:5173` and exposes `ETag`.
- [ ] The init service is idempotent: a second run exits 0 without changes.

**Verification:**
- [ ] `podman compose -f infra/compose.yaml up minio-init` exits 0 twice in a row
- [ ] `podman compose -f infra/compose.yaml run --rm minio-init mc ls local` lists the 4 buckets

**Files:**
- `infra/compose.yaml`
- `infra/minio/init-buckets.sh`
- `infra/minio/cors.json`
