#!/usr/bin/env bash
# =============================================================================
# EZTO Verify - Socket service launcher
# =============================================================================
# Installs dependencies (once) then starts the Express + Socket.IO server.
# Env is loaded by the app via dotenv from `.env` (copy `.env.example` first).
#
#   ./start.sh                # install (if needed), ensure schema, then run
#   SKIP_INSTALL=1 ./start.sh # skip npm install (deps already present)
#   SKIP_MIGRATE=1 ./start.sh # skip the CREATE TABLE bootstrap
# =============================================================================
set -euo pipefail

cd "$(dirname "$0")"

if [ ! -f .env ]; then
  echo "ERROR: .env not found. Copy .env.example to .env and set your DB creds." >&2
  exit 1
fi

if [ "${SKIP_INSTALL:-0}" != "1" ] && [ ! -d node_modules ]; then
  echo "Installing dependencies..."
  npm install
fi

if [ "${SKIP_MIGRATE:-0}" != "1" ]; then
  echo "Ensuring DB schema..."
  node db/bootstrap.js
fi

PORT="$(grep -E '^PORT=' .env | head -1 | cut -d= -f2- | tr -d '[:space:]')"
echo "Starting socket service on port ${PORT:-3001}..."
exec node index.js
