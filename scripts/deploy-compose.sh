#!/usr/bin/env bash
# Guided deployer for ezto Verify on a single host with Docker Compose.
# Standalone by design; helpers are duplicated from scripts/install.sh.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
COMPOSE_FILE="$REPO_ROOT/docker-compose.yml"

DOMAIN=""
REGISTRY=""
IMAGE_PREFIX=""
IMAGE_REPO=""
TAG="latest"
IDP_IMAGE=""
BACKGROUND_IMAGE=""
SOCKET_IMAGE=""
URL_SHORTENER_IMAGE=""
LIVENESS_IMAGE=""
TLS_MODE="self-signed"
SERVICES="background,socket,url-shortener"
LIVENESS=false
BUNDLE_POSTGRES=true
BUNDLE_MINIO=true
BUNDLE_ELASTICSEARCH=true
POSTGRES_HOST="postgres"
POSTGRES_PORT="5432"
MINIO_SERVER_URL=""
MINIO_PUBLIC_URL=""
MINIO_BUCKET="ezto-verify"
ES_HOST="elasticsearch"
ES_PORT="9200"
ES_SCHEME="http"
ROOT_ACCESS_IP="127.0.0.1,::1"
DOCS_URL=""
DOCS_LINK=""
ADMIN_USER="admin"
ADMIN_PASSWORD_FILE=""
POSTGRES_USER="ezto"
POSTGRES_PASSWORD_FILE=""
MINIO_USER="ezto"
MINIO_PASSWORD_FILE=""
SHORTENER_KEY_FILE=""
ENV_OUT=""
YES=false
NON_INTERACTIVE=false
PRINT_PLAN=false
UNINSTALL=false
OVERWRITE=false
# Elasticsearch 7 refuses to start below this.
VM_MAX_MAP_COUNT_MIN=262144

usage() {
  cat <<'USAGE'
Usage: scripts/deploy-compose.sh [options]

Deploys ezto Verify onto this host with Docker Compose. Service images are
pulled; nothing is built. The script generates a mode-600 .env holding every
credential and then runs `docker compose up -d`.

An image source is required. Three naming shapes are supported:
  --registry eztovrfy --tag 1.0.0          -> eztovrfy/idp:1.0.0
  --registry myorg --image-prefix ezto-    -> myorg/ezto-idp:1.0.0   (chart naming)
  --image-repo myorg/ezto-oss              -> myorg/ezto-oss:idp     (service as tag)
An explicit --<service>-image always wins.

Options:
  --domain NAME                  Public domain served over HTTPS (required)
  --registry PREFIX              Registry prefix; images become PREFIX/<service>:TAG
  --image-prefix STR             Inserted before each service name (chart uses "ezto-")
  --image-repo REPO              One repository whose tag is the service name
  --tag TAG                      Image tag used with --registry (default: latest)
  --idp-image REF                Explicit idp image (wins over any derivation)
  --background-image REF         Explicit background image
  --socket-image REF             Explicit socket image
  --url-shortener-image REF      Explicit URL-shortener image
  --liveness-image REF           Explicit liveness image
  --tls-mode MODE                self-signed | files (default: self-signed)
  --services CSV                 background,socket,url-shortener (default: all)
  --liveness                     Enable face liveness (~15 GB image)
  --external-postgres HOST:PORT  Use your own PostgreSQL instead of the bundled one
  --external-minio URL           Use your own S3/MinIO endpoint
  --external-elasticsearch H:P   Use your own Elasticsearch 7.x endpoint
  --minio-public-url URL         Browser-facing URL for public objects
  --minio-bucket NAME            Bucket name (default: ezto-verify)
  --root-access-ip CSV           Exact onboarding IPs, no CIDR
  --docs-url URL                 Admin-console Docs link target
  --docs-link URL                Email / welcome-page Docs link target
  --admin-user USER              Bootstrap admin user (default: admin)
  --admin-password-file FILE     Read the admin password from FILE
  --postgres-user USER           PostgreSQL user (default: ezto)
  --postgres-password-file FILE  Read the PostgreSQL password from FILE
  --minio-user USER              MinIO root user (default: ezto)
  --minio-password-file FILE     Read the MinIO password from FILE
  --shortener-key-file FILE      Read the shared shortener key from FILE
  --env-out FILE                 Generated .env path (default: ./.env)
  --overwrite                    Replace an existing generated .env
  --non-interactive              Never prompt
  --yes                          Confirm deploy/uninstall
  --print-plan                   Print a redacted plan; change nothing
  --uninstall                    Stop the stack; volumes are retained
  -h, --help                     Show this help

Operations, upgrade, rollback, and restore: docs/deployment-compose.md
USAGE
}

need_arg() {
  [ "$#" -ge 2 ] || { echo "error: $1 requires a value" >&2; exit 2; }
}

while [ "$#" -gt 0 ]; do
  case "$1" in
    --domain) need_arg "$@"; DOMAIN="$2"; shift 2 ;;
    --registry) need_arg "$@"; REGISTRY="$2"; shift 2 ;;
    --image-prefix) need_arg "$@"; IMAGE_PREFIX="$2"; shift 2 ;;
    --image-repo) need_arg "$@"; IMAGE_REPO="$2"; shift 2 ;;
    --idp-image) need_arg "$@"; IDP_IMAGE="$2"; shift 2 ;;
    --background-image) need_arg "$@"; BACKGROUND_IMAGE="$2"; shift 2 ;;
    --socket-image) need_arg "$@"; SOCKET_IMAGE="$2"; shift 2 ;;
    --url-shortener-image) need_arg "$@"; URL_SHORTENER_IMAGE="$2"; shift 2 ;;
    --liveness-image) need_arg "$@"; LIVENESS_IMAGE="$2"; shift 2 ;;
    --tag) need_arg "$@"; TAG="$2"; shift 2 ;;
    --tls-mode) need_arg "$@"; TLS_MODE="$2"; shift 2 ;;
    --services) need_arg "$@"; SERVICES="$2"; shift 2 ;;
    --liveness) LIVENESS=true; shift ;;
    --external-postgres) need_arg "$@"; BUNDLE_POSTGRES=false; POSTGRES_HOST="${2%%:*}"; POSTGRES_PORT="${2##*:}"; shift 2 ;;
    --external-minio) need_arg "$@"; BUNDLE_MINIO=false; MINIO_SERVER_URL="$2"; shift 2 ;;
    --external-elasticsearch) need_arg "$@"; BUNDLE_ELASTICSEARCH=false; ES_HOST="${2%%:*}"; ES_PORT="${2##*:}"; shift 2 ;;
    --minio-public-url) need_arg "$@"; MINIO_PUBLIC_URL="$2"; shift 2 ;;
    --minio-bucket) need_arg "$@"; MINIO_BUCKET="$2"; shift 2 ;;
    --root-access-ip) need_arg "$@"; ROOT_ACCESS_IP="$2"; shift 2 ;;
    --docs-url) need_arg "$@"; DOCS_URL="$2"; shift 2 ;;
    --docs-link) need_arg "$@"; DOCS_LINK="$2"; shift 2 ;;
    --admin-user) need_arg "$@"; ADMIN_USER="$2"; shift 2 ;;
    --admin-password-file) need_arg "$@"; ADMIN_PASSWORD_FILE="$2"; shift 2 ;;
    --postgres-user) need_arg "$@"; POSTGRES_USER="$2"; shift 2 ;;
    --postgres-password-file) need_arg "$@"; POSTGRES_PASSWORD_FILE="$2"; shift 2 ;;
    --minio-user) need_arg "$@"; MINIO_USER="$2"; shift 2 ;;
    --minio-password-file) need_arg "$@"; MINIO_PASSWORD_FILE="$2"; shift 2 ;;
    --shortener-key-file) need_arg "$@"; SHORTENER_KEY_FILE="$2"; shift 2 ;;
    --env-out) need_arg "$@"; ENV_OUT="$2"; shift 2 ;;
    --overwrite) OVERWRITE=true; shift ;;
    --non-interactive) NON_INTERACTIVE=true; shift ;;
    --yes) YES=true; shift ;;
    --print-plan) PRINT_PLAN=true; shift ;;
    --uninstall) UNINSTALL=true; shift ;;
    -h|--help) usage; exit 0 ;;
    *) echo "error: unknown option '$1'" >&2; usage >&2; exit 2 ;;
  esac
done

[ -n "$ENV_OUT" ] || ENV_OUT="$REPO_ROOT/.env"
if [ ! -t 0 ]; then NON_INTERACTIVE=true; fi

# --env-out names a file this script fills with credentials. Refuse a tracked
# path: '--env-out README.md --overwrite' would put them in the working tree.
if git -C "$REPO_ROOT" ls-files --error-unmatch "$ENV_OUT" >/dev/null 2>&1; then
  echo "error: $ENV_OUT is tracked by git; refusing to write credentials into it" >&2
  exit 2
fi
case "$(basename "$ENV_OUT")" in
  .env|.env.*|*.env) : ;;
  *) echo "error: --env-out must name a .env file (got '$(basename "$ENV_OUT")')" >&2; exit 2 ;;
esac

prompt_value() {
  label="$1"; default="$2"; reply=""
  printf '%s [%s]: ' "$label" "$default" >&2
  IFS= read -r reply
  printf '%s' "${reply:-$default}"
}

prompt_yes_no() {
  label="$1"; default="$2"; reply=""
  if [ "$default" = true ]; then suffix="Y/n"; else suffix="y/N"; fi
  printf '%s [%s]: ' "$label" "$suffix" >&2
  IFS= read -r reply
  case "$reply" in
    y|Y|yes|YES) return 0 ;;
    n|N|no|NO) return 1 ;;
    '') [ "$default" = true ] ;;
    *) echo "Please answer yes or no." >&2; prompt_yes_no "$label" "$default" ;;
  esac
}

csv_has() {
  case ",${SERVICES}," in *",$1,"*) return 0 ;; *) return 1 ;; esac
}

csv_drop() {
  SERVICES="$(printf '%s' "$SERVICES" | awk -v RS=, -v ORS=, -v skip="$1" '$0 != skip && $0 != "" {print}' | sed 's/,$//')"
}

# Explicit reference wins, then a single repo carrying service-name tags, then
# a registry prefix. Prints nothing when no source was given.
derive_image() {
  service="$1"; explicit="$2"
  if [ -n "$explicit" ]; then printf '%s' "$explicit"; return 0; fi
  if [ -n "$IMAGE_REPO" ]; then printf '%s:%s' "${IMAGE_REPO%/}" "$service"; return 0; fi
  if [ -n "$REGISTRY" ]; then printf '%s/%s%s:%s' "${REGISTRY%/}" "$IMAGE_PREFIX" "$service" "$TAG"; return 0; fi
  printf ''
}

image_plan() {
  printf 'idp=%s' "${IDP_IMAGE:-<none>}"
  csv_has background && printf ' background=%s' "${BACKGROUND_IMAGE:-<none>}"
  csv_has socket && printf ' socket=%s' "${SOCKET_IMAGE:-<none>}"
  csv_has url-shortener && printf ' url-shortener=%s' "${URL_SHORTENER_IMAGE:-<none>}"
  [ "$LIVENESS" = true ] && printf ' liveness=%s' "${LIVENESS_IMAGE:-<none>}"
  printf '\n'
}

random_secret() {
  set +o pipefail
  # Alphanumeric only: `$` would be re-interpolated by Compose.
  secret="$(LC_ALL=C tr -dc 'A-Za-z0-9' </dev/urandom | head -c 24)"
  set -o pipefail
  printf '%s' "$secret"
}

read_secret() {
  file="$1"
  if [ -n "$file" ]; then
    [ -f "$file" ] || { echo "error: secret file not found: $file" >&2; exit 2; }
    value="$(tr -d '\r\n' < "$file")"
    [ -n "$value" ] || { echo "error: secret file is empty: $file" >&2; exit 2; }
    # Compose treats ' #' as an inline comment and re-interpolates '$', so an
    # accepted-but-mangled secret would initialise a store with one value while
    # the application reads another.
    case "$value" in
      *'$'*)  echo "error: secret in $file contains '\$'; Compose would re-interpolate it" >&2; exit 2 ;;
      *'#'*)  echo "error: secret in $file contains '#'; Compose would treat it as a comment" >&2; exit 2 ;;
      *[[:space:]]*) echo "error: secret in $file contains whitespace; Compose would truncate it" >&2; exit 2 ;;
      *\'*|*'"'*|*'\\'*) echo "error: secret in $file contains a quote or backslash; use [A-Za-z0-9._-]" >&2; exit 2 ;;
    esac
    printf '%s' "$value"
  else
    random_secret
  fi
}

# --- Uninstall: needs no image source and no credentials --------------------
if [ "$UNINSTALL" = true ]; then
  if [ "$PRINT_PLAN" = true ]; then
    echo "Would run: docker compose -f $COMPOSE_FILE down"
    echo "Volumes (pgdata, miniodata, esdata), $ENV_OUT, and TLS certificates would be retained."
    exit 0
  fi
  command -v docker >/dev/null 2>&1 || { echo "error: docker is required" >&2; exit 3; }
  if [ "$YES" = false ]; then
    if [ "$NON_INTERACTIVE" = true ] || ! prompt_yes_no "Stop the stack? Volumes and .env are retained" false; then
      echo "Cancelled; no changes were made."
      exit 5
    fi
  fi
  ( cd "$REPO_ROOT" && docker compose --env-file "$ENV_OUT" -f "$COMPOSE_FILE" down )
  echo "Stack stopped. Volumes, .env, and certificates were intentionally retained."
  echo "To delete the data as well, run: docker compose down -v  (irreversible)"
  exit 0
fi

# --- Interactive intake ------------------------------------------------------
if [ "$NON_INTERACTIVE" = false ]; then
  DOMAIN="$(prompt_value "Public domain (HTTPS)" "$DOMAIN")"
  if [ -z "$REGISTRY" ]; then
    echo "Service images are pulled, never built. Give the registry prefix; images" >&2
    echo "become PREFIX/<service>:TAG (idp, background, socket, url-shortener)." >&2
  fi
  REGISTRY="$(prompt_value "Image registry prefix (e.g. eztovrfy)" "$REGISTRY")"
  TAG="$(prompt_value "Image tag" "$TAG")"
  TLS_MODE="$(prompt_value "TLS mode (self-signed|files)" "$TLS_MODE")"
  csv_has background    || true
  prompt_yes_no "Run background jobs?"      "$(csv_has background    && echo true || echo false)" || csv_drop background
  prompt_yes_no "Run the websocket service?" "$(csv_has socket        && echo true || echo false)" || csv_drop socket
  prompt_yes_no "Run the URL shortener?"     "$(csv_has url-shortener && echo true || echo false)" || csv_drop url-shortener
  echo "Face liveness uses an image of roughly 15 GB and is skipped by default." >&2
  if prompt_yes_no "Enable face liveness?" false; then LIVENESS=true; fi
  echo "Master-tenant onboarding is restricted by exact client IP. Behind nginx this" >&2
  echo "is the real address of the browser doing the onboarding, not this host." >&2
  ROOT_ACCESS_IP="$(prompt_value "Onboarding IPs (comma-separated, no CIDR)" "$ROOT_ACCESS_IP")"
fi

# --- Validation --------------------------------------------------------------
[ -n "$DOMAIN" ] || { echo "error: --domain is required" >&2; exit 2; }
case "$TLS_MODE" in
  self-signed|files) : ;;
  *) echo "error: --tls-mode must be 'self-signed' or 'files'" >&2; exit 2 ;;
esac
# Exact string equality in the IdP: a CIDR range allows nobody.
case "$ROOT_ACCESS_IP" in
  '') echo "error: --root-access-ip must not be empty; the IdP returns HTTP 500 without it" >&2; exit 2 ;;
  */*) echo "error: --root-access-ip takes exact comma-separated IPs; CIDR is unsupported" >&2; exit 2 ;;
  *[!0-9a-fA-F:.,]*) echo "error: --root-access-ip contains a non-IP character" >&2; exit 2 ;;
esac
for service in $(printf '%s' "$SERVICES" | tr ',' ' '); do
  case "$service" in
    background|socket|url-shortener) : ;;
    *) echo "error: unsupported service '$service'" >&2; exit 2 ;;
  esac
done

[ -n "$MINIO_SERVER_URL" ] || MINIO_SERVER_URL="http://minio:9000"
if [ -z "$MINIO_PUBLIC_URL" ]; then
  # A URL derived from DOMAIN cannot reach an external endpoint.
  if [ "$BUNDLE_MINIO" = false ]; then
    echo "error: --external-minio requires --minio-public-url; a browser-facing URL" >&2
    echo "       cannot be derived from the domain when object storage is external." >&2
    exit 2
  fi
  MINIO_PUBLIC_URL="https://${DOMAIN}/storage/${MINIO_BUCKET}/public"
fi

# --- Profiles and the URLs the IdP advertises -------------------------------
PROFILES=""
add_profile() { PROFILES="${PROFILES:+$PROFILES,}$1"; }
[ "$BUNDLE_POSTGRES" = true ] && add_profile infra-postgres
[ "$BUNDLE_MINIO" = true ] && add_profile infra-minio
[ "$BUNDLE_ELASTICSEARCH" = true ] && add_profile infra-elasticsearch
csv_has background && add_profile background
csv_has socket && add_profile socket
csv_has url-shortener && add_profile url-shortener
[ "$LIVENESS" = true ] && add_profile face-biometrics

# Blank URL for an absent service, or the IdP advertises it.
BACKGROUND_JOB_URL=""; SOCKET_URL=""; SHORTENER_HTTP_URL=""; SHORTENER_HTTPS_URL=""; LIVELINESS_URL=""
csv_has background && BACKGROUND_JOB_URL="http://background:8070"
csv_has socket && SOCKET_URL="http://socket:3001"
if csv_has url-shortener; then
  SHORTENER_HTTP_URL="http://url-shortener:3000"
  SHORTENER_HTTPS_URL="http://url-shortener:3000"
fi
[ "$LIVENESS" = true ] && LIVELINESS_URL="http://liveness:5000"

IDP_IMAGE="$(derive_image idp "$IDP_IMAGE")"
BACKGROUND_IMAGE="$(derive_image background "$BACKGROUND_IMAGE")"
SOCKET_IMAGE="$(derive_image socket "$SOCKET_IMAGE")"
URL_SHORTENER_IMAGE="$(derive_image url-shortener "$URL_SHORTENER_IMAGE")"
LIVENESS_IMAGE="$(derive_image liveness "$LIVENESS_IMAGE")"

# --- Plan --------------------------------------------------------------------
if [ "$PRINT_PLAN" = true ]; then
  cat <<EOF
Domain:         $DOMAIN
Images:         $(image_plan)TLS mode:       $TLS_MODE
Services:       idp${SERVICES:+,$SERVICES}
Liveness:       $LIVENESS
Profiles:       $PROFILES
PostgreSQL:     $( [ "$BUNDLE_POSTGRES" = true ] && echo "bundled" || echo "external at $POSTGRES_HOST:$POSTGRES_PORT" )
MinIO:          $( [ "$BUNDLE_MINIO" = true ] && echo "bundled" || echo "external at $MINIO_SERVER_URL" )
Elasticsearch:  $( [ "$BUNDLE_ELASTICSEARCH" = true ] && echo "bundled" || echo "external at $ES_HOST:$ES_PORT" )
Public objects: $MINIO_PUBLIC_URL
Onboarding IPs: $ROOT_ACCESS_IP
Env file:       $ENV_OUT (would be generated at mode 600; credentials are not shown here)
Published:      nginx 80 and 443 only
Command:        docker compose --env-file $ENV_OUT -f $COMPOSE_FILE up -d
EOF
  exit 0
fi

# --- Host preconditions ------------------------------------------------------
command -v docker >/dev/null 2>&1 || { echo "error: docker is required" >&2; exit 3; }
COMPOSE_VERSION="$(docker compose version --short 2>/dev/null || true)"
[ -n "$COMPOSE_VERSION" ] || { echo "error: the Docker Compose plugin is required" >&2; exit 3; }
COMPOSE_VERSION="${COMPOSE_VERSION#v}"
# `depends_on: required: false` was introduced in Compose 2.20.0.
if [ "$(printf '%s\n2.20.0\n' "$COMPOSE_VERSION" | sort -V | head -n1)" != "2.20.0" ]; then
  echo "error: Docker Compose >= 2.20.0 is required (found $COMPOSE_VERSION)." >&2
  echo "       Optional-dependency handling ('required: false') needs it." >&2
  exit 3
fi

if [ -z "$IDP_IMAGE" ]; then
  echo "error: no image source was given; pass --registry, --image-repo, or --idp-image" >&2
  exit 2
fi
csv_has background    && [ -z "$BACKGROUND_IMAGE" ]    && { echo "error: background is enabled but has no image" >&2; exit 2; }
csv_has socket        && [ -z "$SOCKET_IMAGE" ]        && { echo "error: socket is enabled but has no image" >&2; exit 2; }
csv_has url-shortener && [ -z "$URL_SHORTENER_IMAGE" ] && { echo "error: the URL shortener is enabled but has no image" >&2; exit 2; }
[ "$LIVENESS" = true ] && [ -z "$LIVENESS_IMAGE" ] && { echo "error: liveness is enabled but has no image" >&2; exit 2; }
true

# Only meaningful when the daemon shares this kernel; Docker Desktop runs a VM.
if [ "$BUNDLE_ELASTICSEARCH" = true ] && [ "$(uname -s)" = Linux ]; then
  CURRENT_MAX_MAP="$(sysctl -n vm.max_map_count 2>/dev/null || echo 0)"
  case "$CURRENT_MAX_MAP" in ''|*[!0-9]*) CURRENT_MAX_MAP=0 ;; esac
  if [ "$CURRENT_MAX_MAP" -lt "$VM_MAX_MAP_COUNT_MIN" ]; then
    echo "error: the bundled Elasticsearch needs vm.max_map_count >= $VM_MAX_MAP_COUNT_MIN (found $CURRENT_MAX_MAP)." >&2
    echo "       Set it now:  sudo sysctl -w vm.max_map_count=$VM_MAX_MAP_COUNT_MIN" >&2
    echo "       Persist it:  echo 'vm.max_map_count=$VM_MAX_MAP_COUNT_MIN' | sudo tee /etc/sysctl.d/99-ezto-verify.conf" >&2
    echo "       Or use your own cluster with --external-elasticsearch HOST:PORT." >&2
    exit 3
  fi
elif [ "$BUNDLE_ELASTICSEARCH" = true ]; then
  echo "note: vm.max_map_count was not checked ($(uname -s) host; the Docker VM owns it)." >&2
  echo "      If Elasticsearch crash-loops, raise it inside the Docker VM to $VM_MAX_MAP_COUNT_MIN." >&2
fi

# --- First install vs. adoption: existing volumes hold old credentials -------
# Compose gives COMPOSE_PROJECT_NAME precedence over the directory name, so a
# stack deployed with it set owns volumes this script would otherwise miss --
# and would then generate new credentials against the old data.
PROJECT_NAME="${COMPOSE_PROJECT_NAME:-$(basename "$REPO_ROOT")}"
PROJECT_NAME="$(printf '%s' "$PROJECT_NAME" | tr '[:upper:]' '[:lower:]' | tr -c 'a-z0-9_-' '_')"
VOLUMES_EXIST=false
for vol in pgdata miniodata esdata; do
  if docker volume inspect "${PROJECT_NAME}_${vol}" >/dev/null 2>&1; then
    VOLUMES_EXIST=true
  fi
done

if [ -e "$ENV_OUT" ] && [ "$OVERWRITE" = false ]; then
  echo "error: $ENV_OUT already exists (use --overwrite, or --env-out to choose another path)" >&2
  exit 2
fi

# Past this point credentials are (re)generated, whether because there is no
# .env or because --overwrite was given. Either way, generating fresh ones
# against populated volumes leaves the stack unable to open its own data.
CREDS_SUPPLIED=false
if [ -n "$POSTGRES_PASSWORD_FILE" ] && [ -n "$MINIO_PASSWORD_FILE" ]; then
  CREDS_SUPPLIED=true
fi

if [ "$VOLUMES_EXIST" = true ] && [ "$CREDS_SUPPLIED" = false ]; then
  echo "error: this host already holds ezto Verify data volumes (project '$PROJECT_NAME')." >&2
  echo "       They were written under the previous credentials, so generating new ones" >&2
  echo "       would leave the stack unable to open its own data." >&2
  echo "       Supply the existing values:" >&2
  echo "         --postgres-password-file FILE" >&2
  echo "         --minio-password-file FILE" >&2
  echo "         --admin-password-file FILE" >&2
  echo "         --shortener-key-file FILE" >&2
  echo "       A stack from an older revision used the literals admin/admin and" >&2
  echo "       minioadmin/minioadmin123; see docs/deployment-compose.md" >&2
  echo "       'Adopting an existing stack'." >&2
  exit 4
fi
if [ "$VOLUMES_EXIST" = true ]; then
  echo "Adopting the existing data volumes with the credentials supplied." >&2
fi

# --- Credentials -------------------------------------------------------------
ADMIN_PASSWORD="$(read_secret "$ADMIN_PASSWORD_FILE")"
POSTGRES_PASSWORD="$(read_secret "$POSTGRES_PASSWORD_FILE")"
MINIO_PASSWORD="$(read_secret "$MINIO_PASSWORD_FILE")"
SHORTENER_KEY="$(read_secret "$SHORTENER_KEY_FILE")"

ENV_DIR="$(dirname "$ENV_OUT")"
[ -d "$ENV_DIR" ] || { mkdir -p "$ENV_DIR"; chmod 700 "$ENV_DIR"; }
umask 077
cat > "$ENV_OUT" <<EOF
# Generated by scripts/deploy-compose.sh on $(date -u '+%Y-%m-%dT%H:%M:%SZ').
# Mode 600. Holds every credential the stack uses — never commit it.
DOMAIN=$DOMAIN
IDP_IMAGE=$IDP_IMAGE
BACKGROUND_IMAGE=$BACKGROUND_IMAGE
SOCKET_IMAGE=$SOCKET_IMAGE
URL_SHORTENER_IMAGE=$URL_SHORTENER_IMAGE
LIVENESS_IMAGE=$LIVENESS_IMAGE
REGISTRY=$REGISTRY
IMAGE_PREFIX=$IMAGE_PREFIX
TAG=$TAG
TLS_MODE=$TLS_MODE
POSTGRES_USER=$POSTGRES_USER
POSTGRES_PASSWORD=$POSTGRES_PASSWORD
MINIO_ROOT_USER=$MINIO_USER
MINIO_ROOT_PASSWORD=$MINIO_PASSWORD
KEYCLOAK_ADMIN=$ADMIN_USER
KEYCLOAK_ADMIN_PASSWORD=$ADMIN_PASSWORD
SHORTENER_KEY=$SHORTENER_KEY
POSTGRES_HOST=$POSTGRES_HOST
POSTGRES_PORT=$POSTGRES_PORT
MINIO_SERVER_URL=$MINIO_SERVER_URL
MINIO_PUBLIC_URL=$MINIO_PUBLIC_URL
MINIO_BUCKET=$MINIO_BUCKET
ES_HOST=$ES_HOST
ES_PORT=$ES_PORT
ES_SCHEME=$ES_SCHEME
ROOT_ACCESS_IP=$ROOT_ACCESS_IP
DOCS_URL=$DOCS_URL
DOCS_LINK=$DOCS_LINK
BACKGROUND_JOB_URL=$BACKGROUND_JOB_URL
SOCKET_URL=$SOCKET_URL
SHORTENER_HTTP_URL=$SHORTENER_HTTP_URL
SHORTENER_HTTPS_URL=$SHORTENER_HTTPS_URL
LIVELINESS_URL=$LIVELINESS_URL
COMPOSE_PROFILES=$PROFILES
EOF
chmod 600 "$ENV_OUT"
echo "Wrote $ENV_OUT (mode 600)."

# --- External stores: init-databases.sql never runs against an external server
if [ "$BUNDLE_POSTGRES" = false ]; then
  echo "External PostgreSQL selected. These databases must already exist and be owned by '$POSTGRES_USER':" >&2
  echo "  eztoverify  jobrnr  urlshortner  socket" >&2
  echo "  (the 'urlshortner' spelling is intentional; it matches init-databases.sql)" >&2
  if command -v psql >/dev/null 2>&1; then
    for db in eztoverify jobrnr urlshortner socket; do
      if ! PGPASSWORD="$POSTGRES_PASSWORD" psql -h "$POSTGRES_HOST" -p "$POSTGRES_PORT" \
           -U "$POSTGRES_USER" -d "$db" -c 'SELECT 1' >/dev/null 2>&1; then
        echo "error: cannot open database '$db' on $POSTGRES_HOST:$POSTGRES_PORT as '$POSTGRES_USER'." >&2
        echo "       Create it before deploying:  CREATE DATABASE \"$db\";" >&2
        exit 4
      fi
    done
    echo "All four databases are reachable." >&2
  else
    echo "warning: psql is not installed, so the databases could not be verified here." >&2
  fi
fi

if [ "$BUNDLE_MINIO" = false ]; then
  echo "External object storage selected. Create the bucket '$MINIO_BUCKET' yourself and" >&2
  echo "grant anonymous download on its 'public' prefix; minio-init does not run." >&2
fi

# --- TLS ---------------------------------------------------------------------
CERT="$REPO_ROOT/nginx/certs/fullchain.pem"
KEY="$REPO_ROOT/nginx/certs/privkey.pem"
if [ "$TLS_MODE" = self-signed ]; then
  if [ ! -s "$CERT" ] || [ ! -s "$KEY" ]; then
    echo "Generating a self-signed certificate for $DOMAIN; browsers will warn." >&2
    DOMAIN="$DOMAIN" "$REPO_ROOT/nginx/gen-self-signed.sh"
  else
    echo "Reusing the existing certificate in nginx/certs." >&2
  fi
else
  if [ ! -s "$CERT" ] || [ ! -s "$KEY" ]; then
    echo "error: --tls-mode files needs nginx/certs/fullchain.pem and nginx/certs/privkey.pem." >&2
    exit 3
  fi
fi

# --- Deploy ------------------------------------------------------------------
if [ "$YES" = false ]; then
  if [ "$NON_INTERACTIVE" = true ] || ! prompt_yes_no "Deploy ezto Verify $TAG to $DOMAIN?" true; then
    echo "Cancelled. $ENV_OUT was written and can be reused."
    exit 5
  fi
fi

cd "$REPO_ROOT"
docker compose --env-file "$ENV_OUT" -f "$COMPOSE_FILE" pull
docker compose --env-file "$ENV_OUT" -f "$COMPOSE_FILE" up -d

cat <<EOF

Deployed. The IdP is at https://$DOMAIN/auth
Admin console: https://$DOMAIN/auth/admin/  (user: $ADMIN_USER)
The admin password is in $ENV_OUT as KEYCLOAK_ADMIN_PASSWORD.

Master-tenant onboarding is restricted to: $ROOT_ACCESS_IP
If your browser reaches this host from another address, onboarding is rejected.
Read the address nginx recorded, add it to ROOT_ACCESS_IP in $ENV_OUT, then run
'docker compose up -d idp':

  docker compose logs nginx | tail -5

Verify nothing but nginx is published:

  docker compose ps --format 'table {{.Service}}\t{{.Ports}}'

Operations, upgrade, rollback, and restore: docs/deployment-compose.md
EOF
