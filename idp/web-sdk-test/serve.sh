#!/usr/bin/env bash
#
# Verify — Web SDK test harness server.
# Serves index.html over http so the browser sends an Origin header
# (the /ezto_web_push/.../register endpoint rejects file:// and missing-origin requests).
#
# Usage:  ./serve.sh [port]      (default 5500)
# Then whitelist the printed origin in the client's CIBA "whitelist_host" config.

set -e
PORT="${1:-5500}"
cd "$(dirname "$0")"

echo "Verify Web SDK test harness"
echo "  open:       http://localhost:${PORT}/"
echo "  whitelist:  http://localhost:${PORT}   <-- add this to the client's whitelist_host"
echo ""
exec python3 -m http.server "${PORT}"
