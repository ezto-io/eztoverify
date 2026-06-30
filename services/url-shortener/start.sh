#!/usr/bin/env bash
#
# Verify - URL Shortener Service
# Local/on-prem start script.
#
# Usage:
#   ./start.sh            install deps (if needed), run migrations, start server
#   SKIP_MIGRATE=1 ./start.sh   start without running prisma migrate
#
# Requires Node 18+ and a reachable PostgreSQL set via DATABASE_URL (.env).

set -euo pipefail

cd "$(dirname "$0")"

# Load .env if present (export every var defined there).
if [ -f .env ]; then
  set -a
  # shellcheck disable=SC1091
  . ./.env
  set +a
fi

if [ -z "${DATABASE_URL:-}" ]; then
  echo "ERROR: DATABASE_URL is not set. Copy .env.example to .env and configure it." >&2
  exit 1
fi

# Install dependencies on first run (or when node_modules is missing).
if [ ! -d node_modules ]; then
  echo "==> Installing dependencies (npm ci)..."
  if [ -f package-lock.json ]; then
    npm ci
  else
    npm install
  fi
fi

# Generate the Prisma client and apply migrations.
echo "==> Generating Prisma client..."
npx prisma generate

if [ "${SKIP_MIGRATE:-0}" != "1" ]; then
  echo "==> Applying database migrations..."
  npx prisma migrate deploy
fi

echo "==> Starting URL Shortener on port ${PORT:-3000}..."
exec node index.js
