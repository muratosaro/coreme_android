#!/usr/bin/env bash
echo "Testing Task 3: NATS JetStream — config, container, stream"
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
DC="$ROOT/docker-compose.yml"

# docker-compose config checks
check "nats service defined in docker-compose" "grep -q 'nats:' '$DC'"
check "nats --js flag (JetStream enabled)" "grep -q -- '--js' '$DC'"
check "nats --http_port 8222" "grep -q '8222' '$DC'"
check "nats_data volume" "grep -q 'nats_data' '$DC'"

# realtime NATS client code
NATS_JS="$ROOT/realtime/src/config/nats.js"
check "realtime/src/config/nats.js exists" "[ -f '$NATS_JS' ]"
check "MESSAGES stream defined in nats.js" "grep -q 'MESSAGES' '$NATS_JS'"
check "workqueue retention in nats.js" "grep -q 'workqueue' '$NATS_JS'"
check "connectNats function" "grep -q 'connectNats' '$NATS_JS'"
check "publish function" "grep -q 'publish' '$NATS_JS'"
check "startConsumer function" "grep -q 'startConsumer' '$NATS_JS'"
check "closeNats function" "grep -q 'closeNats' '$NATS_JS'"

# realtime index.js integrates NATS
REALTIME_IDX="$ROOT/realtime/src/index.js"
check "realtime index.js imports connectNats" "grep -q 'connectNats' '$REALTIME_IDX'"
check "realtime index.js calls closeNats on shutdown" "grep -q 'closeNats' '$REALTIME_IDX'"

# chat handler uses NATS publish (not direct io.emit)
CHAT_HANDLER="$ROOT/realtime/src/socket/handlers/chat.handler.js"
check "chat.handler.js exists" "[ -f '$CHAT_HANDLER' ]"
check "chat.handler.js uses publish (NATS)" "grep -q 'publish' '$CHAT_HANDLER'"

# nats package.json dependency
PKG="$ROOT/realtime/package.json"
check "nats npm package listed in package.json" "grep -q '\"nats\"' '$PKG'"

# container state
NATS_STATUS=$(docker inspect coreme_nats --format='{{.State.Status}}' 2>/dev/null || echo "missing")
NATS_HEALTH=$(docker inspect coreme_nats --format='{{.State.Health.Status}}' 2>/dev/null || echo "n/a")
echo "  ℹ️  nats container status: $NATS_STATUS / health: $NATS_HEALTH"

if [ "$NATS_STATUS" = "running" ]; then
  echo "  ✅ nats container is running"; ((PASS++))
  # KNOWN BUG: nats:latest is distroless → CMD-SHELL healthcheck fails (no /bin/sh)
  if [ "$NATS_HEALTH" = "healthy" ]; then
    echo "  ✅ nats healthcheck healthy"; ((PASS++))
  else
    echo "  ❌ nats healthcheck is '$NATS_HEALTH' (KNOWN BUG: nats:latest distroless — CMD-SHELL has no /bin/sh)"; ((FAIL++))
  fi
  # Try HTTP monitoring endpoint directly
  if docker exec coreme_nats wget -qO- http://localhost:8222/healthz 2>/dev/null | grep -q 'ok'; then
    echo "  ✅ NATS /healthz endpoint responds ok"; ((PASS++))
  else
    echo "  ❌ NATS /healthz not reachable inside container"; ((FAIL++))
  fi
else
  echo "  ❌ nats container not running (status: $NATS_STATUS)"; ((FAIL++))
fi

echo "  Task 3: $PASS passed, $FAIL failed"
[ "$FAIL" -eq 0 ]
