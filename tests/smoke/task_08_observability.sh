#!/usr/bin/env bash
echo "Testing Task 8: Observability — Prometheus, Grafana, Loki, AlertManager"
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
MON="$ROOT/monitoring"
DC="$ROOT/docker-compose.yml"
API="$ROOT/api"

# Config file existence
check "monitoring/prometheus.yml exists" "[ -f '$MON/prometheus.yml' ]"
check "monitoring/rules/alerts.yml exists" "[ -f '$MON/rules/alerts.yml' ]"
check "monitoring/alertmanager.yml exists" "[ -f '$MON/alertmanager.yml' ]"
check "monitoring/loki-config.yml exists" "[ -f '$MON/loki-config.yml' ]"

# Prometheus config content
PROM="$MON/prometheus.yml"
check "prometheus.yml scrapes api job" "grep -q 'job_name.*api\|job.*coreme' '$PROM'"
check "prometheus.yml rule_files defined" "grep -q 'rule_files' '$PROM'"
check "prometheus.yml alertmanager configured" "grep -q 'alertmanager\|alertmanagers' '$PROM'"

# Alert rules
ALERTS="$MON/rules/alerts.yml"
check "alerts.yml has at least one alert rule" "grep -q 'alert:' '$ALERTS'"

# AlertManager config
AM="$MON/alertmanager.yml"
check "alertmanager.yml has receiver" "grep -q 'receiver\|receivers' '$AM'"

# Go API has Prometheus metrics endpoint
check "prometheus/client_golang in go.mod" "grep -q 'prometheus/client_golang' '$API/go.mod'"
MAIN="$API/cmd/api/main.go"
check "main.go exposes /metrics endpoint" "grep -q '/metrics' '$MAIN'"
check "main.go uses promhttp.Handler" "grep -q 'promhttp' '$MAIN'"

# docker-compose observability profile
check "prometheus service in docker-compose" "grep -q 'prometheus' '$DC'"
check "grafana service in docker-compose" "grep -q 'grafana' '$DC'"
check "loki service in docker-compose" "grep -q 'loki' '$DC'"
check "alertmanager service in docker-compose" "grep -q 'alertmanager' '$DC'"
check "observability profile used" "grep -q 'observability' '$DC'"

# API /metrics live check (api may be running without observability profile)
API_STATUS=$(docker inspect coreme_api --format='{{.State.Status}}' 2>/dev/null || echo "missing")
if [ "$API_STATUS" = "running" ]; then
  METRICS=$(docker exec coreme_api wget -qO- http://localhost:3001/metrics 2>/dev/null || echo "")
  if echo "$METRICS" | grep -q '# HELP\|go_goroutines'; then
    echo "  ✅ API /metrics exposes Prometheus metrics"; ((PASS++))
  else
    echo "  ❌ API /metrics not returning Prometheus format"; ((FAIL++))
  fi
else
  echo "  ❌ api container not running — cannot check /metrics"; ((FAIL++))
fi

# Observability containers (only under profile)
for svc in prometheus grafana loki alertmanager; do
  STATUS=$(docker inspect "coreme_${svc}" --format='{{.State.Status}}' 2>/dev/null || echo "missing")
  if [ "$STATUS" = "running" ]; then
    echo "  ✅ $svc container running"; ((PASS++))
  else
    echo "  ⚠️  SKIP $svc container: not running (status: $STATUS) — requires --profile observability"
    ((SKIP++))
  fi
done

echo "  Task 8: $PASS passed, $FAIL failed, $SKIP skipped"
[ "$FAIL" -eq 0 ]
