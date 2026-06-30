#!/usr/bin/env bash
# Start the Verify documentation site (Docusaurus 2).
#
#   ./start.sh           # dev server with live reload (default port 3000)
#   ./start.sh build     # build static bundle into ./build then serve it
#   PORT=3100 ./start.sh # override port
#
# Opt-outs:
#   SKIP_INSTALL=1        # skip `npm install` even if node_modules is missing
#
# Docusaurus 2.1.0 requires Node >=16 <21 (see package.json engines). Node 21+
# breaks the build ("document is not defined"). Use nvm: `nvm use` (reads .nvmrc).

set -euo pipefail
cd "$(dirname "$0")"

# --- Node version guard -----------------------------------------------------
NODE_MAJOR="$(node -p 'process.versions.node.split(".")[0]' 2>/dev/null || echo 0)"
if [ "$NODE_MAJOR" -lt 16 ] || [ "$NODE_MAJOR" -gt 20 ]; then
  echo "ERROR: Node $(node --version 2>/dev/null || echo '?') detected; need >=16 <21."
  echo "       Run 'nvm use' (reads .nvmrc -> 20) then retry."
  exit 1
fi

# --- Dependencies -----------------------------------------------------------
if [ -z "${SKIP_INSTALL:-}" ] && [ ! -d node_modules ]; then
  echo "Installing dependencies (first run)..."
  npm install
fi

PORT="${PORT:-3000}"

# --- Run --------------------------------------------------------------------
if [ "${1:-}" = "build" ]; then
  npm run build
  exec npm run serve -- --port "$PORT"
else
  exec npm run start -- --port "$PORT"
fi
