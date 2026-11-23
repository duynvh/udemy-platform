# Task

T08 — RustFS buckets init. Task file: [tasks/platform/T08-rustfs-init.md](../../../tasks/platform/T08-rustfs-init.md). Spec: [architecture.md §5.4](../../architecture.md) (buckets), [SPEC-media.md](../../../spec/SPEC-media.md) (Storage, upload flow).

# What I changed

| File | What it does |
|---|---|
| `infra/compose.yaml` | One-shot service `rustfs-init` (`docker.io/amazon/aws-cli:2.42.1`, user `nobody`). Waits for a healthy RustFS, runs the init script, exits. |
| `infra/rustfs/init-buckets.sh` | Creates the 4 buckets if missing (`head-bucket` first), sets the public read policy, CORS, and lifecycle. Bash with `set -euo pipefail`, one function per step. |
| `infra/rustfs/public-assets-policy.json` | Anonymous `s3:GetObject` on `public-assets/*` only. No list, no write. |
| `infra/rustfs/cors.json` | `PUT` from `http://localhost:5173`, any request header, exposes `ETag`, preflight cache 1 h. |
| `infra/rustfs/lifecycle.json` | Abort incomplete multipart uploads after 1 day. |
| `infra/rustfs/aws-config` | Region and path-style addressing for the AWS CLI. |
| `tasks/platform/T08-rustfs-init.md` | Verification commands fixed for podman-compose (see Common mistakes). |

# The real problem

The application needs storage with exact rules before any upload works:

- Course videos and attachments are paid content. If a bucket is public by mistake, anyone with the URL downloads courses (piracy risk in the plan, §10).
- Course images and avatars must load in any browser without a signed URL.
- The browser uploads 4 GB videos directly to storage with presigned URLs (SPEC-media). Without CORS, the browser blocks the upload. Without `ETag` exposed, the browser cannot finish a multipart upload, because the complete call needs the ETag of each part.
- A user who closes the browser during an upload leaves parts in storage. Without a lifecycle rule, these parts cost money forever.

If this setup is done by hand in a console, each developer and each environment has different rules.

# Why this solution

- **Configuration as files + an idempotent script:** the same rules on every machine; a second run changes nothing (verified: all buckets "exists", exit 0).
- **AWS CLI, not a vendor tool:** RustFS speaks the S3 API, so the standard `aws s3api` commands work. The same script works against AWS S3, SeaweedFS, or Garage. This is the same idea as the `FileStoragePort` in the backend.
- **Private by default:** only `public-assets` has a policy, and it allows only `GetObject`. Verified: anonymous GET in `video-hls` → 403, anonymous LIST and PUT in `public-assets` → 403.
- **CORS only for the web app origin and only `PUT`:** verified: preflight from `http://localhost:5173` → 200 with `Access-Control-Allow-Origin`; from `https://evil.example` → 403.
- **Path-style addressing:** inside the container network the host is `rustfs`; virtual-hosted URLs (`uploads-raw.rustfs`) would need wildcard DNS.
- **Non-root job:** the job runs as `nobody` with `HOME=/tmp`; it needs no root rights.

# Production shape

- The same rules are managed by infrastructure as code (Terraform or OpenTofu `aws_s3_bucket_*` resources) instead of a shell script.
- `public-assets` and `video-hls` are served through a CDN; the CDN reads `video-hls` with its own identity, and the bucket stays private (architecture §6.3: signed CDN URLs).
- CORS allows the production web origin (for example `https://www.example.com`), never `*`.
- Encryption at rest with a KMS key, versioning on buckets with user content, access logs for audits.
- The application uses its own key with rights only on its buckets, not the root credentials that the init job uses here.

# Other possible approaches

1. Create buckets by hand in the RustFS console.
2. Let the backend create buckets on startup.
3. The MinIO client `mc`.
4. RustFS-specific environment variables or config to pre-create buckets.

# Why I did not choose those alternatives

1. Not repeatable; each developer gets different rules; nothing to review in Git.
2. The application would need admin rights on storage, and policy/CORS are infrastructure, not application logic. Least privilege says the app gets object rights only.
3. MinIO stopped publishing images; `mc` also ties the setup to one vendor.
4. Vendor-specific; the S3 API keeps the storage replaceable.

# Key concepts to learn

- **Bucket policy:** a JSON document that grants actions (for example `s3:GetObject`) on resources (`arn:aws:s3:::bucket/*`) to principals (`*` = anyone).
- **CORS preflight:** before a cross-origin `PUT`, the browser sends `OPTIONS` with `Origin` and `Access-Control-Request-Method`; the server answers which origins, methods, and headers are allowed.
- **`ExposeHeaders`:** response headers that JavaScript may read on the real response (here `ETag`); they do not appear in the preflight.
- **Multipart upload:** a large file is uploaded in parts; each part returns an ETag; a final call joins the parts.
- **Lifecycle rule:** the storage deletes or aborts data automatically after a time.
- **Path-style vs virtual-hosted URLs:** `http://host/bucket/key` vs `http://bucket.host/key`.
- **Idempotent job:** running it once or many times gives the same result.

# Common mistakes

- `podman compose up rustfs-init` (attached) with podman-compose: it attaches to all services of the project and waits until all stop; RustFS never stops, so the command hangs although the job finished with exit 0. Use `podman compose run --rm rustfs-init`, or `up -d` and read the logs.
- `podman compose run --rm rustfs-init aws ...`: the service entrypoint is the init script, so the arguments go to the script. Use `--entrypoint aws`.
- A public-read policy that also allows `s3:ListBucket`: anyone can list all object names.
- CORS `AllowedOrigins: ["*"]` "for development", then copied to production.
- Forgetting `ExposeHeaders: ETag`: single-part uploads work, multipart uploads fail in the browser only.
- Testing only that the buckets exist: test the negative cases too (anonymous access to private data must fail).

# Small example

```powershell
podman compose -f infra/compose.yaml run --rm rustfs-init
# bucket uploads-raw: exists ... rustfs-init: done   (exit 0)

curl.exe -s -o NUL -w "%{http_code}" http://localhost:9000/public-assets/logo.png   # 200
curl.exe -s -o NUL -w "%{http_code}" http://localhost:9000/video-hls/lecture/1.ts   # 403

curl.exe -s -D - -o NUL -X OPTIONS -H "Origin: http://localhost:5173" `
  -H "Access-Control-Request-Method: PUT" http://localhost:9000/uploads-raw/x
# HTTP/1.1 200 OK
# access-control-allow-origin: http://localhost:5173
```

# How to think about this next time

- For each bucket ask: Who may read? Who may write? From which origin? How long does data stay?
- Write the rules as files and apply them with a script that is safe to run again.
- Verify with real requests, and always include the requests that must fail.
- Depend on the protocol (S3 API, AWS CLI) so the vendor can change without new scripts.
