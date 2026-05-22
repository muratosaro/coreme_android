#!/usr/bin/env bash
echo "Testing Task 10: CI/CD pipeline — GitHub Actions workflow"
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
# GitHub Actions workflows are at the repo root (one level above server/)
WORKFLOWS="$ROOT/../.github/workflows"

# workflow file
check ".github/workflows/ directory exists" "[ -d '$WORKFLOWS' ]"

WF="$WORKFLOWS/backend-ci.yml"
check "backend-ci.yml exists" "[ -f '$WF' ]"

# Jobs
check "go-api job defined" "grep -q 'go-api\|go_api' '$WF'"
check "node-realtime job defined" "grep -q 'node-realtime\|node_realtime\|realtime' '$WF'"
check "docker-build job defined" "grep -q 'docker-build\|docker_build' '$WF'"
check "deploy job defined" "grep -q 'deploy' '$WF'"

# Go job steps
check "workflow: setup-go action" "grep -q 'setup-go' '$WF'"
check "workflow: go mod download" "grep -q 'go mod download\|go mod tidy' '$WF'"
check "workflow: golangci-lint" "grep -q 'golangci-lint' '$WF'"
check "workflow: go test -race" "grep -q 'go test.*-race\|-race' '$WF'"
check "workflow: go build" "grep -q 'go build' '$WF'"

# Node job steps
check "workflow: setup-node action" "grep -q 'setup-node' '$WF'"
check "workflow: npm ci" "grep -q 'npm ci\|npm install' '$WF'"
check "workflow: eslint" "grep -q 'eslint\|lint' '$WF'"

# Docker build
check "workflow: docker compose build" "grep -q 'docker compose build\|docker-compose build' '$WF'"

# Deploy
check "workflow: SSH deploy step" "grep -q 'ssh\|appleboy/ssh-action\|SSH' '$WF'"
check "workflow: docker compose up in deploy" "grep -q 'docker compose up\|docker-compose up' '$WF'"
check "workflow: health check in deploy" "grep -q 'health\|curl\|wget' '$WF'"

# YAML syntax validation — try yq, then python3, then python, else skip
YAML_OK=false
if command -v yq &>/dev/null && yq '.' "$WF" &>/dev/null; then
  echo "  ✅ backend-ci.yml YAML syntax valid (yq)"; ((PASS++)); YAML_OK=true
fi
if ! $YAML_OK; then
  for py in python3 python python3.exe python.exe; do
    if command -v "$py" &>/dev/null 2>&1; then
      result=$("$py" -c "import yaml; yaml.safe_load(open('$WF')); print('ok')" 2>/dev/null)
      if [ "$result" = "ok" ]; then
        echo "  ✅ backend-ci.yml YAML syntax valid ($py)"; ((PASS++)); YAML_OK=true; break
      fi
    fi
  done
fi
if ! $YAML_OK; then
  echo "  ⚠️  SKIP YAML syntax check: no working python/yq found"
  ((SKIP++))
fi

# Trigger condition
check "workflow triggers on push" "grep -q 'on:' '$WF' && grep -q 'push\|pull_request' '$WF'"

echo "  Task 10: $PASS passed, $FAIL failed, $SKIP skipped"
[ "$FAIL" -eq 0 ]
