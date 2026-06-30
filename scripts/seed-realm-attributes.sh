#!/usr/bin/env bash
#
# seed-realm-attributes.sh — seed root-tenant push / FIDO config on the Verify IdP.
#
# Sets up to three Keycloak realm attributes on the root tenant (the `master` realm):
#
#   firebase-json            FCM service-account JSON. Read from the ROOT realm at push
#                            time (MobileAuthorizer), so it is platform-wide: every
#                            tenant's push notifications use this one value.
#   fido-asset-links-ios     Apple app-site-association payload.
#   fido-asset-links-android Android assetlinks payload.
#                            Served at  <base>/realms/<realm>/.well-known/fido-ios
#                                  and  <base>/realms/<realm>/.well-known/fido-android
#                            NOTE: FIDO links are read from the CURRENT realm only.
#                            Seeding the root realm covers the root realm's own app —
#                            NOT sub-tenants (each sub-tenant sets its own under the
#                            "FIDO" tab).
#
# Idempotent: merges into the realm's existing attributes (other realm settings are
# left untouched). Only the files you pass are seeded — omit a flag to leave that
# attribute alone. Re-running with new files overwrites just those keys.
#
# Requires: curl, jq.
#
# Usage:
#   KC_ADMIN_PASSWORD=secret ./scripts/seed-realm-attributes.sh \
#       --firebase      ./firebase-sa.json \
#       --fido-ios      ./apple-app-site-association.json \
#       --fido-android  ./assetlinks.json
#
# Verify only (set nothing, just report current state):
#   KC_ADMIN_PASSWORD=secret ./scripts/seed-realm-attributes.sh
#
# Env (all optional except the password):
#   KC_BASE            IdP base URL incl. /auth   (default http://localhost:8080/auth)
#   KC_REALM           root tenant realm          (default master)
#   KC_ADMIN           admin username             (default admin)
#   KC_ADMIN_PASSWORD  admin password             (required; prompted if unset)
#   KC_CLIENT_ID       token client               (default admin-cli)
#
set -euo pipefail

KC_BASE="${KC_BASE:-http://localhost:8080/auth}"
KC_REALM="${KC_REALM:-master}"
KC_ADMIN="${KC_ADMIN:-admin}"
KC_CLIENT_ID="${KC_CLIENT_ID:-admin-cli}"

FIREBASE_FILE=""
FIDO_IOS_FILE=""
FIDO_ANDROID_FILE=""

usage() { sed -n '2,52p' "$0" | sed 's/^# \{0,1\}//'; exit "${1:-0}"; }

while [ $# -gt 0 ]; do
  case "$1" in
    --firebase)      FIREBASE_FILE="${2:?--firebase needs a file}"; shift 2 ;;
    --fido-ios)      FIDO_IOS_FILE="${2:?--fido-ios needs a file}"; shift 2 ;;
    --fido-android)  FIDO_ANDROID_FILE="${2:?--fido-android needs a file}"; shift 2 ;;
    -h|--help)       usage 0 ;;
    *) echo "unknown arg: $1" >&2; usage 1 ;;
  esac
done

# --- preflight ---------------------------------------------------------------
for bin in curl jq; do
  command -v "$bin" >/dev/null 2>&1 || { echo "error: '$bin' not found on PATH" >&2; exit 1; }
done
for f in "$FIREBASE_FILE" "$FIDO_IOS_FILE" "$FIDO_ANDROID_FILE"; do
  [ -z "$f" ] && continue
  [ -f "$f" ] || { echo "error: file not found: $f" >&2; exit 1; }
  jq empty "$f" >/dev/null 2>&1 || { echo "error: not valid JSON: $f" >&2; exit 1; }
done

if [ -z "${KC_ADMIN_PASSWORD:-}" ]; then
  printf 'Admin password for %s@%s: ' "$KC_ADMIN" "$KC_REALM" >&2
  read -rs KC_ADMIN_PASSWORD; echo >&2
fi

work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT

# --- auth --------------------------------------------------------------------
echo ">> authenticating as $KC_ADMIN on $KC_BASE/realms/master ..."
TOKEN="$(curl -fsS "$KC_BASE/realms/master/protocol/openid-connect/token" \
  -d grant_type=password -d "client_id=$KC_CLIENT_ID" \
  --data-urlencode "username=$KC_ADMIN" \
  --data-urlencode "password=$KC_ADMIN_PASSWORD" \
  | jq -re .access_token)" \
  || { echo "error: login failed (check KC_ADMIN / KC_ADMIN_PASSWORD / KC_BASE)" >&2; exit 1; }

auth=(-H "Authorization: Bearer $TOKEN")
realm_url="$KC_BASE/admin/realms/$KC_REALM"

# --- fetch current realm rep -------------------------------------------------
curl -fsS "${auth[@]}" "$realm_url" > "$work/realm.json" \
  || { echo "error: cannot read realm '$KC_REALM'" >&2; exit 1; }

# --- merge requested attributes ----------------------------------------------
# --rawfile reads the file as a single string and JSON-escapes it, which is
# exactly how Keycloak stores these JSON-valued attributes (string-typed).
apply_attr() {
  local key="$1" file="$2"
  echo ">> setting attribute '$key' from $file"
  jq --rawfile v "$file" --arg k "$key" '.attributes = (.attributes // {}) | .attributes[$k] = $v' \
     "$work/realm.json" > "$work/realm.next.json"
  mv "$work/realm.next.json" "$work/realm.json"
}

changed=0
[ -n "$FIREBASE_FILE" ]     && { apply_attr "firebase-json"            "$FIREBASE_FILE";     changed=1; }
[ -n "$FIDO_IOS_FILE" ]     && { apply_attr "fido-asset-links-ios"     "$FIDO_IOS_FILE";     changed=1; }
[ -n "$FIDO_ANDROID_FILE" ] && { apply_attr "fido-asset-links-android" "$FIDO_ANDROID_FILE"; changed=1; }

# --- push back ---------------------------------------------------------------
if [ "$changed" -eq 1 ]; then
  curl -fsS -X PUT "${auth[@]}" -H "Content-Type: application/json" \
    "$realm_url" -d @"$work/realm.json" \
    || { echo "error: realm update (PUT) failed" >&2; exit 1; }
  echo ">> realm '$KC_REALM' updated."
else
  echo ">> no files passed — nothing seeded (verify-only)."
fi

# --- verify (report presence + size, never print secret values) --------------
echo ">> current state of seeded attributes on '$KC_REALM':"
curl -fsS "${auth[@]}" "$realm_url" | jq -r '
  def state(k): (.attributes[k]) as $v
    | if $v == null then "UNSET"
      else "set (" + ($v | length | tostring) + " chars)" end;
  "   firebase-json            : " + state("firebase-json"),
  "   fido-asset-links-ios     : " + state("fido-asset-links-ios"),
  "   fido-asset-links-android : " + state("fido-asset-links-android")'

cat <<EOF

Verify the live FIDO well-known endpoints (root realm, public — no auth):
   curl $KC_BASE/realms/$KC_REALM/.well-known/fido-ios
   curl $KC_BASE/realms/$KC_REALM/.well-known/fido-android

firebase-json has no per-realm endpoint — it is consumed at push time and always
read from the root realm.

Security: firebase-json is stored as a plaintext realm attribute (it lands in the DB
and in any realm export). Do not commit realm exports of '$KC_REALM' to git.
EOF
