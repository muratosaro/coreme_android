#!/bin/sh
set -e

SSL_DIR=/etc/nginx/ssl

if [ ! -f "$SSL_DIR/cert.pem" ] || [ ! -f "$SSL_DIR/key.pem" ]; then
  mkdir -p "$SSL_DIR"
  openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
    -keyout "$SSL_DIR/key.pem" \
    -out "$SSL_DIR/cert.pem" \
    -subj "/CN=localhost/O=CoreMe/C=UA" \
    2>/dev/null
  echo "[nginx] Self-signed TLS certificate generated."
fi

exec nginx -g 'daemon off;'
