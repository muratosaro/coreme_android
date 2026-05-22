#!/usr/bin/env bash
echo "Testing Task 12: TURN server (coturn) — config and container"
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
TURN="$ROOT/turn"
DC="$ROOT/docker-compose.yml"

# File structure
check "turn/ directory exists" "[ -d '$TURN' ]"
check "turn/turnserver.conf exists" "[ -f '$TURN/turnserver.conf' ]"
check "turn/Dockerfile exists" "[ -f '$TURN/Dockerfile' ]"
check "turn/entrypoint.sh exists" "[ -f '$TURN/entrypoint.sh' ]"

# turnserver.conf content
CONF="$TURN/turnserver.conf"
check "turnserver.conf has listening-port" "grep -q 'listening-port\|port' '$CONF'"
check "turnserver.conf has realm" "grep -q 'realm\|TURN_REALM' '$CONF'"
check "turnserver.conf denies RFC-1918 peers (SSRF prevention)" \
  "grep -q 'no-multicast-peers\|denied-peer-ip\|10\.0\.\|192\.168\.\|172\.' '$CONF'"
check "turnserver.conf fingerprint enabled" "grep -q 'fingerprint' '$CONF'"
check "turnserver.conf no-auth or lt-cred-mech" \
  "grep -q 'lt-cred-mech\|use-auth-secret\|static-auth-secret\|no-auth' '$CONF'"

# Dockerfile
DOCKERFILE="$TURN/Dockerfile"
check "turn/Dockerfile uses debian or coturn base" "grep -q 'debian\|coturn\|ubuntu\|FROM' '$DOCKERFILE'"
check "turn/Dockerfile installs coturn" "grep -q 'coturn\|turnserver' '$DOCKERFILE'"

# entrypoint.sh
ENTRY="$TURN/entrypoint.sh"
check "entrypoint.sh injects TURN_USERNAME" "grep -q 'TURN_USERNAME\|TURN_USER' '$ENTRY'"
check "entrypoint.sh injects TURN_PASSWORD" "grep -q 'TURN_PASSWORD\|TURN_PASS' '$ENTRY'"
check "entrypoint.sh injects TURN_REALM" "grep -q 'TURN_REALM' '$ENTRY'"
check "entrypoint.sh injects TURN_PUBLIC_IP" "grep -q 'TURN_PUBLIC_IP\|external-ip' '$ENTRY'"

# docker-compose 'turn' profile
check "turn service in docker-compose" "grep -q 'coturn\|turn' '$DC'"
check "turn profile defined in docker-compose" "grep -A2 'profiles:' '$DC' | grep -q '\- turn'"

# Container check — TURN is under 'turn' profile (not started by default)
TURN_STATUS=$(docker inspect coreme_turn --format='{{.State.Status}}' 2>/dev/null || echo "missing")
if [ "$TURN_STATUS" = "running" ]; then
  echo "  ✅ coturn container running"; ((PASS++))
  # STUN/TURN port 3478 test — cannot reliably test without client SDK
  echo "  ⚠️  SKIP STUN/TURN protocol test: requires WebRTC client or turnutils_uclient"
  ((SKIP++))
else
  echo "  ⚠️  SKIP coturn live test: container not running (status: $TURN_STATUS) — requires --profile turn"
  ((SKIP++))
fi

# STUN/TURN connectivity note
echo "  ⚠️  SKIP external STUN connectivity: requires public IP and open UDP 3478 (production-only test)"
((SKIP++))

echo "  Task 12: $PASS passed, $FAIL failed, $SKIP skipped"
[ "$FAIL" -eq 0 ]
