#!/usr/bin/env bash
# Generates strong random secrets for .env and prints them to stdout.
# Usage:
#   ./server/scripts/generate-secrets.sh >> server/.env
#
# Requires: openssl
set -euo pipefail

gen() {
  openssl rand -base64 48 | tr -d '\n/+=' | cut -c1-64
}

echo "JWT_SECRET=$(gen)"
echo "JWT_REFRESH_SECRET=$(gen)"
echo ""
echo "# Paste these values into server/.env"
echo "# Then seal them for K3s:"
echo "#   ./server/scripts/seal-secrets.sh"
