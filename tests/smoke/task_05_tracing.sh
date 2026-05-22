#!/usr/bin/env bash
echo "Testing Task 5: OpenTelemetry tracing"
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
DC="$ROOT/docker-compose.yml"

# go.mod OTel dependencies
check "otelgin instrumentation in go.mod" "grep -q 'otelgin' '$API/go.mod'"
check "otel SDK in go.mod" "grep -q 'go.opentelemetry.io/otel/sdk' '$API/go.mod'"
check "otlptracehttp exporter in go.mod" "grep -q 'otlptracehttp' '$API/go.mod'"

# tracing package
TRACING="$API/internal/tracing/tracing.go"
check "internal/tracing/tracing.go exists" "[ -f '$TRACING' ]"
check "tracing.go Init function" "grep -q 'func Init' '$TRACING'"
check "tracing.go OTLP exporter" "grep -q 'otlptracehttp\|otlptrace' '$TRACING'"
check "tracing.go sampler (ratio)" "grep -q 'TraceIDRatioBased\|Sampler' '$TRACING'"
check "tracing.go service.name resource" "grep -q 'service.name\|semconv' '$TRACING'"
check "tracing.go shutdown returned" "grep -q 'shutdown\|Shutdown' '$TRACING'"

# main.go uses tracing
MAIN="$API/cmd/api/main.go"
check "main.go imports tracing package" "grep -q '\".*tracing\"' '$MAIN'"
check "main.go calls tracing.Init" "grep -q 'tracing.Init' '$MAIN'"
check "main.go defers shutdownTracing" "grep -q 'shutdownTracing\|defer.*shutdown' '$MAIN'"
check "main.go uses otelgin middleware" "grep -q 'otelgin.Middleware' '$MAIN'"

# docker-compose Jaeger service (under observability profile)
check "jaeger service in docker-compose" "grep -q 'jaeger' '$DC'"
check "OTEL_EXPORTER_OTLP_ENDPOINT in docker-compose" "grep -q 'OTEL_EXPORTER_OTLP_ENDPOINT' '$DC'"

# Jaeger container (only runs under observability profile — may not be started)
JAEGER_STATUS=$(docker inspect coreme_jaeger --format='{{.State.Status}}' 2>/dev/null || echo "missing")
if [ "$JAEGER_STATUS" = "running" ]; then
  echo "  ✅ Jaeger container running"; ((PASS++))
  if curl -sk http://localhost:16686 2>/dev/null | grep -qi 'jaeger'; then
    echo "  ✅ Jaeger UI accessible at :16686"; ((PASS++))
  else
    echo "  ❌ Jaeger UI not responding at :16686"; ((FAIL++))
  fi
else
  echo "  ⚠️  Jaeger not running (status: $JAEGER_STATUS) — observability profile not started. SKIP live Jaeger check."
fi

echo "  Task 5: $PASS passed, $FAIL failed"
[ "$FAIL" -eq 0 ]
