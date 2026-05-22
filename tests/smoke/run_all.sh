#!/usr/bin/env bash
# Main smoke test runner — executes all task-specific tests and prints summary.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PASS=0; FAIL=0; SKIP=0
declare -a FAILED_TASKS=()

run_test() {
  local script="$1"
  local name="$(basename "$script" .sh)"
  echo ""
  echo "══════════════════════════════════════════════════"
  if bash "$script"; then
    PASS=$((PASS+1))
  else
    FAIL=$((FAIL+1))
    FAILED_TASKS+=("$name")
  fi
}

for f in "$SCRIPT_DIR"/task_*.sh; do
  run_test "$f"
done

echo ""
echo "══════════════════════════════════════════════════"
echo "ПІДСУМОК SMOKE TESTS"
echo "══════════════════════════════════════════════════"
echo "  Всього : $((PASS + FAIL + SKIP))"
echo "  Passed : $PASS"
echo "  Failed : $FAIL"
echo "  Skipped: $SKIP"
if [ ${#FAILED_TASKS[@]} -gt 0 ]; then
  echo ""
  echo "  Провалені:"
  for t in "${FAILED_TASKS[@]}"; do echo "    ❌ $t"; done
fi
echo ""
[ "$FAIL" -eq 0 ]
