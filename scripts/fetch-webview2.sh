#!/usr/bin/env bash
# Fetch the Microsoft Edge WebView2 Evergreen Standalone Installer used by the
# Windows desktop build of ezto_verify_sdk. The binary is a ~170 MB Microsoft
# redistributable and is intentionally NOT committed (git-ignored).
#
# Usage: scripts/fetch-webview2.sh
set -euo pipefail

DEST="authenticator/ezto_verify_sdk/webviewAsset/WebView2Runtime.exe"
URL="https://go.microsoft.com/fwlink/p/?LinkId=2124703"

cd "$(dirname "$0")/.."
mkdir -p "$(dirname "$DEST")"

if [ -f "$DEST" ]; then
  echo "WebView2 installer already present: $DEST"
  exit 0
fi

echo "Downloading WebView2 Evergreen Standalone Installer..."
curl -fL "$URL" -o "$DEST"
echo "Saved to $DEST"
