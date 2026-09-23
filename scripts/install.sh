#!/usr/bin/env bash
# Interactive installer for ezto Verify on an existing Kubernetes cluster.
# It renders one auditable values file and delegates deployment to Helm.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
CHART="$REPO_ROOT/charts/ezto-verify"

CONTEXT=""
NAMESPACE="ezto-verify"
RELEASE="ezto-verify"
SERVICES="background,socket,url-shortener"
LIVENESS=false
MAILDEV=false
STORAGE_CLASS=""
ROOT_ACCESS_IP="127.0.0.1,::1"
VALUES_OUT="$REPO_ROOT/.ezto-install/values.yaml"
TIMEOUT="15m"
YES=false
NON_INTERACTIVE=false
PRINT_PLAN=false
DRY_RUN=false
UNINSTALL=false
OVERWRITE=false
IMAGE_PULL_SECRET=""
ADMIN_USER="admin"
ADMIN_PASSWORD_FILE=""
POSTGRES_PASSWORD_FILE=""
MINIO_PASSWORD_FILE=""
SHORTENER_KEY_FILE=""
REGISTRY=""
IMAGE_REPO=""
IMAGE_TAG="latest"
IDP_IMAGE=""
BACKGROUND_IMAGE=""
SOCKET_IMAGE=""
URL_SHORTENER_IMAGE=""
LIVENESS_IMAGE=""

usage() {
  cat <<'USAGE'
Usage: scripts/install.sh [options]

Installs ezto Verify into an existing Kubernetes context. The chart ships no
default image registry, so an image source is required: --registry for one
repository per service, --image-repo for a single repository with per-service
tags, or an explicit --<service>-image. Interactive mode prompts for both the
image source and service selection; liveness defaults to off.

Options:
  --context NAME                 Kubernetes context (required non-interactively)
  --namespace NAME               Namespace (default: ezto-verify)
  --release NAME                 Helm release (default: ezto-verify)
  --services CSV                 background,socket,url-shortener (default: all)
  --registry PREFIX              Registry prefix; images become PREFIX/ezto-<service>:TAG
  --image-repo REPO              Single repository; the service name is the tag
  --image-tag TAG                Tag used with --registry (default: latest)
  --idp-image REPO:TAG           Explicit idp image
  --background-image REPO:TAG    Explicit background image
  --socket-image REPO:TAG        Explicit socket image
  --url-shortener-image REPO:TAG Explicit URL-shortener image
  --liveness                     Enable liveness (needs a resolvable image)
  --liveness-image REPO:TAG      Explicit liveness image
  --maildev                      Enable MailDev
  --storage-class NAME           StorageClass for bundled data services
  --root-access-ip CSV           Exact onboarding IPs, no CIDR
  --image-pull-secret NAME       Existing Kubernetes image-pull Secret
  --admin-user USER              Bootstrap admin user (default: admin)
  --admin-password-file FILE     Read admin password from FILE
  --postgres-password-file FILE  Read PostgreSQL password from FILE
  --minio-password-file FILE     Read MinIO password from FILE
  --shortener-key-file FILE      Read shared shortener key from FILE
  --values-out FILE              Generated values path
  --timeout DURATION             Helm timeout (default: 15m)
  --non-interactive              Never prompt
  --yes                          Confirm apply/uninstall
  --overwrite                    Replace an existing generated values file
  --print-plan                   Print a redacted plan; perform no external calls
  --dry-run                      Render with Helm but do not touch the cluster
  --uninstall                    Remove only the named Helm release
  -h, --help                     Show this help
USAGE
}

need_arg() {
  [ "$#" -ge 2 ] || { echo "error: $1 requires a value" >&2; exit 2; }
}

while [ "$#" -gt 0 ]; do
  case "$1" in
    --context) need_arg "$@"; CONTEXT="$2"; shift 2 ;;
    --namespace) need_arg "$@"; NAMESPACE="$2"; shift 2 ;;
    --release) need_arg "$@"; RELEASE="$2"; shift 2 ;;
    --services) need_arg "$@"; SERVICES="$2"; shift 2 ;;
    --registry) need_arg "$@"; REGISTRY="$2"; shift 2 ;;
    --image-repo) need_arg "$@"; IMAGE_REPO="$2"; shift 2 ;;
    --image-tag) need_arg "$@"; IMAGE_TAG="$2"; shift 2 ;;
    --idp-image) need_arg "$@"; IDP_IMAGE="$2"; shift 2 ;;
    --background-image) need_arg "$@"; BACKGROUND_IMAGE="$2"; shift 2 ;;
    --socket-image) need_arg "$@"; SOCKET_IMAGE="$2"; shift 2 ;;
    --url-shortener-image) need_arg "$@"; URL_SHORTENER_IMAGE="$2"; shift 2 ;;
    --liveness) LIVENESS=true; shift ;;
    --liveness-image) need_arg "$@"; LIVENESS_IMAGE="$2"; shift 2 ;;
    --maildev) MAILDEV=true; shift ;;
    --storage-class) need_arg "$@"; STORAGE_CLASS="$2"; shift 2 ;;
    --root-access-ip) need_arg "$@"; ROOT_ACCESS_IP="$2"; shift 2 ;;
    --image-pull-secret) need_arg "$@"; IMAGE_PULL_SECRET="$2"; shift 2 ;;
    --admin-user) need_arg "$@"; ADMIN_USER="$2"; shift 2 ;;
    --admin-password-file) need_arg "$@"; ADMIN_PASSWORD_FILE="$2"; shift 2 ;;
    --postgres-password-file) need_arg "$@"; POSTGRES_PASSWORD_FILE="$2"; shift 2 ;;
    --minio-password-file) need_arg "$@"; MINIO_PASSWORD_FILE="$2"; shift 2 ;;
    --shortener-key-file) need_arg "$@"; SHORTENER_KEY_FILE="$2"; shift 2 ;;
    --values-out) need_arg "$@"; VALUES_OUT="$2"; shift 2 ;;
    --timeout) need_arg "$@"; TIMEOUT="$2"; shift 2 ;;
    --non-interactive) NON_INTERACTIVE=true; shift ;;
    --yes) YES=true; shift ;;
    --overwrite) OVERWRITE=true; shift ;;
    --print-plan) PRINT_PLAN=true; shift ;;
    --dry-run) DRY_RUN=true; shift ;;
    --uninstall) UNINSTALL=true; shift ;;
    -h|--help) usage; exit 0 ;;
    *) echo "error: unknown option '$1'" >&2; usage >&2; exit 2 ;;
  esac
done

if [ ! -t 0 ]; then
  NON_INTERACTIVE=true
fi

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

# Resolve one service image: an explicit reference wins, then a single
# repository carrying per-service tags, then a registry prefix with one
# repository per service. Prints nothing when no source was given.
derive_image() {
  service="$1"; explicit="$2"
  if [ -n "$explicit" ]; then printf '%s' "$explicit"; return 0; fi
  if [ -n "$IMAGE_REPO" ]; then printf '%s:%s' "${IMAGE_REPO%/}" "$service"; return 0; fi
  if [ -n "$REGISTRY" ]; then printf '%s/ezto-%s:%s' "${REGISTRY%/}" "$service" "$IMAGE_TAG"; return 0; fi
  printf ''
}

set_service() {
  service="$1"; label="$2"; enabled=false
  if csv_has "$service"; then enabled=true; fi
  if prompt_yes_no "Enable $label?" "$enabled"; then
    case ",${SERVICES}," in *",$service,"*) : ;; *) SERVICES="${SERVICES:+$SERVICES,}$service" ;; esac
  else
    SERVICES="$(printf '%s' "$SERVICES" | awk -v RS=, -v ORS=, -v skip="$service" '$0 != skip && $0 != "" {print}' | sed 's/,$//')"
  fi
}

if [ "$NON_INTERACTIVE" = false ]; then
  if [ -z "$CONTEXT" ] && command -v kubectl >/dev/null 2>&1; then
    CONTEXT="$(kubectl config current-context 2>/dev/null || true)"
  fi
  CONTEXT="$(prompt_value "Kubernetes context" "$CONTEXT")"
  NAMESPACE="$(prompt_value "Namespace" "$NAMESPACE")"
  RELEASE="$(prompt_value "Helm release" "$RELEASE")"
fi

if [ "$NON_INTERACTIVE" = false ] && [ "$UNINSTALL" = false ]; then
  if [ -z "$IMAGE_REPO" ] && [ -z "$REGISTRY" ]; then
    echo "The chart ships no default image registry. Give the registry prefix holding" >&2
    echo "one repository per service (ezto-idp, ezto-background, ezto-socket," >&2
    echo "ezto-url-shortener, ezto-liveness), or re-run with --image-repo for a single" >&2
    echo "repository whose tags are the service names." >&2
    REGISTRY="$(prompt_value "Image registry prefix (e.g. docker.io/myorg)" "$REGISTRY")"
    IMAGE_TAG="$(prompt_value "Image tag" "$IMAGE_TAG")"
  fi
  set_service background "background jobs"
  set_service socket "websocket service"
  set_service url-shortener "URL shortener"
  echo "Face liveness uses an image of roughly 15 GB and is skipped by default." >&2
  if prompt_yes_no "Enable face liveness?" false; then
    LIVENESS=true
    LIVENESS_IMAGE="$(prompt_value "Liveness image" "$(derive_image liveness "$LIVENESS_IMAGE")")"
  else
    LIVENESS=false
  fi
  if prompt_yes_no "Enable MailDev SMTP catcher?" false; then MAILDEV=true; else MAILDEV=false; fi
  ROOT_ACCESS_IP="$(prompt_value "Master-onboarding exact IPs (no CIDR)" "$ROOT_ACCESS_IP")"
fi

[ -n "$CONTEXT" ] || { echo "error: --context is required" >&2; exit 2; }
case "$NAMESPACE" in ''|*[!a-z0-9-]*|-*|*-) echo "error: invalid namespace '$NAMESPACE'" >&2; exit 2 ;; esac
case "$RELEASE" in ''|*[!a-z0-9-]*|-*|*-) echo "error: invalid release '$RELEASE'" >&2; exit 2 ;; esac

# Removing a release needs a context, a namespace, and a release name -- not an
# image source and not credentials. Handle it before either is resolved.
if [ "$UNINSTALL" = true ]; then
  if [ "$PRINT_PLAN" = true ]; then
    echo "Would uninstall Helm release '$RELEASE' from namespace '$NAMESPACE' using context '$CONTEXT'."
    exit 0
  fi
  command -v helm >/dev/null 2>&1 || { echo "error: helm is required" >&2; exit 3; }
  command -v kubectl >/dev/null 2>&1 || { echo "error: kubectl is required" >&2; exit 3; }
  if [ "$YES" = false ]; then
    if [ "$NON_INTERACTIVE" = true ] || ! prompt_yes_no "Uninstall release '$RELEASE'? PVCs and namespace will remain" false; then
      echo "Cancelled; no changes were made."
      exit 5
    fi
  fi
  helm --kube-context "$CONTEXT" uninstall "$RELEASE" --namespace "$NAMESPACE"
  echo "Release removed. PVCs, namespace, and cluster were intentionally retained."
  exit 0
fi

case "$ROOT_ACCESS_IP" in ''|*/*|*[!0-9a-fA-F:.,]*) echo "error: root access IPs must be exact comma-separated IPs; CIDR is unsupported" >&2; exit 2 ;; esac

for service in $(printf '%s' "$SERVICES" | tr ',' ' '); do
  case "$service" in background|socket|url-shortener) : ;; *) echo "error: unsupported service '$service'" >&2; exit 2 ;; esac
done
IDP_IMAGE="$(derive_image idp "$IDP_IMAGE")"
BACKGROUND_IMAGE="$(derive_image background "$BACKGROUND_IMAGE")"
SOCKET_IMAGE="$(derive_image socket "$SOCKET_IMAGE")"
URL_SHORTENER_IMAGE="$(derive_image url-shortener "$URL_SHORTENER_IMAGE")"
LIVENESS_IMAGE="$(derive_image liveness "$LIVENESS_IMAGE")"

if [ -z "$IDP_IMAGE" ]; then
  echo "error: no image source was given; pass --registry, --image-repo, or --idp-image" >&2
  exit 2
fi
if csv_has background && [ -z "$BACKGROUND_IMAGE" ]; then
  echo "error: background is enabled but has no image; pass --background-image" >&2
  exit 2
fi
if csv_has socket && [ -z "$SOCKET_IMAGE" ]; then
  echo "error: socket is enabled but has no image; pass --socket-image" >&2
  exit 2
fi
if csv_has url-shortener && [ -z "$URL_SHORTENER_IMAGE" ]; then
  echo "error: the URL shortener is enabled but has no image; pass --url-shortener-image" >&2
  exit 2
fi
if [ "$LIVENESS" = true ] && [ -z "$LIVENESS_IMAGE" ]; then
  echo "error: liveness is enabled but has no image; pass --liveness-image" >&2
  exit 2
fi

# Lists only the images the selected services will actually use.
image_plan() {
  printf 'idp=%s' "$IDP_IMAGE"
  if csv_has background; then printf ' background=%s' "$BACKGROUND_IMAGE"; fi
  if csv_has socket; then printf ' socket=%s' "$SOCKET_IMAGE"; fi
  if csv_has url-shortener; then printf ' url-shortener=%s' "$URL_SHORTENER_IMAGE"; fi
  if [ "$LIVENESS" = true ]; then printf ' liveness=%s' "$LIVENESS_IMAGE"; fi
}

if [ "$PRINT_PLAN" = true ]; then
  cat <<EOF
Context:    $CONTEXT
Namespace:  $NAMESPACE
Release:    $RELEASE
Services:   idp${SERVICES:+,$SERVICES}
Images:     $(image_plan)
Liveness:   $LIVENESS
MailDev:    $MAILDEV
Values:     $VALUES_OUT (would be generated with credentials redacted)
Command:    helm upgrade --install $RELEASE $CHART --namespace $NAMESPACE --create-namespace -f $VALUES_OUT --wait --timeout $TIMEOUT
EOF
  exit 0
fi

# Splits REPO:TAG into SPLIT_REPOSITORY/SPLIT_TAG. These names are deliberately
# distinct from IMAGE_TAG, which holds the --image-tag default.
split_image() {
  ref="$1"
  case "$ref" in *:*) : ;; *) echo "error: image '$ref' must include a tag" >&2; exit 2 ;; esac
  SPLIT_REPOSITORY="${ref%:*}"
  SPLIT_TAG="${ref##*:}"
  [ -n "$SPLIT_REPOSITORY" ] && [ -n "$SPLIT_TAG" ] || { echo "error: invalid image '$ref'" >&2; exit 2; }
}

random_secret() {
  set +o pipefail
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
    printf '%s' "$value"
  else
    random_secret
  fi
}

ADMIN_PASSWORD="$(read_secret "$ADMIN_PASSWORD_FILE")"
POSTGRES_PASSWORD="$(read_secret "$POSTGRES_PASSWORD_FILE")"
MINIO_PASSWORD="$(read_secret "$MINIO_PASSWORD_FILE")"
SHORTENER_KEY="$(read_secret "$SHORTENER_KEY_FILE")"

VALUES_DIR="$(dirname "$VALUES_OUT")"
if [ ! -d "$VALUES_DIR" ]; then
  mkdir -p "$VALUES_DIR"
  chmod 700 "$VALUES_DIR"
fi
umask 077
if [ -e "$VALUES_OUT" ] && [ "$OVERWRITE" = false ]; then
  echo "error: values file already exists: $VALUES_OUT (use --overwrite or choose --values-out)" >&2
  exit 2
fi

split_image "$IDP_IMAGE"; IDP_REPO="$SPLIT_REPOSITORY"; IDP_TAG="$SPLIT_TAG"
split_image "$BACKGROUND_IMAGE"; BACKGROUND_REPO="$SPLIT_REPOSITORY"; BACKGROUND_TAG="$SPLIT_TAG"
split_image "$SOCKET_IMAGE"; SOCKET_REPO="$SPLIT_REPOSITORY"; SOCKET_TAG="$SPLIT_TAG"
split_image "$URL_SHORTENER_IMAGE"; URL_REPO="$SPLIT_REPOSITORY"; URL_TAG="$SPLIT_TAG"
if [ -n "$LIVENESS_IMAGE" ]; then
  split_image "$LIVENESS_IMAGE"; LIVENESS_REPO="$SPLIT_REPOSITORY"; LIVENESS_TAG="$SPLIT_TAG"
else
  LIVENESS_REPO="REPLACE_ME/ezto-liveness"; LIVENESS_TAG="latest"
fi

BACKGROUND_ENABLED=false; if csv_has background; then BACKGROUND_ENABLED=true; fi
SOCKET_ENABLED=false; if csv_has socket; then SOCKET_ENABLED=true; fi
URL_ENABLED=false; if csv_has url-shortener; then URL_ENABLED=true; fi

cat > "$VALUES_OUT" <<EOF
# Generated by scripts/install.sh. Contains credentials; do not commit.
global:
  storageClass: "$STORAGE_CLASS"
imagePullSecrets:$([ -n "$IMAGE_PULL_SECRET" ] && printf '\n  - name: "%s"' "$IMAGE_PULL_SECRET" || printf ' []')
idp:
  image: { repository: "$IDP_REPO", tag: "$IDP_TAG", pullPolicy: IfNotPresent }
  masterSslRequired: none
  localUrl: http://localhost:8080
  rootAccessIp: "$ROOT_ACCESS_IP"
  admin: { user: "$ADMIN_USER", password: "$ADMIN_PASSWORD" }
  shortenerKey: "$SHORTENER_KEY"
background:
  enabled: $BACKGROUND_ENABLED
  image: { repository: "$BACKGROUND_REPO", tag: "$BACKGROUND_TAG", pullPolicy: IfNotPresent }
socket:
  enabled: $SOCKET_ENABLED
  image: { repository: "$SOCKET_REPO", tag: "$SOCKET_TAG", pullPolicy: IfNotPresent }
urlShortener:
  enabled: $URL_ENABLED
  image: { repository: "$URL_REPO", tag: "$URL_TAG", pullPolicy: IfNotPresent }
  apiKeys: "$SHORTENER_KEY"
liveness:
  enabled: $LIVENESS
  image: { repository: "$LIVENESS_REPO", tag: "$LIVENESS_TAG", pullPolicy: IfNotPresent }
maildev:
  enabled: $MAILDEV
postgres:
  enabled: true
  password: "$POSTGRES_PASSWORD"
minio:
  enabled: true
  rootPassword: "$MINIO_PASSWORD"
elasticsearch:
  enabled: true
ingress:
  enabled: false
EOF
chmod 600 "$VALUES_OUT"

print_plan() {
  cat <<EOF
Context:    $CONTEXT
Namespace:  $NAMESPACE
Release:    $RELEASE
Services:   idp${SERVICES:+,$SERVICES}
Images:     $(image_plan)
Liveness:   $LIVENESS
MailDev:    $MAILDEV
Values:     $VALUES_OUT (credentials redacted; mode 0600)
Command:    helm upgrade --install $RELEASE $CHART --namespace $NAMESPACE --create-namespace -f $VALUES_OUT --wait --timeout $TIMEOUT
EOF
}

print_plan

command -v helm >/dev/null 2>&1 || { echo "error: helm is required" >&2; exit 3; }
command -v kubectl >/dev/null 2>&1 || { echo "error: kubectl is required" >&2; exit 3; }

helm template "$RELEASE" "$CHART" --namespace "$NAMESPACE" -f "$VALUES_OUT" >/dev/null
if [ "$DRY_RUN" = true ]; then
  echo "Helm render succeeded; the cluster was not changed."
  exit 0
fi

VERSION_JSON="$(kubectl --context "$CONTEXT" --request-timeout=10s get --raw /version)"
KUBE_MAJOR="$(printf '%s' "$VERSION_JSON" | sed -n 's/.*"major"[[:space:]]*:[[:space:]]*"\([0-9][0-9]*\)".*/\1/p')"
KUBE_MINOR="$(printf '%s' "$VERSION_JSON" | sed -n 's/.*"minor"[[:space:]]*:[[:space:]]*"\([0-9][0-9]*\)[^" ]*".*/\1/p')"
if [ -n "$KUBE_MAJOR" ] && [ -n "$KUBE_MINOR" ]; then
  if [ "$KUBE_MAJOR" -lt 1 ] || { [ "$KUBE_MAJOR" -eq 1 ] && [ "$KUBE_MINOR" -lt 25 ]; }; then
    echo "error: Kubernetes 1.25 or newer is required; context '$CONTEXT' reports $KUBE_MAJOR.$KUBE_MINOR" >&2
    exit 4
  fi
fi
NODE_ARCHES=""
if NODE_ARCHES="$(kubectl --context "$CONTEXT" --request-timeout=10s get nodes -o jsonpath='{range .items[*]}{.status.nodeInfo.architecture}{"\n"}{end}' 2>/dev/null)"; then
  if printf '%s\n' "$NODE_ARCHES" | grep -Ev '^(amd64)?$' >/dev/null; then
    echo "error: the ezto Verify application images are built for linux/amd64 only; context '$CONTEXT' contains: $(printf '%s' "$NODE_ARCHES" | tr '\n' ' ')" >&2
    exit 4
  fi
else
  echo "warning: node architecture could not be inspected with this namespace-scoped identity; image compatibility will be verified by Kubernetes during rollout" >&2
fi
if [ "$YES" = false ]; then
  if [ "$NON_INTERACTIVE" = true ] || ! prompt_yes_no "Apply this plan to context '$CONTEXT'?" false; then
    echo "Cancelled; no cluster changes were made. Values remain at $VALUES_OUT."
    exit 5
  fi
fi

helm --kube-context "$CONTEXT" upgrade --install "$RELEASE" "$CHART" \
  --namespace "$NAMESPACE" --create-namespace -f "$VALUES_OUT" \
  --wait --timeout "$TIMEOUT"

helm --kube-context "$CONTEXT" status "$RELEASE" --namespace "$NAMESPACE"
echo "ezto Verify is installed."
echo "Access it with: kubectl --context '$CONTEXT' -n '$NAMESPACE' port-forward svc/${RELEASE}-idp 8080:8080"
echo "Then browse: http://localhost:8080/auth/"
