#!/usr/bin/env bash
echo "Testing Task 7: K3s manifests — YAML structure validation"
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
K8S="$ROOT/k8s"

# File existence
for f in namespace.yaml api-deployment.yaml realtime-deployment.yaml postgres-statefulset.yaml ingress.yaml pvc.yaml; do
  check "k8s/$f exists" "[ -f '$K8S/$f' ]"
done

# Sealed secrets
check "k8s/sealed-secrets/ directory exists" "[ -d '$K8S/sealed-secrets' ]"
check "sealed-secrets/coreme-sealedsecret.yaml exists" "[ -f '$K8S/sealed-secrets/coreme-sealedsecret.yaml' ]"

# namespace.yaml
check "namespace.yaml kind: Namespace" "grep -q 'kind: Namespace' '$K8S/namespace.yaml'"
check "namespace.yaml name: coreme" "grep -q 'name: coreme' '$K8S/namespace.yaml'"

# api-deployment.yaml
check "api-deployment.yaml kind: Deployment" "grep -q 'kind: Deployment' '$K8S/api-deployment.yaml'"
check "api-deployment.yaml namespace: coreme" "grep -q 'namespace: coreme' '$K8S/api-deployment.yaml'"
check "api-deployment.yaml livenessProbe defined" "grep -q 'livenessProbe' '$K8S/api-deployment.yaml'"
check "api-deployment.yaml readinessProbe defined" "grep -q 'readinessProbe' '$K8S/api-deployment.yaml'"

# realtime-deployment.yaml
check "realtime-deployment.yaml kind: Deployment" "grep -q 'kind: Deployment' '$K8S/realtime-deployment.yaml'"
check "realtime-deployment.yaml namespace: coreme" "grep -q 'namespace: coreme' '$K8S/realtime-deployment.yaml'"

# postgres-statefulset.yaml
check "postgres-statefulset.yaml kind: StatefulSet" "grep -q 'kind: StatefulSet' '$K8S/postgres-statefulset.yaml'"
check "postgres-statefulset.yaml volumeClaimTemplates" "grep -q 'volumeClaimTemplates' '$K8S/postgres-statefulset.yaml'"

# ingress.yaml
check "ingress.yaml kind: Ingress" "grep -q 'kind: Ingress' '$K8S/ingress.yaml'"
check "ingress.yaml TLS section" "grep -q 'tls:' '$K8S/ingress.yaml'"

# pvc.yaml
check "pvc.yaml kind: PersistentVolumeClaim" "grep -q 'kind: PersistentVolumeClaim' '$K8S/pvc.yaml'"
check "pvc.yaml 20Gi storage" "grep -q '20Gi' '$K8S/pvc.yaml'"

# sealed-secrets content
SS="$K8S/sealed-secrets/coreme-sealedsecret.yaml"
check "SealedSecret kind" "grep -q 'SealedSecret\|sealed-secret' '$SS'"

# kubeval / kubeconform (optional — may not be installed)
if command -v kubeval &>/dev/null; then
  for f in "$K8S"/*.yaml; do
    fname=$(basename "$f")
    if kubeval "$f" &>/dev/null; then
      echo "  ✅ kubeval: $fname valid"; ((PASS++))
    else
      echo "  ❌ kubeval: $fname invalid"; ((FAIL++))
    fi
  done
elif command -v kubeconform &>/dev/null; then
  for f in "$K8S"/*.yaml; do
    fname=$(basename "$f")
    if kubeconform "$f" &>/dev/null; then
      echo "  ✅ kubeconform: $fname valid"; ((PASS++))
    else
      echo "  ❌ kubeconform: $fname invalid"; ((FAIL++))
    fi
  done
else
  echo "  ⚠️  SKIP kubeval/kubeconform: neither tool installed — YAML schema validation skipped"
  ((SKIP++))
fi

# kubectl not expected in dev environment
if command -v kubectl &>/dev/null; then
  if kubectl get ns coreme &>/dev/null; then
    echo "  ✅ K3s namespace 'coreme' exists"; ((PASS++))
  else
    echo "  ⚠️  kubectl available but namespace 'coreme' not found (cluster not deployed)"; ((SKIP++))
  fi
else
  echo "  ⚠️  SKIP kubectl checks: kubectl not installed (expected in dev environment)"
  ((SKIP++))
fi

echo "  Task 7: $PASS passed, $FAIL failed, $SKIP skipped"
[ "$FAIL" -eq 0 ]
