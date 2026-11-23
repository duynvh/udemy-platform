#!/bin/bash
# Creates the object storage buckets and their rules (docs/architecture.md §5.4, spec/SPEC-media.md).
# Safe to run again: existing buckets are kept; policy, CORS, and lifecycle are set to the same values.
set -euo pipefail

CONFIG_DIR=/init
PRIVATE_BUCKETS=(uploads-raw video-hls attachments)
PUBLIC_BUCKET=public-assets
# Browser uploads use presigned PUT URLs from the web app origin.
CORS_BUCKETS=(uploads-raw public-assets)

ensure_bucket() {
  local bucket="$1"
  if aws s3api head-bucket --bucket "$bucket" 2>/dev/null; then
    echo "bucket ${bucket}: exists"
    return
  fi
  aws s3api create-bucket --bucket "$bucket" > /dev/null
  echo "bucket ${bucket}: created"
}

for bucket in "${PRIVATE_BUCKETS[@]}" "$PUBLIC_BUCKET"; do
  ensure_bucket "$bucket"
done

# Only public-assets is readable without credentials; the other buckets have no policy (private).
aws s3api put-bucket-policy --bucket "$PUBLIC_BUCKET" --policy "file://${CONFIG_DIR}/public-assets-policy.json"
echo "policy ${PUBLIC_BUCKET}: anonymous GetObject"

for bucket in "${CORS_BUCKETS[@]}"; do
  aws s3api put-bucket-cors --bucket "$bucket" --cors-configuration "file://${CONFIG_DIR}/cors.json"
  echo "cors ${bucket}: PUT from http://localhost:5173"
done

aws s3api put-bucket-lifecycle-configuration --bucket uploads-raw \
  --lifecycle-configuration "file://${CONFIG_DIR}/lifecycle.json"
echo "lifecycle uploads-raw: abort incomplete multipart uploads after 1 day"

echo "rustfs-init: done"
