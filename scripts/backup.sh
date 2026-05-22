#!/usr/bin/env bash
# PostgreSQL backup → Cloudflare R2
#
# On the VPS the script runs via host cron and connects to postgres
# through the exposed Docker port (default: localhost:5432).
#
# Required env vars (add to /etc/environment or a dedicated cron env file):
#   DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD
#   R2_BUCKET          — Cloudflare R2 bucket name
#   R2_ENDPOINT        — https://<account-id>.r2.cloudflarestorage.com
#   R2_ACCESS_KEY_ID
#   R2_SECRET_ACCESS_KEY
#
# Optional:
#   RETENTION_DAYS     — default 30
#   BACKUP_PREFIX      — default "backups/postgres"
#
# Host cron install (runs every day at 02:00 UTC):
#   sudo cp server/scripts/backup.sh /opt/coreme/backup.sh
#   sudo chmod +x /opt/coreme/backup.sh
#   (crontab -l; echo "0 2 * * * /opt/coreme/backup.sh >> /var/log/coreme-backup.log 2>&1") | crontab -
#
# Dependencies on host: pg_dump (postgresql-client), awscli
#   apt-get install -y postgresql-client awscli
set -euo pipefail

: "${DB_HOST:=localhost}"
: "${DB_PORT:=5432}"
: "${DB_NAME:?DB_NAME is required}"
: "${DB_USER:?DB_USER is required}"
: "${R2_BUCKET:?R2_BUCKET is required}"
: "${R2_ENDPOINT:?R2_ENDPOINT is required}"
: "${R2_ACCESS_KEY_ID:?R2_ACCESS_KEY_ID is required}"
: "${R2_SECRET_ACCESS_KEY:?R2_SECRET_ACCESS_KEY is required}"

RETENTION_DAYS="${RETENTION_DAYS:-30}"
BACKUP_PREFIX="${BACKUP_PREFIX:-backups/postgres}"
TIMESTAMP="$(date -u '+%Y%m%dT%H%M%SZ')"
FILENAME="${DB_NAME}_${TIMESTAMP}.sql.gz"
TMP_FILE="/tmp/${FILENAME}"

echo "[backup] Starting backup of ${DB_NAME} at ${TIMESTAMP}"

# Dump and compress
PGPASSWORD="${DB_PASSWORD:-}" pg_dump \
  -h "${DB_HOST}" \
  -p "${DB_PORT}" \
  -U "${DB_USER}" \
  -F plain \
  --no-owner \
  --no-acl \
  "${DB_NAME}" | gzip -9 > "${TMP_FILE}"

DUMP_SIZE="$(du -sh "${TMP_FILE}" | cut -f1)"
echo "[backup] Dump created: ${TMP_FILE} (${DUMP_SIZE})"

# Upload to R2 via AWS CLI (S3-compatible endpoint)
AWS_ACCESS_KEY_ID="${R2_ACCESS_KEY_ID}" \
AWS_SECRET_ACCESS_KEY="${R2_SECRET_ACCESS_KEY}" \
aws s3 cp "${TMP_FILE}" \
  "s3://${R2_BUCKET}/${BACKUP_PREFIX}/${FILENAME}" \
  --endpoint-url "${R2_ENDPOINT}" \
  --region auto \
  --no-progress

echo "[backup] Uploaded to s3://${R2_BUCKET}/${BACKUP_PREFIX}/${FILENAME}"
rm -f "${TMP_FILE}"

# Retention policy — delete backups older than RETENTION_DAYS
echo "[backup] Applying retention (>${RETENTION_DAYS} days)"

# GNU date and BSD date (macOS) compatible
if date --version &>/dev/null 2>&1; then
  CUTOFF="$(date -u -d "${RETENTION_DAYS} days ago" '+%Y-%m-%dT%H:%M:%SZ')"
else
  CUTOFF="$(date -u -v"-${RETENTION_DAYS}d" '+%Y-%m-%dT%H:%M:%SZ')"
fi

AWS_ACCESS_KEY_ID="${R2_ACCESS_KEY_ID}" \
AWS_SECRET_ACCESS_KEY="${R2_SECRET_ACCESS_KEY}" \
aws s3api list-objects-v2 \
  --bucket "${R2_BUCKET}" \
  --prefix "${BACKUP_PREFIX}/" \
  --endpoint-url "${R2_ENDPOINT}" \
  --query "Contents[?LastModified<='${CUTOFF}'].Key" \
  --output text | tr '\t' '\n' | while IFS= read -r KEY; do
    [ -z "${KEY}" ] && continue
    AWS_ACCESS_KEY_ID="${R2_ACCESS_KEY_ID}" \
    AWS_SECRET_ACCESS_KEY="${R2_SECRET_ACCESS_KEY}" \
    aws s3 rm "s3://${R2_BUCKET}/${KEY}" \
      --endpoint-url "${R2_ENDPOINT}" \
      --region auto
    echo "[backup] Deleted expired backup: ${KEY}"
done

echo "[backup] Done."
