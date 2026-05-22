#!/usr/bin/env bash
echo "Testing Task 1: Nginx reverse proxy + health endpoints"
PASS=0; FAIL=0

check() {
  local desc="$1"; local cmd="$2"
  if eval "$cmd" &>/dev/null; then
    echo "  ✅ $desc"; ((PASS++))
  else
    echo "  ❌ $desc"; ((FAIL++))
  fi
}

# nginx config file exists
check "nginx/nginx.conf exists" "[ -f '$(dirname "$0")/../../nginx/nginx.conf' ]"
check "nginx/Dockerfile exists" "[ -f '$(dirname "$0")/../../nginx/Dockerfile' ]"
check "nginx/entrypoint.sh exists" "[ -f '$(dirname "$0")/../../nginx/entrypoint.sh' ]"

# nginx config must contain key directives
CONF="$(dirname "$0")/../../nginx/nginx.conf"
check "nginx: HTTP->HTTPS redirect (return 301)" "grep -q 'return 301' '$CONF'"
check "nginx: rate limiting zone 'general' 30r/s" "grep -q 'rate=30r/s' '$CONF'"
check "nginx: rate limiting zone 'auth' 5r/s" "grep -q 'rate=5r/s' '$CONF'"
check "nginx: HSTS header present" "grep -q 'Strict-Transport-Security' '$CONF'"
check "nginx: X-Frame-Options DENY" "grep -q 'X-Frame-Options.*DENY' '$CONF'"
check "nginx: client_max_body_size 50M" "grep -q 'client_max_body_size 50M' '$CONF'"
check "nginx: WebSocket Upgrade header" "grep -q '\"upgrade\"' '$CONF'"
check "nginx: /health endpoint returns 200" "grep -q 'return 200' '$CONF'"
check "nginx: /api/ proxied to api:3001" "grep -q 'proxy_pass.*api:3001' '$CONF'"
check "nginx: /socket.io/ proxied to realtime:3002" "grep -q 'proxy_pass.*realtime:3002' '$CONF'"

# nginx container state
NGINX_STATUS=$(docker inspect coreme_nginx --format='{{.State.Status}}' 2>/dev/null || echo "missing")
if [ "$NGINX_STATUS" = "running" ]; then
  echo "  ✅ nginx container running"
  ((PASS++))
  # Test HTTP redirect
  if curl -sk -o /dev/null -w "%{http_code}" http://localhost/ 2>/dev/null | grep -q "301"; then
    echo "  ✅ HTTP:80 -> 301 redirect"
    ((PASS++))
  else
    echo "  ❌ HTTP:80 -> 301 redirect (no response)"
    ((FAIL++))
  fi
  # Test HTTPS /health
  if curl -sk https://localhost/health 2>/dev/null | grep -q '"status"'; then
    echo "  ✅ HTTPS /health returns JSON"
    ((PASS++))
  else
    echo "  ❌ HTTPS /health not responding"
    ((FAIL++))
  fi
else
  echo "  ❌ nginx container NOT running (status: $NGINX_STATUS) — depends on realtime being healthy"
  ((FAIL++))
  echo "     [KNOWN CAUSE] realtime is not started because nats healthcheck fails"
fi

echo "  Task 1: $PASS passed, $FAIL failed"
[ "$FAIL" -eq 0 ]
