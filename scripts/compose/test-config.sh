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

# Never sourced from a real .env: a local file must not affect the result.
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
# Line-anchored, or `${POSTGRES_PASSWORD:?}` matches itself.
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
  # Strip comments and `$$` escapes; neither is a Compose variable.
  sed -e 's/#.*$//' -e 's/\$\$//g' "$COMPOSE_FILE" \
    | grep -oE '\$\{[A-Z_][A-Z0-9_]*' | sed 's/^\${//' | sort -u
}
example_vars() {
  grep -oE '^[A-Z_][A-Z0-9_]*=' "$ENV_EXAMPLE" | sed 's/=$//' | sort -u
}
# Read by Compose or the deployer, never interpolated by the compose file.
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

echo "== only nginx publishes a port =="
# Attribute each rendered `published:` key to its owning service.
publishers="$(run_config "$ALL_PROFILES,face-biometrics" | awk '
  /^  [a-z0-9_-]+:$/ { svc = $1; sub(/:$/, "", svc) }
  /published:/       { gsub(/"/, "", $2); print svc "=" $2 }
')"
offenders="$(printf '%s\n' "$publishers" | grep -v '^nginx=' | grep -v '^$')"
if [ -n "$offenders" ]; then
  printf '%s\n' "$offenders" >&2
  fail "a service other than nginx publishes a port"
else
  pass "nginx is the only service publishing a port"
fi

# Numeric sort: 443 lexicographically precedes 80.
nginx_ports="$(printf '%s\n' "$publishers" | grep '^nginx=' | cut -d= -f2 | sort -n -u | paste -sd, -)"
if [ "$nginx_ports" = "80,443" ]; then
  pass "nginx publishes exactly 80 and 443"
else
  fail "nginx publishes '$nginx_ports', expected '80,443'"
fi

echo "== dropping a service profile removes it and blanks its advertised URL =="
# Absence and blank URL asserted together, as a real deploy produces them.
assert_service_dropped() {
  local profile="$1" service="$2" var="$3"
  local profiles out rc env_file
  profiles="$(printf '%s' "$ALL_PROFILES" | tr ',' '\n' | grep -v "^${profile}$" | paste -sd, -)"
  env_file="$(mktemp)"
  full_env | grep -v "^${var}=" > "$env_file"
  printf '%s=\n' "$var" >> "$env_file"
  printf 'COMPOSE_PROFILES=%s\n' "$profiles" >> "$env_file"
  out="$( cd "$REPO_ROOT" && docker compose --env-file "$env_file" -f "$COMPOSE_FILE" config 2>&1 )"
  rc=$?
  rm -f "$env_file"
  if [ $rc -ne 0 ]; then
    printf '%s\n' "$out" >&2
    fail "the graph does not resolve with '$profile' dropped"
    return
  fi
  if printf '%s\n' "$out" | grep -qE "^  ${service}:$"; then
    fail "'$service' is still present with profile '$profile' dropped"
  else
    pass "'$service' is absent with profile '$profile' dropped"
  fi
  # Two patterns: BSD grep rejects an alternation with an empty branch.
  if printf '%s\n' "$out" | grep -qE "^ +${var}: *(\"\"|'')$" \
    || printf '%s\n' "$out" | grep -qE "^ +${var}: *$"; then
    pass "$var is blank with '$service' off"
  else
    printf '%s\n' "$out" | grep -E "^ +${var}:" >&2
    fail "$var is not blank with '$service' off"
  fi
}

assert_service_dropped background     background     BACKGROUND_JOB_URL
assert_service_dropped socket         socket         SOCKET_URL
assert_service_dropped url-shortener  url-shortener  SHORTENER_HTTP_URL

echo "== dropping a bundled store leaves the rest of the graph resolvable =="
without_pg="$(printf '%s' "$ALL_PROFILES" | tr ',' '\n' | grep -v '^infra-postgres$' | paste -sd, -)"
if out="$(run_config "$without_pg")"; then
  if printf '%s\n' "$out" | grep -qE '^  postgres:$'; then
    fail "'postgres' is still present with infra-postgres dropped"
  else
    pass "'postgres' is absent with infra-postgres dropped"
  fi
  if printf '%s\n' "$out" | grep -qE '^  idp:$'; then
    pass "the idp still resolves against an external database"
  else
    fail "the idp vanished when infra-postgres was dropped"
  fi
else
  printf '%s\n' "$out" >&2
  fail "the graph does not resolve with infra-postgres dropped"
fi

echo "== face liveness is off by default =="
if run_config "$ALL_PROFILES" | grep -qE '^  liveness:$'; then
  fail "liveness runs without the face-biometrics profile"
else
  pass "liveness requires the face-biometrics profile"
fi

echo "== every long-running service restarts and caps its logs =="
# Per-service blocks, so a key is never credited to a neighbour.
service_block() {
  printf '%s\n' "$2" | awk -v s="  $1:" '
    $0 == s { inblock = 1; next }
    /^  [a-z0-9_-]+:$/ { inblock = 0 }
    inblock { print }
  '
}

rendered_all="$(run_config "$ALL_PROFILES,face-biometrics")"
for svc in postgres minio elasticsearch idp nginx background socket url-shortener liveness; do
  block="$(service_block "$svc" "$rendered_all")"
  if printf '%s\n' "$block" | grep -q 'restart: unless-stopped'; then
    pass "$svc restarts unless stopped"
  else
    fail "$svc has no 'restart: unless-stopped'"
  fi
  if printf '%s\n' "$block" | grep -q 'max-size'; then
    pass "$svc caps its log size"
  else
    fail "$svc does not cap its log size"
  fi
done

echo "== one-shot jobs do not restart =="
for svc in minio-init idp-init certs-check; do
  block="$(service_block "$svc" "$rendered_all")"
  if printf '%s\n' "$block" | grep -qE 'restart: "?no"?'; then
    pass "$svc does not restart"
  else
    fail "$svc should declare restart: \"no\""
  fi
done

echo "== every long-running application service reports health =="
for svc in postgres minio elasticsearch background idp socket url-shortener; do
  block="$(service_block "$svc" "$rendered_all")"
  if printf '%s\n' "$block" | grep -q 'healthcheck:'; then
    pass "$svc declares a healthcheck"
  else
    fail "$svc has no healthcheck"
  fi
done

echo "== the deployer plans without credentials, an image source, or side effects =="
DEPLOY="$REPO_ROOT/scripts/deploy-compose.sh"
if [ ! -x "$DEPLOY" ]; then
  fail "scripts/deploy-compose.sh is missing or not executable"
else
  sandbox="$(mktemp -d)"
  if "$DEPLOY" --print-plan --env-out "$sandbox/.env" --domain verify.example.com \
      </dev/null >"$sandbox/plan.out" 2>&1; then
    pass "--print-plan succeeds with no image source and no credentials"
  else
    cat "$sandbox/plan.out" >&2
    fail "--print-plan failed"
  fi
  if "$DEPLOY" --uninstall --print-plan --env-out "$sandbox/.env" \
      </dev/null >"$sandbox/uninstall.out" 2>&1; then
    pass "--uninstall --print-plan succeeds"
  else
    cat "$sandbox/uninstall.out" >&2
    fail "--uninstall --print-plan failed"
  fi
  if [ -e "$sandbox/.env" ]; then
    fail "--print-plan wrote an .env file"
  else
    pass "--print-plan left no .env behind"
  fi
  if grep -qiE '(password|secret|key)=[A-Za-z0-9]{8,}' "$sandbox/plan.out"; then
    fail "--print-plan printed an unredacted credential"
  else
    pass "--print-plan redacts credentials"
  fi
  # CIDR must be rejected: the IdP compares with exact string equality.
  if "$DEPLOY" --print-plan --non-interactive --domain verify.example.com \
      --root-access-ip 192.168.0.0/24 --env-out "$sandbox/.env" \
      </dev/null >"$sandbox/cidr.out" 2>&1; then
    fail "the deployer accepted a CIDR range for --root-access-ip"
  else
    pass "the deployer rejects CIDR for --root-access-ip"
  fi
  help_out="$("$DEPLOY" --help 2>&1)"
  for flag in --domain --registry --tag --tls-mode --services --liveness \
              --root-access-ip --minio-public-url --external-postgres \
              --external-minio --external-elasticsearch --print-plan \
              --uninstall --env-out --overwrite --non-interactive --yes; do
    if printf '%s\n' "$help_out" | grep -q -- "$flag"; then :; else
      fail "--help does not document $flag"
    fi
  done
  pass "--help documents every documented flag"
  rm -rf "$sandbox"
fi

echo
if [ "$FAILURES" -eq 0 ]; then
  echo "All compose config assertions passed."
  exit 0
fi
echo "$FAILURES assertion(s) failed." >&2
exit 1
