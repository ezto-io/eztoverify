#!/usr/bin/env bash
# Fast chart render checks for image placeholders and optional services.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
CHART="$ROOT/charts/ezto-verify"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

fail() { echo "FAIL: $*" >&2; exit 1; }
assert_has() { grep -Fq "$2" "$1" || fail "$1 does not contain: $2"; }
assert_lacks() { if grep -Fq "$2" "$1"; then fail "$1 unexpectedly contains: $2"; fi; }

# The chart ships placeholder repositories on purpose: an unconfigured install
# must render an obviously wrong image rather than silently pull someone else's.
helm template ezto-verify "$CHART" -f "$CHART/values-kind.yaml" > "$TMP/default.yaml"
assert_has "$TMP/default.yaml" 'image: "REPLACE_ME/ezto-idp:latest"'
assert_has "$TMP/default.yaml" 'image: "REPLACE_ME/ezto-background:latest"'
assert_has "$TMP/default.yaml" 'image: "REPLACE_ME/ezto-socket:latest"'
assert_has "$TMP/default.yaml" 'image: "REPLACE_ME/ezto-url-shortener:latest"'
assert_lacks "$TMP/default.yaml" 'name: ezto-verify-liveness'

# An operator-supplied registry must reach every service.
helm template ezto-verify "$CHART" -f "$CHART/values-kind.yaml" \
  --set idp.image.repository=example.test/myorg/ezto-idp \
  --set background.image.repository=example.test/myorg/ezto-background \
  --set socket.image.repository=example.test/myorg/ezto-socket \
  --set urlShortener.image.repository=example.test/myorg/ezto-url-shortener \
  > "$TMP/registry.yaml"
assert_has "$TMP/registry.yaml" 'image: "example.test/myorg/ezto-idp:latest"'
assert_has "$TMP/registry.yaml" 'image: "example.test/myorg/ezto-url-shortener:latest"'
assert_lacks "$TMP/registry.yaml" 'REPLACE_ME'

helm template ezto-verify "$CHART" -f "$CHART/values-kind.yaml" \
  --set background.enabled=false \
  --set socket.enabled=false \
  --set urlShortener.enabled=false > "$TMP/minimal.yaml"
assert_lacks "$TMP/minimal.yaml" 'name: ezto-verify-background'
assert_lacks "$TMP/minimal.yaml" 'name: ezto-verify-socket'
assert_lacks "$TMP/minimal.yaml" 'name: ezto-verify-url-shortener'
assert_has "$TMP/minimal.yaml" 'BACKGROUND_JOB_URL: ""'
assert_has "$TMP/minimal.yaml" 'SOCKET_URL: ""'
assert_has "$TMP/minimal.yaml" 'SHORTENER_HTTP_URL: ""'

helm template ezto-verify "$CHART" -f "$CHART/values-kind.yaml" \
  --set liveness.enabled=true > "$TMP/liveness.yaml"
assert_has "$TMP/liveness.yaml" 'name: ezto-verify-liveness'
assert_has "$TMP/liveness.yaml" 'image: "REPLACE_ME/ezto-liveness:latest"'

helm template ezto-verify "$CHART" -f "$CHART/values-kind.yaml" \
  -f "$CHART/values-local-build.yaml" > "$TMP/local-build.yaml"
assert_has "$TMP/local-build.yaml" 'image: "ezto-idp:latest"'
assert_has "$TMP/local-build.yaml" 'imagePullPolicy: Never'

helm lint "$CHART" >/dev/null
printf 'PASS: placeholder, registry-override, optional-service, liveness, and local-build renders\n'
