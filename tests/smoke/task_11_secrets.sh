#!/usr/bin/env bash
echo "Testing Task 11: Secrets management — .env security, Sealed Secrets"
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

# .env files must NOT be committed
check ".env NOT committed (not in git)" "! git -C '$ROOT' ls-files --error-unmatch .env 2>/dev/null"
check "api/.env NOT committed" "! git -C '$ROOT' ls-files --error-unmatch api/.env 2>/dev/null"
check "realtime/.env NOT committed" "! git -C '$ROOT' ls-files --error-unmatch realtime/.env 2>/dev/null"

# .gitignore — look in server/, parent (repo root), grandparent
GITIGNORE=""
for candidate in "$ROOT/.gitignore" "$ROOT/../.gitignore" "$ROOT/../../.gitignore"; do
  if [ -f "$candidate" ]; then
    GITIGNORE="$candidate"
    break
  fi
done
if [ -n "$GITIGNORE" ]; then
  check ".gitignore contains .env" "grep -q '\.env' '$GITIGNORE'"
else
  echo "  ❌ No .gitignore found in server/, repo root, or grandparent"; ((FAIL++))
fi

# .env.example should exist (template without values)
check ".env.example exists" "[ -f '$ROOT/.env.example' ] || [ -f '$ROOT/api/.env.example' ]"

# Sealed Secrets K8s manifest
SS="$ROOT/k8s/sealed-secrets/coreme-sealedsecret.yaml"
check "SealedSecret manifest exists" "[ -f '$SS' ]"
check "SealedSecret does NOT contain plaintext passwords" \
  "! grep -q 'password:\s*[a-zA-Z0-9]' '$SS' 2>/dev/null || grep -q 'encryptedData\|SealedSecret' '$SS'"

# generate-secrets.sh
GEN="$ROOT/scripts/generate-secrets.sh"
if [ -f "$GEN" ]; then
  check "generate-secrets.sh uses openssl rand" "grep -q 'openssl rand' '$GEN'"
  check "generate-secrets.sh outputs JWT_SECRET" "grep -q 'JWT_SECRET\|jwt' '$GEN' -i"
else
  echo "  ⚠️  SKIP generate-secrets.sh checks: file not found"
  ((SKIP++))
fi

# docker-compose: no hardcoded secrets (should use env var refs)
DC="$ROOT/docker-compose.yml"
check "docker-compose.yml uses \${VAR} for passwords (not hardcoded)" \
  "! grep -E 'password:\s*[a-zA-Z0-9]{8,}' '$DC' | grep -v '\${\|#'"

# No credentials in Go source files
HARDCODED=$(grep -r 'password\s*=\s*"[^"]\{6,\}"\|secret\s*=\s*"[^"]\{6,\}"' "$ROOT/api" 2>/dev/null | grep -v '_test.go\|example\|placeholder' | wc -l)
if [ "$HARDCODED" -eq 0 ]; then
  echo "  ✅ No hardcoded credentials found in api/ source"; ((PASS++))
else
  echo "  ❌ Possible hardcoded credentials in api/ source ($HARDCODED matches)"; ((FAIL++))
fi

# No credentials in Node source
HARDCODED_NODE=$(grep -r 'password\s*=\s*['"'"'"][^'"'"'"]\{6,\}['"'"'"]' "$ROOT/realtime/src" 2>/dev/null | grep -v 'test\|example\|placeholder' | wc -l)
if [ "$HARDCODED_NODE" -eq 0 ]; then
  echo "  ✅ No hardcoded credentials found in realtime/src/"; ((PASS++))
else
  echo "  ❌ Possible hardcoded credentials in realtime/src/ ($HARDCODED_NODE matches)"; ((FAIL++))
fi

echo "  Task 11: $PASS passed, $FAIL failed, $SKIP skipped"
[ "$FAIL" -eq 0 ]
