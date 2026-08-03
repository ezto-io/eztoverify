#!/usr/bin/env bash
# Build ezto Verify service images locally and load them straight into a
# kind or minikube node's image cache, so the cluster never needs to reach
# a registry (pullPolicy: Never in values-kind.yaml / values-minikube.yaml).
#
# Usage:
#   ./scripts/helm/build-and-load.sh kind|minikube [cluster-name] [--with-liveness]
#
# Examples:
#   ./scripts/helm/build-and-load.sh kind
#   ./scripts/helm/build-and-load.sh kind my-cluster --with-liveness
#   ./scripts/helm/build-and-load.sh minikube
#   ./scripts/helm/build-and-load.sh minikube my-profile
set -euo pipefail

usage() {
  echo "Usage: $0 kind|minikube [cluster-name] [--with-liveness]" >&2
  echo "  kind|minikube     target local cluster tool (required)" >&2
  echo "  cluster-name      kind cluster name (default: kind) or minikube profile (default: minikube)" >&2
  echo "  --with-liveness   also build+load the face-liveness image (heavy: pulls dlib/face_recognition deps)" >&2
  exit 1
}

TARGET=""
CLUSTER_NAME=""
WITH_LIVENESS=false

for arg in "$@"; do
  case "$arg" in
    --with-liveness)
      WITH_LIVENESS=true
      ;;
    kind|minikube)
      if [ -n "$TARGET" ]; then
        echo "error: target already set to '$TARGET', got second target '$arg'" >&2
        usage
      fi
      TARGET="$arg"
      ;;
    -h|--help)
      usage
      ;;
    *)
      if [ -z "$TARGET" ]; then
        echo "error: first argument must be 'kind' or 'minikube', got '$arg'" >&2
        usage
      fi
      if [ -n "$CLUSTER_NAME" ]; then
        echo "error: cluster name already set to '$CLUSTER_NAME', got '$arg'" >&2
        usage
      fi
      CLUSTER_NAME="$arg"
      ;;
  esac
done

if [ -z "$TARGET" ]; then
  usage
fi

if [ "$TARGET" = "kind" ]; then
  CLUSTER_NAME="${CLUSTER_NAME:-kind}"
else
  CLUSTER_NAME="${CLUSTER_NAME:-minikube}"
fi

# Repo root = two levels up from this script (scripts/helm/build-and-load.sh).
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
cd "$REPO_ROOT"

echo "==> Target: $TARGET (cluster/profile: $CLUSTER_NAME)"
if [ "$WITH_LIVENESS" = true ]; then
  echo "==> Including liveness image (heavy build)"
fi

# image tag => build context, in build order.
IMAGES=(
  "ezto-idp:latest:./idp"
  "ezto-background:latest:./services/background"
  "ezto-socket:latest:./services/socket"
  "ezto-url-shortener:latest:./services/url-shortener"
)
if [ "$WITH_LIVENESS" = true ]; then
  IMAGES+=("ezto-liveness:latest:./services/liveness")
fi

BUILT_IMAGES=()
for entry in "${IMAGES[@]}"; do
  IFS=':' read -r name tag context <<< "$entry"
  image="${name}:${tag}"
  echo "==> Building ${image} from ${context}"
  docker build -t "$image" "$context"
  BUILT_IMAGES+=("$image")
done

echo "==> Loading images into $TARGET"
for image in "${BUILT_IMAGES[@]}"; do
  echo "==> Loading ${image}"
  if [ "$TARGET" = "kind" ]; then
    kind load docker-image "$image" --name "$CLUSTER_NAME"
  else
    minikube image load "$image" -p "$CLUSTER_NAME"
  fi
done

echo ""
echo "==> Done. Images loaded into '$CLUSTER_NAME': ${BUILT_IMAGES[*]}"
echo ""
echo "Install (or upgrade) the chart with:"
echo ""
echo "  helm upgrade --install ezto charts/ezto-verify -f charts/ezto-verify/values-${TARGET}.yaml"
echo ""
