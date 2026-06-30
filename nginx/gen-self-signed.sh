#!/usr/bin/env bash
# Generate a self-signed cert for LOCAL/testing of the nginx HTTPS overlay.
# For real deployments use a CA cert (Let's Encrypt, your org CA) instead.
#
# Usage:  DOMAIN=verify.local ./gen-self-signed.sh
set -euo pipefail
cd "$(dirname "$0")"
DOMAIN="${DOMAIN:-verify.local}"
mkdir -p certs
openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
  -keyout certs/privkey.pem \
  -out    certs/fullchain.pem \
  -subj   "/CN=${DOMAIN}" \
  -addext "subjectAltName=DNS:${DOMAIN}"
echo "Wrote certs/fullchain.pem + certs/privkey.pem for ${DOMAIN}"
echo "Self-signed — browsers will warn. Add '${DOMAIN}' to /etc/hosts -> 127.0.0.1 for local testing."
