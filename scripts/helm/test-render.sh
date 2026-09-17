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

# ... including when liveness is the service being enabled.
helm template ezto-verify "$CHART" -f "$CHART/values-kind.yaml" \
  --set liveness.enabled=true \
  --set liveness.image.repository=example.test/myorg/ezto-liveness \
  --set liveness.image.tag=2.1.0 > "$TMP/registry-liveness.yaml"
assert_has "$TMP/registry-liveness.yaml" 'name: ezto-verify-liveness'
assert_has "$TMP/registry-liveness.yaml" 'image: "example.test/myorg/ezto-liveness:2.1.0"'

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

for overlay in kind minikube; do
  helm template ezto-verify "$CHART" -f "$CHART/values-$overlay.yaml" \
    -f "$CHART/values-local-build.yaml" > "$TMP/local-build-$overlay.yaml"
  assert_has "$TMP/local-build-$overlay.yaml" 'image: "ezto-idp:latest"'
  assert_has "$TMP/local-build-$overlay.yaml" 'imagePullPolicy: Never'
  assert_lacks "$TMP/local-build-$overlay.yaml" 'REPLACE_ME'
done

helm lint "$CHART" >/dev/null

# Installer plan checks. --print-plan reaches no cluster and writes no files.
INSTALL="$ROOT/scripts/install.sh"

# Removing a release must not demand an image source or credentials.
"$INSTALL" --non-interactive --uninstall --context test-ctx --print-plan > "$TMP/uninstall-plan.txt"
assert_has "$TMP/uninstall-plan.txt" "Would uninstall Helm release 'ezto-verify'"

# Installing without any image source must refuse rather than guess.
if "$INSTALL" --non-interactive --context test-ctx --print-plan > "$TMP/no-image.txt" 2>&1; then
  fail "install.sh accepted an install with no image source"
fi
assert_has "$TMP/no-image.txt" 'no image source was given'

# Both image layouts must resolve for every enabled service.
"$INSTALL" --non-interactive --context test-ctx --registry example.test/myorg --image-tag 3.2.1 \
  --print-plan > "$TMP/plan-registry.txt"
assert_has "$TMP/plan-registry.txt" 'idp=example.test/myorg/ezto-idp:3.2.1'
assert_has "$TMP/plan-registry.txt" 'url-shortener=example.test/myorg/ezto-url-shortener:3.2.1'

"$INSTALL" --non-interactive --context test-ctx --image-repo example.test/myorg/ezto-oss \
  --services socket --print-plan > "$TMP/plan-repo.txt"
assert_has "$TMP/plan-repo.txt" 'idp=example.test/myorg/ezto-oss:idp'
assert_has "$TMP/plan-repo.txt" 'socket=example.test/myorg/ezto-oss:socket'
assert_lacks "$TMP/plan-repo.txt" 'background='

printf 'PASS: chart renders (placeholder, registry override, liveness, optional services, both local-build overlays) and installer plans\n'
