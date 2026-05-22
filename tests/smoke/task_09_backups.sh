#!/usr/bin/env bash
echo "Testing Task 9: Backup scripts — pg_dump + Cloudflare R2"
PASS=0; FAIL=0
SKIP=0

check() {
  local desc="$1"; local cmd="$2"
  if eval "$cmd" &>/dev/null; then
    echo "  ✅ $desc"; ((PASS++))
  else
    echo "  ❌ $desc"; ((FAIL++))
  fi
}

ROOT="$(dirname "$0")/../.."
SCRIPTS="$ROOT/scripts"

# File existence
check "scripts/backup.sh exists" "[ -f '$SCRIPTS/backup.sh' ]"
check "scripts/backup-cron exists" "[ -f '$SCRIPTS/backup-cron' ]"

# backup.sh content
BACKUP="$SCRIPTS/backup.sh"
check "backup.sh uses pg_dump" "grep -q 'pg_dump' '$BACKUP'"
check "backup.sh uploads to R2 (aws s3 cp or rclone)" "grep -q 's3://\|rclone\|cloudflare\|r2\.' '$BACKUP' -i"
check "backup.sh has 30-day retention cleanup" "grep -q '30\|retention\|older\|mtime\|days' '$BACKUP'"
check "backup.sh uses PGPASSWORD or .pgpass" "grep -q 'PGPASSWORD\|pgpass\|POSTGRES' '$BACKUP'"
check "backup.sh shebang #!/usr/bin/env bash" "head -1 '$BACKUP' | grep -q '#!/'"

# cron schedule
CRON="$SCRIPTS/backup-cron"
check "backup-cron has cron schedule" "grep -q 'backup.sh\|pg_dump' '$CRON'"

# generate-secrets.sh (may exist as part of scripts)
if [ -f "$SCRIPTS/generate-secrets.sh" ]; then
  check "generate-secrets.sh uses openssl" "grep -q 'openssl' '$SCRIPTS/generate-secrets.sh'"
else
  echo "  ⚠️  SKIP generate-secrets.sh: file not found"
  ((SKIP++))
fi

# Live pg_dump test (requires postgres running)
PG_STATUS=$(docker inspect coreme_postgres --format='{{.State.Status}}' 2>/dev/null || echo "missing")
if [ "$PG_STATUS" = "running" ]; then
  DUMP_SIZE=$(docker exec coreme_postgres pg_dump -U "${DB_USER:-postgres}" -d "${DB_NAME:-coreme}" --schema-only 2>/dev/null | wc -c || echo 0)
  if [ "$DUMP_SIZE" -gt 100 ]; then
    echo "  ✅ pg_dump --schema-only succeeds (${DUMP_SIZE} bytes)"; ((PASS++))
  else
    echo "  ❌ pg_dump produced no output"; ((FAIL++))
  fi
else
  echo "  ❌ postgres not running — cannot test pg_dump"; ((FAIL++))
fi

# R2 / aws CLI live test — SKIP (requires real credentials)
echo "  ⚠️  SKIP R2 upload test: requires real Cloudflare R2 credentials (AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY)"
((SKIP++))

echo "  Task 9: $PASS passed, $FAIL failed, $SKIP skipped"
[ "$FAIL" -eq 0 ]
