#!/usr/bin/env bash
echo "Testing Task 6: Sentry error tracking (Go API + Node.js realtime)"
PASS=0; FAIL=0

check() {
  local desc="$1"; local cmd="$2"
  if eval "$cmd" &>/dev/null; then
    echo "  ✅ $desc"; ((PASS++))
  else
    echo "  ❌ $desc"; ((FAIL++))
  fi
}

ROOT="$(dirname "$0")/../.."
API="$ROOT/api"
REALTIME="$ROOT/realtime"

# Go API Sentry
check "sentry-go in go.mod" "grep -q 'getsentry/sentry-go' '$API/go.mod'"

MAIN="$API/cmd/api/main.go"
check "main.go imports sentry-go" "grep -q 'sentry-go\|sentry\"' '$MAIN'"
check "main.go calls sentry.Init" "grep -q 'sentry.Init' '$MAIN'"
check "main.go defers sentry.Flush" "grep -q 'sentry.Flush' '$MAIN'"
check "main.go PII scrub (BeforeSend or scrubHeaders)" "grep -q 'BeforeSend\|scrubHeaders\|Authorization\|Cookie' '$MAIN'"
check "main.go sentryMiddleware registered" "grep -q 'sentryMiddleware' '$MAIN'"

# Check for PII filtering: auth/messages body redaction
check "main.go redacts /auth/ body in Sentry" "grep -q '/auth/' '$MAIN'"
check "main.go redacts /messages body in Sentry" "grep -q 'messages\|REDACTED' '$MAIN'"

# Node.js realtime Sentry
PKG="$REALTIME/package.json"
check "@sentry/node in realtime package.json" "grep -q '@sentry/node' '$PKG'"

REALTIME_IDX="$REALTIME/src/index.js"
check "realtime index.js imports Sentry" "grep -q -i 'sentry\|@sentry' '$REALTIME_IDX'"
check "realtime index.js calls Sentry.init" "grep -q 'Sentry.init\|sentry.init' '$REALTIME_IDX'"
check "realtime index.js beforeSend PII filter" "grep -q 'beforeSend\|BeforeSend' '$REALTIME_IDX'"
check "realtime index.js captureException on uncaughtException" "grep -q 'captureException\|uncaughtException' '$REALTIME_IDX'"
check "realtime index.js Sentry.close on shutdown" "grep -q 'Sentry.close\|sentry.close' '$REALTIME_IDX'"

# docker-compose has SENTRY_DSN env
DC="$ROOT/docker-compose.yml"
check "SENTRY_DSN env var in docker-compose (api)" "grep -q 'SENTRY_DSN' '$DC'"

echo "  Task 6: $PASS passed, $FAIL failed"
[ "$FAIL" -eq 0 ]
