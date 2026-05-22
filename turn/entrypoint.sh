#!/bin/sh
set -e

CONF=/etc/turnserver.conf

# Inject required values from env
if [ -n "$TURN_USERNAME" ] && [ -n "$TURN_PASSWORD" ]; then
  echo "user=${TURN_USERNAME}:${TURN_PASSWORD}" >> "${CONF}"
fi

if [ -n "$TURN_REALM" ]; then
  echo "realm=${TURN_REALM}" >> "${CONF}"
fi

if [ -n "$TURN_PUBLIC_IP" ]; then
  echo "listening-ip=${TURN_PUBLIC_IP}" >> "${CONF}"
  echo "relay-ip=${TURN_PUBLIC_IP}" >> "${CONF}"
fi

exec turnserver -c "${CONF}"
