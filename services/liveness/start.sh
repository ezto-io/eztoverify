#!/usr/bin/env bash
#
# Verify - Liveness Service (Python / Flask + Waitress)
# Local/on-prem start script.
#
# Usage:
#   ./start.sh                 create venv (if needed), install deps, start server
#   SKIP_INSTALL=1 ./start.sh  skip venv creation + pip install (deps already present)
#
# Requires Python 3.8+ and a reachable MinIO set via MINIO_HOST (.env).
# Serves the Flask app (app:app) with Waitress on PORT (default 5000).

set -euo pipefail

cd "$(dirname "$0")"

if [ ! -f .env ]; then
  echo "ERROR: .env not found. Copy .env.example to .env and configure MINIO_HOST etc." >&2
  exit 1
fi

# Load .env (export every var defined there) so the app sees MINIO_HOST, PORT, ...
set -a
# shellcheck disable=SC1091
. ./.env
set +a

VENV=.venv

if [ "${SKIP_INSTALL:-0}" != "1" ]; then
  if [ ! -d "$VENV" ]; then
    echo "==> Creating virtualenv ($VENV)..."
    python3 -m venv "$VENV"
  fi
  # shellcheck disable=SC1091
  . "$VENV/bin/activate"
  echo "==> Installing dependencies (requirements.txt)..."
  pip install --upgrade pip >/dev/null
  pip install -r requirements.txt
else
  # shellcheck disable=SC1091
  . "$VENV/bin/activate"
fi

echo "==> Starting Liveness service on port ${PORT:-5000}..."
exec waitress-serve --host 0.0.0.0 --port "${PORT:-5000}" app:app
