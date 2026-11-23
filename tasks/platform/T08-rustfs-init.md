# T08: RustFS buckets init

Phase 2 · Size S · Dependencies: T06

**Description:** Add a one-shot compose service `rustfs-init` with image `docker.io/amazon/aws-cli:2.42.1`. It runs `aws --endpoint-url http://rustfs:9000 s3api` commands (`create-bucket`, `put-bucket-policy`, `put-bucket-cors`, `put-bucket-lifecycle-configuration`). It creates the buckets and sets policies, CORS, and lifecycle rules from architecture §5.4 and SPEC-media. It runs `head-bucket` before `create-bucket`, so a second run changes nothing.

**Acceptance criteria:**
- [ ] Buckets `uploads-raw`, `video-hls`, `attachments` are private; `public-assets` allows anonymous GET only.
- [ ] Lifecycle: `uploads-raw` aborts incomplete multipart uploads after 1 day.
- [ ] CORS on `uploads-raw` and `public-assets` allows `PUT` from `http://localhost:5173` and exposes `ETag`.
- [ ] The init service is idempotent: a second run exits 0 without changes.

**Verification:**
- [ ] `podman compose -f infra/compose.yaml run --rm rustfs-init` exits 0 twice in a row (do not use attached `up`: podman-compose waits for every container in the project, also RustFS)
- [ ] `podman compose -f infra/compose.yaml run --rm --entrypoint aws rustfs-init s3 ls` lists the 4 buckets
- [ ] Anonymous GET works only in `public-assets`; anonymous PUT and LIST return 403; CORS preflight from `http://localhost:5173` is allowed, from another origin refused

**Files:**
- `infra/compose.yaml`
- `infra/rustfs/init-buckets.sh`
- `infra/rustfs/cors.json`
- `infra/rustfs/lifecycle.json`
- `infra/rustfs/public-assets-policy.json`, `infra/rustfs/aws-config` (path-style addressing)
