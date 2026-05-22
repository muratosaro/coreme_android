#!/usr/bin/env bash
echo "Testing Task 4: Circuit breakers + exponential backoff"
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

# go.mod dependencies
check "github.com/sony/gobreaker in go.mod" "grep -q 'sony/gobreaker' '$API/go.mod'"
check "github.com/cenkalti/backoff/v4 in go.mod" "grep -q 'cenkalti/backoff' '$API/go.mod'"

# resilience package
BREAKER="$API/internal/resilience/breaker.go"
check "internal/resilience/breaker.go exists" "[ -f '$BREAKER' ]"
check "breaker.go imports gobreaker" "grep -q 'gobreaker' '$BREAKER'"
check "breaker.go NewDBBreaker function" "grep -q 'NewDBBreaker' '$BREAKER'"
check "breaker.go ConsecutiveFailures threshold" "grep -q 'ConsecutiveFailures' '$BREAKER'"
check "breaker.go MaxRequests defined" "grep -q 'MaxRequests' '$BREAKER'"
check "breaker.go Timeout defined" "grep -q 'Timeout' '$BREAKER'"

# db exponential backoff
DB_FILE="$API/internal/db/db.go"
check "internal/db/db.go exists" "[ -f '$DB_FILE' ]"
check "db.go uses backoff.Retry" "grep -q 'backoff.Retry\|backoff.NewExponentialBackOff' '$DB_FILE'"
check "db.go MaxConns pool tuning" "grep -q 'MaxConns' '$DB_FILE'"
check "db.go MinConns pool tuning" "grep -q 'MinConns' '$DB_FILE'"
check "db.go MaxConnLifetime pool tuning" "grep -q 'MaxConnLifetime' '$DB_FILE'"

# main.go uses circuit breaker
MAIN="$API/cmd/api/main.go"
check "main.go imports resilience package" "grep -q 'resilience' '$MAIN'"
check "main.go calls NewDBBreaker" "grep -q 'NewDBBreaker' '$MAIN'"
check "main.go uses dbBreaker.Execute" "grep -q 'dbBreaker.Execute\|\.Execute(' '$MAIN'"
check "main.go handles gobreaker.ErrOpenState" "grep -q 'ErrOpenState' '$MAIN'"

# Live API health check with circuit breaker response
API_STATUS=$(docker inspect coreme_api --format='{{.State.Status}}' 2>/dev/null || echo "missing")
if [ "$API_STATUS" = "running" ]; then
  HEALTH=$(docker exec coreme_api wget -qO- http://localhost:3001/health 2>/dev/null || echo "")
  if echo "$HEALTH" | grep -q '"status"'; then
    echo "  ✅ API /health responds (circuit breaker active)"; ((PASS++))
  else
    echo "  ❌ API /health not responding"; ((FAIL++))
  fi
else
  echo "  ❌ api container not running (status: $API_STATUS)"; ((FAIL++))
fi

echo "  Task 4: $PASS passed, $FAIL failed"
[ "$FAIL" -eq 0 ]
