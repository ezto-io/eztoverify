#!/usr/bin/env bash
# Asserts the compose deployment contract via `docker compose config`.
# Starts no containers and pulls no images.
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
COMPOSE_FILE="$REPO_ROOT/docker-compose.yml"
BUILD_FILE="$REPO_ROOT/docker-compose.build.yml"
ENV_EXAMPLE="$REPO_ROOT/.env.example"

FAILURES=0
pass() { printf '  ok   %s\n' "$1"; }
fail() { printf '  FAIL %s\n' "$1" >&2; FAILURES=$((FAILURES + 1)); }

# A fully populated environment used by assertions that need the graph to
# resolve. Deliberately NOT sourced from any .env on disk, so a developer's
# local .env can never make this suite pass or fail.
full_env() {
  cat <<'ENV'
DOMAIN=verify.example.com
REGISTRY=registry.example.com/ezto
TAG=1.0.0
TLS_MODE=self-signed
POSTGRES_USER=ezto
POSTGRES_PASSWORD=pgpw
POSTGRES_HOST=postgres
POSTGRES_PORT=5432
MINIO_ROOT_USER=ezto
MINIO_ROOT_PASSWORD=miniopw
MINIO_SERVER_URL=http://minio:9000
MINIO_PUBLIC_URL=https://verify.example.com/storage/ezto-verify/public
MINIO_BUCKET=ezto-verify
KEYCLOAK_ADMIN=admin
KEYCLOAK_ADMIN_PASSWORD=adminpw
SHORTENER_KEY=shortkey
ES_HOST=elasticsearch
ES_PORT=9200
ES_SCHEME=http
ROOT_ACCESS_IP=127.0.0.1,::1
BACKGROUND_JOB_URL=http://background:8070
SOCKET_URL=http://socket:3001
SHORTENER_HTTP_URL=http://url-shortener:3000
SHORTENER_HTTPS_URL=http://url-shortener:3000
LIVELINESS_URL=
DOCS_URL=
DOCS_LINK=
ENV
}

# Runs `docker compose config` with an explicit environment.
# $1 = COMPOSE_PROFILES value. Remaining args are extra -f files.
run_config() {
  local profiles="$1"; shift
  local env_file rc
  env_file="$(mktemp)"
  full_env > "$env_file"
  printf 'COMPOSE_PROFILES=%s\n' "$profiles" >> "$env_file"
  local args=(-f "$COMPOSE_FILE")
  local extra
  for extra in "$@"; do args+=(-f "$extra"); done
  ( cd "$REPO_ROOT" && docker compose --env-file "$env_file" "${args[@]}" config 2>&1 )
  rc=$?
  rm -f "$env_file"
  return $rc
}

ALL_PROFILES=infra-postgres,infra-minio,infra-elasticsearch,background,socket,url-shortener

echo "== no literal credentials in the tracked compose file =="
# Anchored to a YAML key at the start of a line. Without the anchor the
# interpolation `${POSTGRES_PASSWORD:?}` matches itself — "PASSWORD" then ":"
# then "?" — and the assertion fires on the very construct it is asking for.
CRED_LITERAL='^[[:space:]]+[A-Z_]*(PASSWORD|SECRET|ROOT_USER|VALID_API_KEYS|SHORTENER_KEY)[A-Z_]*:[[:space:]]*[^$[:space:]]'
if grep -nE "$CRED_LITERAL" "$COMPOSE_FILE" >/dev/null; then
  grep -nE "$CRED_LITERAL" "$COMPOSE_FILE" >&2
  fail "credential keys must reference \${VAR}, not literals"
else
  pass "every credential key references an environment variable"
fi

if grep -nE 'change_me|minioadmin123|REPLACE_ME' "$COMPOSE_FILE" >/dev/null; then
  fail "placeholder credential values remain in the compose file"
else
  pass "no change_me / REPLACE_ME placeholders remain"
fi

echo "== the stack fails closed with an empty environment =="
if ( cd "$REPO_ROOT" && docker compose --env-file /dev/null -f "$COMPOSE_FILE" config >/dev/null 2>&1 ); then
  fail "compose resolved with no environment; required values must use \${VAR:?}"
else
  pass "compose refuses to resolve without required values"
fi

echo "== .env.example and the compose file agree =="
compose_vars() {
  # Two things must be stripped before scanning, or the assertion reports
  # defects that are not there:
  #   - comments, which document interpolation using `${VAR:?}` as prose;
  #   - `$$`, Compose's escape for a literal `$` handed to the container's own
  #     shell, so `$${MASTER_SSL_REQUIRED:-external}` is NOT a Compose variable.
  sed -e 's/#.*$//' -e 's/\$\$//g' "$COMPOSE_FILE" \
    | grep -oE '\$\{[A-Z_][A-Z0-9_]*' | sed 's/^\${//' | sort -u
}
example_vars() {
  grep -oE '^[A-Z_][A-Z0-9_]*=' "$ENV_EXAMPLE" | sed 's/=$//' | sort -u
}
# Variables the deployer consumes but the compose file never interpolates.
# COMPOSE_PROFILES is read by Compose itself; TLS_MODE only steers certificate
# handling in scripts/deploy-compose.sh.
DEPLOYER_ONLY_VARS='^(COMPOSE_PROFILES|TLS_MODE)$'
missing="$(comm -23 <(compose_vars) <(example_vars))"
if [ -n "$missing" ]; then
  printf '%s\n' "$missing" >&2
  fail "compose references variables absent from .env.example"
else
  pass "every compose variable is documented in .env.example"
fi
unused="$(comm -13 <(compose_vars) <(example_vars) | grep -vE "$DEPLOYER_ONLY_VARS")"
if [ -n "$unused" ]; then
  printf '%s\n' "$unused" >&2
  fail ".env.example documents variables the compose file never reads"
else
  pass "every .env.example variable is read by the compose file"
fi

echo "== the deployment file builds nothing =="
if grep -nE '^[[:space:]]+build:' "$COMPOSE_FILE" >/dev/null; then
  fail "docker-compose.yml still declares build contexts; they belong in docker-compose.build.yml"
else
  pass "docker-compose.yml declares no build context"
fi

echo "== REGISTRY and TAG reach every service image =="
rendered="$(run_config "$ALL_PROFILES,face-biometrics")"
for svc in idp background socket url-shortener liveness; do
  if printf '%s' "$rendered" | grep -q "registry.example.com/ezto/ezto-${svc}:1.0.0"; then
    pass "$svc resolves to REGISTRY/ezto-$svc:TAG"
  else
    fail "$svc does not resolve from REGISTRY and TAG"
  fi
done

echo "== the build overlay restores every contributor build context =="
if [ ! -f "$BUILD_FILE" ]; then
  fail "docker-compose.build.yml is missing"
else
  built="$(run_config "$ALL_PROFILES,face-biometrics" "$BUILD_FILE")"
  count="$(printf '%s' "$built" | grep -cE '^[[:space:]]+context:')"
  if [ "$count" -eq 5 ]; then
    pass "the overlay restores 5 build contexts"
  else
    fail "the overlay restores $count build contexts, expected 5"
  fi
fi

echo
if [ "$FAILURES" -eq 0 ]; then
  echo "All compose config assertions passed."
  exit 0
fi
echo "$FAILURES assertion(s) failed." >&2
exit 1
