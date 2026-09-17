# Production-Ready Docker Compose Deployment — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Turn `docker-compose.yml` from a contributor build stack into a production single-host deployment path with registry images, generated credentials, selectable services, and no public exposure beyond nginx.

**Architecture:** `docker-compose.yml` becomes the deployment file — registry images resolved from `REGISTRY`/`TAG`, every credential read from a generated mode-600 `.env` via `${VAR:?}`, every optional component behind a Compose profile, and no published port except nginx's 80/443. Contributor build contexts move to a new `docker-compose.build.yml` overlay. A standalone `scripts/deploy-compose.sh` generates credentials and drives the stack; `scripts/compose/test-config.sh` asserts the whole contract through `docker compose config` without starting a container.

**Tech Stack:** Docker Compose v2 (spec version), POSIX shell (`bash` with `set -euo pipefail`), nginx 1.27, PostgreSQL 14, MinIO, Elasticsearch 7.17.

**Spec:** `docs/superpowers/specs/2026-09-04-compose-deployment-design.md`

## Global Constraints

Every task's requirements implicitly include this section. Values are copied verbatim from the spec.

- **Docker Compose >= 2.20.0** is a documented prerequisite. `depends_on: <svc>: {condition: service_started, required: false}` requires it.
- **No `change_me` default survives anywhere in the compose file.** Every required value uses `${VAR:?message}` so `docker compose up` refuses to start when one is unset.
- **`KEYCLOAK_ADMIN`**, never `KEYCLOAK_ADMIN_USER`. The compose file, the `idp-init` bootstrap command, and `charts/ezto-verify/templates/secret-idp.yaml` all read `KEYCLOAK_ADMIN`.
- **`ES_HOST` is a bare hostname with a separate `ES_PORT`**, not a URL.
- **`MINIO_PUBLIC_URL` is a distinct browser-facing URL**, not derivable from `MINIO_SERVER_URL`. It must be reachable by an end user's browser.
- **`SHORTENER_KEY`** is consumed by both the IdP (`SHORTENER_KEY`) and the URL shortener (`VALID_API_KEYS`) and must be identical in both; one variable enforces that.
- **`MINIO_ROOT_USER`/`MINIO_ROOT_PASSWORD` also populate `MINIO_ACCESS_KEY`/`MINIO_SECRET_KEY`** in v1. Deliberate, recorded under the spec's Out of scope.
- **Only nginx publishes ports, and exactly `80` and `443`.** Any debugging port is bound to `127.0.0.1` explicitly.
- **`VARIANT` must render non-empty** (`"ezto verify"`). `EnvironmentReader.getValue("VARIANT").equalsIgnoreCase(...)` has no null guard; unset means an NPE during master-realm bootstrap.
- **`ROOT_ACCESS_IP` must be non-empty, exact comma-separated IPs, no CIDR.** `isLocal()` does `ROOT_ACCESS_IP.split(",")` with no null guard; unset means HTTP 500 on `GET /auth/`.
- **The database name is spelled `urlshortner`** (as in `init-databases.sql`), not `urlshortener`.
- **`docker-compose.dev.yml` is unchanged** by this work.
- **Scripts are standalone** — no shared library. Duplicated credential helpers are an accepted cost, decided in the spec.
- **No service images are built or published by this work.** `REGISTRY`/`TAG` point at images that already exist; failing to find them must produce a clear message.

---

### Task 1: Credentials leave the tracked tree

The compose file currently carries `POSTGRES_PASSWORD: admin`, `MINIO_ROOT_PASSWORD: minioadmin123`, and `SHORTENER_KEY: change_me_key` as literals. This task replaces every one with a `${VAR:?}` reference and writes the `.env` contract that documents them.

**Files:**
- Create: `scripts/compose/test-config.sh`
- Modify: `docker-compose.yml` (all `environment:` blocks)
- Modify: `.env.example` (full rewrite)

**Interfaces:**
- Consumes: nothing.
- Produces: `scripts/compose/test-config.sh` with shell helpers `pass <msg>`, `fail <msg>`, `run_config <profiles>` used by Tasks 2–5; the `.env.example` variable set consumed by `scripts/deploy-compose.sh` in Task 6.

- [ ] **Step 1: Write the failing test**

Create `scripts/compose/test-config.sh`:

```bash
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
# resolve. Deliberately NOT sourced from any .env on disk.
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
  local env_file
  env_file="$(mktemp)"
  full_env > "$env_file"
  printf 'COMPOSE_PROFILES=%s\n' "$profiles" >> "$env_file"
  local args=(-f "$COMPOSE_FILE")
  local extra
  for extra in "$@"; do args+=(-f "$extra"); done
  ( cd "$REPO_ROOT" && docker compose --env-file "$env_file" "${args[@]}" config 2>&1 )
  local rc=$?
  rm -f "$env_file"
  return $rc
}

ALL_PROFILES=infra-postgres,infra-minio,infra-elasticsearch,background,socket,url-shortener

echo "== no literal credentials in the tracked compose file =="
if grep -nE '(PASSWORD|SECRET|ROOT_USER|VALID_API_KEYS|SHORTENER_KEY)[[:space:]]*:[[:space:]]*[^$[:space:]]' "$COMPOSE_FILE" >/dev/null; then
  grep -nE '(PASSWORD|SECRET|ROOT_USER|VALID_API_KEYS|SHORTENER_KEY)[[:space:]]*:[[:space:]]*[^$[:space:]]' "$COMPOSE_FILE" >&2
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
  grep -oE '\$\{[A-Z_][A-Z0-9_]*' "$COMPOSE_FILE" | sed 's/^\${//' | sort -u
}
example_vars() {
  grep -oE '^[A-Z_][A-Z0-9_]*=' "$ENV_EXAMPLE" | sed 's/=$//' | sort -u
}
missing="$(comm -23 <(compose_vars) <(example_vars))"
if [ -n "$missing" ]; then
  printf '%s\n' "$missing" >&2
  fail "compose references variables absent from .env.example"
else
  pass "every compose variable is documented in .env.example"
fi
unused="$(comm -13 <(compose_vars) <(example_vars) | grep -v '^COMPOSE_PROFILES$')"
if [ -n "$unused" ]; then
  printf '%s\n' "$unused" >&2
  fail ".env.example documents variables the compose file never reads"
else
  pass "every .env.example variable is read by the compose file"
fi

echo
if [ "$FAILURES" -eq 0 ]; then
  echo "All compose config assertions passed."
  exit 0
fi
echo "$FAILURES assertion(s) failed." >&2
exit 1
```

Make it executable:

```bash
chmod +x scripts/compose/test-config.sh
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./scripts/compose/test-config.sh`
Expected: FAIL — `credential keys must reference ${VAR}, not literals`, `placeholder credential values remain`, and `compose references variables absent from .env.example`.

- [ ] **Step 3: Rewrite `.env.example`**

Replace the whole file:

```bash
# ezto Verify — Docker Compose deployment environment.
#
#   cp .env.example .env    # then edit, or let scripts/deploy-compose.sh generate it
#
# .env is git-ignored and must be mode 600: it holds every credential the stack
# uses. docker-compose.yml reads each required value as ${VAR:?}, so an unset
# value stops the stack with a named error instead of silently using a default.
#
# docker-compose.dev.yml (plain-HTTP contributor infra) ignores this file.

# --- Identity of the deployment ---------------------------------------------
# Public domain the IdP is served on. Must resolve to this host. TLS certs for
# it live in nginx/certs/ (fullchain.pem + privkey.pem).
DOMAIN=verify.example.com
# Registry prefix holding one repository per service: ezto-idp, ezto-background,
# ezto-socket, ezto-url-shortener, ezto-liveness.
REGISTRY=registry.example.com/ezto
# One tag deployed across every service. Upgrades are "change TAG, pull, up -d".
TAG=1.0.0

# --- TLS --------------------------------------------------------------------
# self-signed = scripts/deploy-compose.sh generates a certificate (browsers warn).
# files       = you placed fullchain.pem and privkey.pem in nginx/certs yourself.
TLS_MODE=self-signed

# --- Credentials (generated by scripts/deploy-compose.sh) -------------------
# Rotating these after first start does NOT re-key existing data. See
# docs/deployment-compose.md "Adopting an existing stack".
POSTGRES_USER=ezto
POSTGRES_PASSWORD=
MINIO_ROOT_USER=ezto
MINIO_ROOT_PASSWORD=
KEYCLOAK_ADMIN=admin
KEYCLOAK_ADMIN_PASSWORD=
# Shared by the IdP (SHORTENER_KEY) and the URL shortener (VALID_API_KEYS).
# One variable so the two can never drift apart.
SHORTENER_KEY=

# --- Endpoints --------------------------------------------------------------
# Internal service names when the matching infra-* profile is on; your own
# endpoint when it is off. Dropping a profile has preconditions — see
# docs/deployment-compose.md "External stores".
POSTGRES_HOST=postgres
POSTGRES_PORT=5432
MINIO_SERVER_URL=http://minio:9000
# Browser-facing URL for public objects. NOT derivable from MINIO_SERVER_URL.
# Must be reachable by an end user's browser; required when MinIO is external.
MINIO_PUBLIC_URL=https://verify.example.com/storage/ezto-verify/public
MINIO_BUCKET=ezto-verify
# Bare hostname and port, not a URL.
ES_HOST=elasticsearch
ES_PORT=9200
ES_SCHEME=http

# --- IdP behaviour ----------------------------------------------------------
# Exact comma-separated IPs allowed to perform master-tenant onboarding.
# NO CIDR. Behind nginx this is compared against the X-Real-IP header, so it
# must contain the real client IP of the browser doing the onboarding.
ROOT_ACCESS_IP=127.0.0.1,::1
# Admin-console and email "Docs" links. Empty hides them.
DOCS_URL=
DOCS_LINK=

# --- Service URLs advertised by the IdP -------------------------------------
# Each must be EMPTY when the matching profile is off, so the IdP does not
# advertise a service that is not running. scripts/deploy-compose.sh keeps
# these in step with COMPOSE_PROFILES; edit both together if you hand-edit.
BACKGROUND_JOB_URL=http://background:8070
SOCKET_URL=http://socket:3001
SHORTENER_HTTP_URL=http://url-shortener:3000
SHORTENER_HTTPS_URL=http://url-shortener:3000
# Face liveness is off by default (~15 GB image).
LIVELINESS_URL=

# --- Active services and bundled stores -------------------------------------
# Drop an infra-* profile to use your own store. Drop a service profile to not
# run it — and blank its URL above.
COMPOSE_PROFILES=infra-postgres,infra-minio,infra-elasticsearch,background,socket,url-shortener
```

- [ ] **Step 4: Replace every literal credential in `docker-compose.yml`**

Apply these substitutions inside `docker-compose.yml`. Only `environment:` values change in this step; ports, images, and profiles are Tasks 2–4.

`postgres`:

```yaml
    environment:
      POSTGRES_USER: ${POSTGRES_USER:?set POSTGRES_USER in .env}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD:?set POSTGRES_PASSWORD in .env}
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U $${POSTGRES_USER}"]
```

`minio`:

```yaml
    environment:
      MINIO_ROOT_USER: ${MINIO_ROOT_USER:?set MINIO_ROOT_USER in .env}
      MINIO_ROOT_PASSWORD: ${MINIO_ROOT_PASSWORD:?set MINIO_ROOT_PASSWORD in .env}
```

`minio-init` — the alias credentials move to the environment so they are not
interpolated into a shell string in the tracked file:

```yaml
    environment:
      MINIO_ROOT_USER: ${MINIO_ROOT_USER:?}
      MINIO_ROOT_PASSWORD: ${MINIO_ROOT_PASSWORD:?}
      MINIO_BUCKET: ${MINIO_BUCKET:?set MINIO_BUCKET in .env}
      MINIO_SERVER_URL: ${MINIO_SERVER_URL:?set MINIO_SERVER_URL in .env}
    entrypoint: >
      /bin/sh -c "
      mc alias set local \"$$MINIO_SERVER_URL\" \"$$MINIO_ROOT_USER\" \"$$MINIO_ROOT_PASSWORD\" &&
      mc mb -p \"local/$$MINIO_BUCKET\" || true &&
      mc anonymous set download \"local/$$MINIO_BUCKET/public\" || true
      "
```

`idp` — every credential, endpoint, and advertised URL:

```yaml
    environment:
      KC_DB: postgres
      KC_DB_URL_HOST: ${POSTGRES_HOST:?set POSTGRES_HOST in .env}
      KC_DB_URL_PORT: ${POSTGRES_PORT:?set POSTGRES_PORT in .env}
      KC_DB_USERNAME: ${POSTGRES_USER:?}
      KC_DB_PASSWORD: ${POSTGRES_PASSWORD:?}
      KC_DB_URL_DATABASE: eztoverify
      KEYCLOAK_ADMIN: ${KEYCLOAK_ADMIN:?set KEYCLOAK_ADMIN in .env}
      KEYCLOAK_ADMIN_PASSWORD: ${KEYCLOAK_ADMIN_PASSWORD:?set KEYCLOAK_ADMIN_PASSWORD in .env}
      KC_HOSTNAME_URL: https://${DOMAIN:?}/auth
      KC_HOSTNAME: ${DOMAIN:?}
      KC_HOSTNAME_STRICT: "true"
      KC_HOSTNAME_STRICT_HTTPS: "true"
      KC_HTTP_ENABLED: "true"
      KC_PROXY: edge
      IDP_HOST: http://idp:8080
      ENV: prod
      # EnvironmentReader.getValue("VARIANT").equalsIgnoreCase(...) has no null
      # guard — unset means an NPE during master-realm bootstrap.
      VARIANT: "ezto verify"
      DASHBOARD_URL: https://${DOMAIN:?}
      DOCS_URL: ${DOCS_URL:-}
      DOCS_LINK: ${DOCS_LINK:-}
      # isLocal() does ROOT_ACCESS_IP.split(",") with no null guard — unset
      # means HTTP 500 on GET /auth/. Exact IPs only, no CIDR. Behind nginx
      # this is matched against X-Real-IP.
      ROOT_ACCESS_IP: ${ROOT_ACCESS_IP:?set ROOT_ACCESS_IP in .env}
      # Empty whenever the matching profile is off — see .env.example.
      BACKGROUND_JOB_URL: ${BACKGROUND_JOB_URL:-}
      SHORTENER_HTTPS_URL: ${SHORTENER_HTTPS_URL:-}
      SHORTENER_HTTP_URL: ${SHORTENER_HTTP_URL:-}
      SOCKET_URL: ${SOCKET_URL:-}
      LIVELINESS_URL: ${LIVELINESS_URL:-}
      SHORTENER_KEY: ${SHORTENER_KEY:?set SHORTENER_KEY in .env}
      MINIO_SERVER_URL: ${MINIO_SERVER_URL:?}
      # v1: the application uses MinIO ROOT credentials. Recorded as a known
      # weakness in the design's Out of scope; a scoped key pair is follow-up.
      MINIO_ACCESS_KEY: ${MINIO_ROOT_USER:?}
      MINIO_SECRET_KEY: ${MINIO_ROOT_PASSWORD:?}
      MINIO_PUBLIC_URL: ${MINIO_PUBLIC_URL:?set MINIO_PUBLIC_URL in .env}
      ES_HOST: ${ES_HOST:?set ES_HOST in .env}
      ES_PORT: ${ES_PORT:?set ES_PORT in .env}
      ES_SCHEME: ${ES_SCHEME:?set ES_SCHEME in .env}
      APP_PUSH_VERSION: 9.3.1
```

`idp-init`:

```yaml
    environment:
      KEYCLOAK_ADMIN: ${KEYCLOAK_ADMIN:?}
      KEYCLOAK_ADMIN_PASSWORD: ${KEYCLOAK_ADMIN_PASSWORD:?}
      MASTER_SSL_REQUIRED: external
```

`background`:

```yaml
    environment:
      JOB_DB_TYPE: psql
      JOB_DB_URL: jdbc:postgresql://${POSTGRES_HOST:?}:${POSTGRES_PORT:?}/jobrnr
      JOB_DB_USER: ${POSTGRES_USER:?}
      JOB_DB_PASSWORD: ${POSTGRES_PASSWORD:?}
```

`socket`:

```yaml
    environment:
      PORT: "3001"
      PGHOST: ${POSTGRES_HOST:?}
      PGPORT: ${POSTGRES_PORT:?}
      PGUSER: ${POSTGRES_USER:?}
      PGPASSWORD: ${POSTGRES_PASSWORD:?}
      PGDATABASE: socket
```

`url-shortener` — note the `urlshortner` spelling, which matches `init-databases.sql`:

```yaml
    environment:
      PORT: "3000"
      DATABASE_URL: postgresql://${POSTGRES_USER:?}:${POSTGRES_PASSWORD:?}@${POSTGRES_HOST:?}:${POSTGRES_PORT:?}/urlshortner?schema=public
      VALID_API_KEYS: ${SHORTENER_KEY:?}
      FALLBACK_URL: https://${DOMAIN:?}
```

`liveness`:

```yaml
    environment:
      MINIO_HOST: ${MINIO_SERVER_URL:?}
      MINIO_ACCESS_KEY: ${MINIO_ROOT_USER:?}
      MINIO_SECRET: ${MINIO_ROOT_PASSWORD:?}
      FACE_TOLERANCE: "0.68"
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./scripts/compose/test-config.sh`
Expected: PASS on all four Task 1 assertions.

- [ ] **Step 6: Commit**

```bash
git add scripts/compose/test-config.sh .env.example docker-compose.yml
git commit -m "feat(compose): read every credential from .env instead of tracked literals"
```

---

### Task 2: Registry images, with build contexts moved to an overlay

A deployer must need no toolchain. Every `build:` context moves to `docker-compose.build.yml`, and the deployed version becomes a tag.

**Files:**
- Create: `docker-compose.build.yml`
- Modify: `docker-compose.yml` (`image:` and `build:` keys on `idp`, `idp-init`, `background`, `socket`, `url-shortener`, `liveness`)
- Modify: `scripts/compose/test-config.sh` (append assertions)

**Interfaces:**
- Consumes: `run_config`, `pass`, `fail`, `ALL_PROFILES` from Task 1.
- Produces: image references of the form `${REGISTRY}/ezto-<service>:${TAG}`, consumed by `scripts/deploy-compose.sh` in Task 6.

- [ ] **Step 1: Write the failing test**

Insert into `scripts/compose/test-config.sh`, immediately before the final `echo` / `FAILURES` block:

```bash
echo "== the deployment file builds nothing =="
if grep -nE '^\s+build:' "$COMPOSE_FILE" >/dev/null; then
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
  count="$(printf '%s' "$built" | grep -cE '^\s+context:')"
  if [ "$count" -eq 5 ]; then
    pass "the overlay restores 5 build contexts"
  else
    fail "the overlay restores $count build contexts, expected 5"
  fi
fi
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./scripts/compose/test-config.sh`
Expected: FAIL — `docker-compose.yml still declares build contexts`, five `does not resolve from REGISTRY and TAG` lines, and `docker-compose.build.yml is missing`.

- [ ] **Step 3: Point every service at a registry image**

In `docker-compose.yml`, delete each `build:` block and set the image. `idp-init` deliberately shares the IdP image because it runs `kcadm.sh` from it:

```yaml
  idp:
    image: ${REGISTRY:?set REGISTRY in .env}/ezto-idp:${TAG:?set TAG in .env}
```

```yaml
  idp-init:
    image: ${REGISTRY:?}/ezto-idp:${TAG:?}
```

```yaml
  background:
    image: ${REGISTRY:?}/ezto-background:${TAG:?}
```

```yaml
  socket:
    image: ${REGISTRY:?}/ezto-socket:${TAG:?}
```

```yaml
  url-shortener:
    image: ${REGISTRY:?}/ezto-url-shortener:${TAG:?}
```

```yaml
  liveness:
    image: ${REGISTRY:?}/ezto-liveness:${TAG:?}
```

- [ ] **Step 4: Create the contributor build overlay**

Create `docker-compose.build.yml`:

```yaml
# Contributor overlay: restores build-from-source for the five buildable
# services. The deployment file (docker-compose.yml) pulls registry images and
# needs no toolchain; this file is how a contributor builds them locally.
#
#   docker compose -f docker-compose.yml -f docker-compose.build.yml up --build
#
# The image name is still taken from REGISTRY/TAG so a locally built image
# lands on the same reference the deployment file expects. Face liveness is a
# roughly 15 GB build and stays behind the face-biometrics profile.

services:
  idp:
    build:
      context: ./idp

  background:
    build:
      context: ./services/background

  socket:
    build:
      context: ./services/socket

  url-shortener:
    build:
      context: ./services/url-shortener

  liveness:
    build:
      context: ./services/liveness
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./scripts/compose/test-config.sh`
Expected: PASS on the build-context, registry-resolution, and overlay assertions.

- [ ] **Step 6: Commit**

```bash
git add docker-compose.yml docker-compose.build.yml scripts/compose/test-config.sh
git commit -m "feat(compose): deploy registry images and move build contexts to an overlay"
```

---

### Task 3: Nothing but nginx is reachable from outside

The current file publishes PostgreSQL, MinIO, Elasticsearch, `background`, `socket`, and `url-shortener` on every interface. On a host with a public address that is an unauthenticated audit store, an exposed database, and exposed object storage.

**Files:**
- Modify: `docker-compose.yml` (every `ports:` key)
- Modify: `scripts/compose/test-config.sh` (append assertions)

**Interfaces:**
- Consumes: `run_config`, `pass`, `fail`, `ALL_PROFILES` from Task 1.
- Produces: nothing consumed by later tasks.

- [ ] **Step 1: Write the failing test**

Insert into `scripts/compose/test-config.sh`, before the final `FAILURES` block:

```bash
echo "== only nginx publishes a port =="
# `docker compose config` renders published ports as a `published:` key under
# each service's `ports:`. Walk the rendered document and collect the owning
# service for every published entry.
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

nginx_ports="$(printf '%s\n' "$publishers" | grep '^nginx=' | cut -d= -f2 | sort -u | paste -sd, -)"
if [ "$nginx_ports" = "80,443" ]; then
  pass "nginx publishes exactly 80 and 443"
else
  fail "nginx publishes '$nginx_ports', expected '80,443'"
fi
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./scripts/compose/test-config.sh`
Expected: FAIL — `a service other than nginx publishes a port`, listing `postgres=5432`, `minio=9000`, `minio=9001`, `elasticsearch=9200`, `background=8070`, `socket=3001`, `url-shortener=3000`, `liveness=5000`.

- [ ] **Step 3: Remove every published port except nginx's**

Delete these lines from `docker-compose.yml` entirely:

```
    ports: ["5432:5432"]
    ports: ["9000:9000", "9001:9001"]
    ports: ["9200:9200"]
    ports: ["8070:8070"]
    ports: ["3001:3001"]
    ports: ["3000:3000"]
    ports: ["5000:5000"]
```

Leave `nginx` exactly as it is:

```yaml
    ports:
      - "80:80"
      - "443:443"
```

Add this comment above the `postgres` service so the decision is not silently reversed later:

```yaml
  # Backing stores and internal services publish NO ports: they reach each
  # other over the compose network. Publishing them binds 0.0.0.0, which on a
  # host with a public address exposes the database, object storage, and an
  # Elasticsearch running with security disabled. If you genuinely need one for
  # debugging, bind it to loopback explicitly — ports: ["127.0.0.1:5432:5432"] —
  # and remove it again afterwards.
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./scripts/compose/test-config.sh`
Expected: PASS — `nginx is the only service publishing a port`, `nginx publishes exactly 80 and 443`.

- [ ] **Step 5: Commit**

```bash
git add docker-compose.yml scripts/compose/test-config.sh
git commit -m "fix(compose): stop publishing the database, object storage, and internal services"
```

---

### Task 4: Profiles make every optional component selectable

Compose cannot define a service conditionally, so each optional component sits behind a profile that `COMPOSE_PROFILES` activates. A dependency on a service excluded by profiles otherwise fails the whole graph.

**Files:**
- Modify: `docker-compose.yml` (`profiles:` and `depends_on:` on every service)
- Modify: `scripts/compose/test-config.sh` (append assertions)

**Interfaces:**
- Consumes: `run_config`, `pass`, `fail`, `ALL_PROFILES` from Task 1.
- Produces: the profile names `infra-postgres`, `infra-minio`, `infra-elasticsearch`, `background`, `socket`, `url-shortener`, `face-biometrics`, consumed by `scripts/deploy-compose.sh` in Task 6.

- [ ] **Step 1: Write the failing test**

Insert into `scripts/compose/test-config.sh`, before the final `FAILURES` block:

```bash
echo "== dropping a service profile removes it and blanks its advertised URL =="
# The URL is blanked by the deployer writing an empty value, so assert the two
# together the way a real deploy produces them.
assert_service_dropped() {
  local profile="$1" service="$2" var="$3"
  local profiles out env_file
  profiles="$(printf '%s' "$ALL_PROFILES" | tr ',' '\n' | grep -v "^${profile}$" | paste -sd, -)"
  env_file="$(mktemp)"
  full_env | grep -v "^${var}=" > "$env_file"
  printf '%s=\n' "$var" >> "$env_file"
  printf 'COMPOSE_PROFILES=%s\n' "$profiles" >> "$env_file"
  out="$( cd "$REPO_ROOT" && docker compose --env-file "$env_file" -f "$COMPOSE_FILE" config 2>&1 )"
  local rc=$?
  rm -f "$env_file"
  if [ $rc -ne 0 ]; then
    printf '%s\n' "$out" >&2
    fail "the graph does not resolve with '$profile' dropped"
    return
  fi
  if printf '%s' "$out" | grep -qE "^  ${service}:$"; then
    fail "'$service' is still present with profile '$profile' dropped"
  else
    pass "'$service' is absent with profile '$profile' dropped"
  fi
  if printf '%s' "$out" | grep -qE "${var}: \"\"|${var}: ''|${var}: *$"; then
    pass "$var is blank with '$service' off"
  else
    fail "$var is not blank with '$service' off"
  fi
}

assert_service_dropped background     background     BACKGROUND_JOB_URL
assert_service_dropped socket         socket         SOCKET_URL
assert_service_dropped url-shortener  url-shortener  SHORTENER_HTTP_URL

echo "== dropping a bundled store leaves the rest of the graph resolvable =="
without_pg="$(printf '%s' "$ALL_PROFILES" | tr ',' '\n' | grep -v '^infra-postgres$' | paste -sd, -)"
if out="$(run_config "$without_pg")"; then
  if printf '%s' "$out" | grep -qE '^  postgres:$'; then
    fail "'postgres' is still present with infra-postgres dropped"
  else
    pass "'postgres' is absent with infra-postgres dropped"
  fi
  if printf '%s' "$out" | grep -qE '^  idp:$'; then
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
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./scripts/compose/test-config.sh`
Expected: FAIL — services are still present with their profiles dropped, and `the graph does not resolve with infra-postgres dropped` (a hard `depends_on` on an excluded service).

- [ ] **Step 3: Add profiles and tolerant infrastructure dependencies**

Add a `profiles:` key to each optional service in `docker-compose.yml`:

```yaml
  postgres:
    profiles: ["infra-postgres"]
```

```yaml
  minio:
    profiles: ["infra-minio"]
```

```yaml
  minio-init:
    profiles: ["infra-minio"]
```

```yaml
  elasticsearch:
    profiles: ["infra-elasticsearch"]
```

```yaml
  background:
    profiles: ["background"]
```

```yaml
  socket:
    profiles: ["socket"]
```

```yaml
  url-shortener:
    profiles: ["url-shortener"]
```

`liveness` keeps `profiles: ["face-biometrics"]`. `idp`, `idp-init`, `nginx`, and `certs-check` carry no profile and always run.

Replace the `idp` `depends_on:` block. `required: false` reduces an absent dependency to a warning instead of a graph failure, and needs Compose >= 2.20.0:

```yaml
    # Infrastructure and optional services may be excluded by profiles, so each
    # dependency is declared non-required: Compose warns rather than failing the
    # graph. `required: false` needs Docker Compose >= 2.20.0.
    depends_on:
      postgres:      { condition: service_healthy, required: false }
      minio:         { condition: service_healthy, required: false }
      minio-init:    { condition: service_completed_successfully, required: false }
      elasticsearch: { condition: service_healthy, required: false }
      background:    { condition: service_healthy, required: false }
```

Apply the same treatment to the remaining cross-service dependencies:

```yaml
  background:
    depends_on:
      postgres: { condition: service_healthy, required: false }
```

```yaml
  socket:
    depends_on:
      postgres: { condition: service_healthy, required: false }
```

```yaml
  url-shortener:
    depends_on:
      postgres: { condition: service_healthy, required: false }
```

```yaml
  liveness:
    depends_on:
      minio: { condition: service_healthy, required: false }
```

```yaml
  minio-init:
    depends_on:
      minio: { condition: service_healthy, required: false }
```

`idp-init` depends on `idp`, and `nginx` depends on `certs-check` and `idp`. Those are never excluded by a profile, so they stay required.

Add the Compose floor to the file header comment:

```yaml
# Requires Docker Compose >= 2.20.0 (`depends_on: required: false`).
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./scripts/compose/test-config.sh`
Expected: PASS on all Task 4 assertions.

- [ ] **Step 5: Commit**

```bash
git add docker-compose.yml scripts/compose/test-config.sh
git commit -m "feat(compose): make every store and optional service selectable by profile"
```

---

### Task 5: The stack survives a reboot and cannot fill the disk

Every long-running service needs a restart policy, a healthcheck, and a log-size cap so a single host cannot fill its disk with container logs.

**Files:**
- Modify: `docker-compose.yml` (`restart:`, `logging:`, `healthcheck:` keys)
- Modify: `scripts/compose/test-config.sh` (append assertions)

**Interfaces:**
- Consumes: `run_config`, `pass`, `fail`, `ALL_PROFILES` from Task 1.
- Produces: nothing consumed by later tasks.

- [ ] **Step 1: Write the failing test**

Insert into `scripts/compose/test-config.sh`, before the final `FAILURES` block:

```bash
echo "== every long-running service restarts and caps its logs =="
LONG_RUNNING="postgres minio elasticsearch idp nginx background socket url-shortener liveness"
rendered_all="$(run_config "$ALL_PROFILES,face-biometrics")"
for svc in $LONG_RUNNING; do
  block="$(printf '%s\n' "$rendered_all" | awk -v s="  ${svc}:" '
    $0 == s { inblock = 1; next }
    /^  [a-z0-9_-]+:$/ { inblock = 0 }
    inblock { print }
  ')"
  if printf '%s' "$block" | grep -q 'restart: unless-stopped'; then
    pass "$svc restarts unless stopped"
  else
    fail "$svc has no 'restart: unless-stopped'"
  fi
  if printf '%s' "$block" | grep -q 'max-size'; then
    pass "$svc caps its log size"
  else
    fail "$svc does not cap its log size"
  fi
done

echo "== one-shot jobs do not restart =="
for svc in minio-init idp-init certs-check; do
  block="$(printf '%s\n' "$rendered_all" | awk -v s="  ${svc}:" '
    $0 == s { inblock = 1; next }
    /^  [a-z0-9_-]+:$/ { inblock = 0 }
    inblock { print }
  ')"
  if printf '%s' "$block" | grep -qE 'restart: "?no"?'; then
    pass "$svc does not restart"
  else
    fail "$svc should declare restart: \"no\""
  fi
done
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./scripts/compose/test-config.sh`
Expected: FAIL — nine `does not cap its log size` lines. `url-shortener` and `liveness` restart correctly already; `socket` and `url-shortener` have no healthcheck yet but that is not asserted here.

- [ ] **Step 3: Add a log cap to every service**

Add this block to each of the nine long-running services plus the three one-shot jobs in `docker-compose.yml`. A capped, rotated JSON log keeps a single noisy container from filling the host disk:

```yaml
    logging:
      driver: json-file
      options:
        max-size: "10m"
        max-file: "3"
```

Confirm `restart: unless-stopped` is present on `postgres`, `minio`, `elasticsearch`, `idp`, `nginx`, `background`, `socket`, `url-shortener`, and `liveness`, and `restart: "no"` on `minio-init`, `idp-init`, and `certs-check`.

Add healthchecks to the two services that lack them, so `depends_on: service_healthy` is meaningful and a wedged process is restarted:

```yaml
  socket:
    healthcheck:
      test: ["CMD-SHELL", "node -e 'require(\"net\").connect(3001,\"127.0.0.1\").on(\"connect\",()=>process.exit(0)).on(\"error\",()=>process.exit(1))'"]
      interval: 10s
      timeout: 5s
      retries: 5
      start_period: 20s
```

```yaml
  url-shortener:
    healthcheck:
      test: ["CMD-SHELL", "node -e 'require(\"net\").connect(3000,\"127.0.0.1\").on(\"connect\",()=>process.exit(0)).on(\"error\",()=>process.exit(1))'"]
      interval: 10s
      timeout: 5s
      retries: 5
      start_period: 20s
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./scripts/compose/test-config.sh`
Expected: PASS on every restart, log-cap, and one-shot assertion.

- [ ] **Step 5: Commit**

```bash
git add docker-compose.yml scripts/compose/test-config.sh
git commit -m "feat(compose): cap container logs and health-check every long-running service"
```

---

### Task 6: The guided deployer

`scripts/deploy-compose.sh` generates credentials into a mode-600 `.env`, keeps `COMPOSE_PROFILES` and the advertised service URLs in step, checks the host preconditions Compose cannot, and refuses to invent credentials for a host that already holds data.

**Files:**
- Create: `scripts/deploy-compose.sh`
- Modify: `scripts/compose/test-config.sh` (append assertions)

**Interfaces:**
- Consumes: the `.env.example` variable set (Task 1), image references `${REGISTRY}/ezto-<service>:${TAG}` (Task 2), profile names (Task 4).
- Produces: a generated `.env`, and `--print-plan` output asserted by the tests below.

- [ ] **Step 1: Write the failing test**

Insert into `scripts/compose/test-config.sh`, before the final `FAILURES` block:

```bash
echo "== the deployer plans without credentials, an image source, or side effects =="
DEPLOY="$REPO_ROOT/scripts/deploy-compose.sh"
if [ ! -x "$DEPLOY" ]; then
  fail "scripts/deploy-compose.sh is missing or not executable"
else
  sandbox="$(mktemp -d)"
  if ( cd "$sandbox" && "$DEPLOY" --print-plan --env-out "$sandbox/.env" --domain verify.example.com </dev/null >"$sandbox/plan.out" 2>&1 ); then
    pass "--print-plan succeeds with no image source and no credentials"
  else
    cat "$sandbox/plan.out" >&2
    fail "--print-plan failed"
  fi
  if ( cd "$sandbox" && "$DEPLOY" --uninstall --print-plan --env-out "$sandbox/.env" </dev/null >"$sandbox/uninstall.out" 2>&1 ); then
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
  if [ -e "$REPO_ROOT/nginx/certs/fullchain.pem.generated" ]; then
    fail "--print-plan generated a certificate"
  else
    pass "--print-plan generated no certificate"
  fi
  if grep -qiE '(password|secret|key)[=:][[:alnum:]]{6,}' "$sandbox/plan.out"; then
    fail "--print-plan printed an unredacted credential"
  else
    pass "--print-plan redacts credentials"
  fi
  rm -rf "$sandbox"
fi
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./scripts/compose/test-config.sh`
Expected: FAIL — `scripts/deploy-compose.sh is missing or not executable`.

- [ ] **Step 3: Write the deployer**

Create `scripts/deploy-compose.sh`:

```bash
#!/usr/bin/env bash
# Guided deployer for ezto Verify on a single host with Docker Compose.
# It generates one auditable .env and delegates the deployment to Compose.
#
# It reads top-to-bottom on purpose: there is no shared library between this
# script and scripts/install.sh, so the credential helpers are duplicated. That
# cost was accepted in docs/superpowers/specs/2026-09-04-compose-deployment-design.md.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
COMPOSE_FILE="$REPO_ROOT/docker-compose.yml"

DOMAIN=""
REGISTRY=""
TAG="latest"
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
POSTGRES_PASSWORD_FILE=""
POSTGRES_USER="ezto"
MINIO_USER="ezto"
MINIO_PASSWORD_FILE=""
SHORTENER_KEY_FILE=""
ENV_OUT=""
YES=false
NON_INTERACTIVE=false
PRINT_PLAN=false
UNINSTALL=false
OVERWRITE=false
# Elasticsearch 7 refuses to start below this; the kernel default is far lower
# on most distributions, so check it rather than let the container crash-loop.
VM_MAX_MAP_COUNT_MIN=262144

usage() {
  cat <<'USAGE'
Usage: scripts/deploy-compose.sh [options]

Deploys ezto Verify onto this host with Docker Compose. Service images are
pulled from a registry; nothing is built. The script generates a mode-600 .env
holding every credential and then runs `docker compose up -d`.

Options:
  --domain NAME                  Public domain served over HTTPS (required)
  --registry PREFIX              Registry prefix holding one repo per service
  --tag TAG                      Image tag deployed across every service
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
USAGE
}

need_arg() {
  [ "$#" -ge 2 ] || { echo "error: $1 requires a value" >&2; exit 2; }
}

while [ "$#" -gt 0 ]; do
  case "$1" in
    --domain) need_arg "$@"; DOMAIN="$2"; shift 2 ;;
    --registry) need_arg "$@"; REGISTRY="$2"; shift 2 ;;
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

# --- Uninstall handled before anything is resolved --------------------------
# Stopping the stack needs neither an image source nor credentials.
if [ "$UNINSTALL" = true ]; then
  if [ "$PRINT_PLAN" = true ]; then
    echo "Would run: docker compose -f $COMPOSE_FILE down"
    echo "Volumes (pgdata, miniodata, esdata) and the generated .env would be retained."
    exit 0
  fi
  command -v docker >/dev/null 2>&1 || { echo "error: docker is required" >&2; exit 3; }
  if [ "$YES" = false ]; then
    if [ "$NON_INTERACTIVE" = true ] || ! prompt_yes_no "Stop the stack? Volumes and .env are retained" false; then
      echo "Cancelled; no changes were made."
      exit 5
    fi
  fi
  ( cd "$REPO_ROOT" && docker compose -f "$COMPOSE_FILE" down )
  echo "Stack stopped. Volumes, .env, and certificates were intentionally retained."
  echo "To delete the data as well, run: docker compose down -v"
  exit 0
fi

# --- Interactive intake ------------------------------------------------------
if [ "$NON_INTERACTIVE" = false ]; then
  DOMAIN="$(prompt_value "Public domain (HTTPS)" "$DOMAIN")"
  if [ -z "$REGISTRY" ]; then
    echo "Service images are pulled, never built. Give the registry prefix holding" >&2
    echo "one repository per service (ezto-idp, ezto-background, ezto-socket," >&2
    echo "ezto-url-shortener, ezto-liveness)." >&2
  fi
  REGISTRY="$(prompt_value "Image registry prefix (e.g. docker.io/myorg)" "$REGISTRY")"
  TAG="$(prompt_value "Image tag" "$TAG")"
  TLS_MODE="$(prompt_value "TLS mode (self-signed|files)" "$TLS_MODE")"
  if prompt_yes_no "Run background jobs?" "$(csv_has background && echo true || echo false)"; then :; else
    SERVICES="$(printf '%s' "$SERVICES" | awk -v RS=, -v ORS=, '$0 != "background" && $0 != "" {print}' | sed 's/,$//')"
  fi
  if prompt_yes_no "Run the websocket service?" "$(csv_has socket && echo true || echo false)"; then :; else
    SERVICES="$(printf '%s' "$SERVICES" | awk -v RS=, -v ORS=, '$0 != "socket" && $0 != "" {print}' | sed 's/,$//')"
  fi
  if prompt_yes_no "Run the URL shortener?" "$(csv_has url-shortener && echo true || echo false)"; then :; else
    SERVICES="$(printf '%s' "$SERVICES" | awk -v RS=, -v ORS=, '$0 != "url-shortener" && $0 != "" {print}' | sed 's/,$//')"
  fi
  echo "Face liveness uses an image of roughly 15 GB and is skipped by default." >&2
  if prompt_yes_no "Enable face liveness?" false; then LIVENESS=true; fi
  ROOT_ACCESS_IP="$(prompt_value "Master-onboarding exact IPs (no CIDR)" "$ROOT_ACCESS_IP")"
fi

# --- Validation --------------------------------------------------------------
[ -n "$DOMAIN" ] || { echo "error: --domain is required" >&2; exit 2; }
case "$TLS_MODE" in self-signed|files) : ;; *) echo "error: --tls-mode must be 'self-signed' or 'files'" >&2; exit 2 ;; esac
case "$ROOT_ACCESS_IP" in ''|*/*|*[!0-9a-fA-F:.,]*) echo "error: root access IPs must be exact comma-separated IPs; CIDR is unsupported" >&2; exit 2 ;; esac
for service in $(printf '%s' "$SERVICES" | tr ',' ' '); do
  case "$service" in background|socket|url-shortener) : ;; *) echo "error: unsupported service '$service'" >&2; exit 2 ;; esac
done

[ -n "$MINIO_SERVER_URL" ] || MINIO_SERVER_URL="http://minio:9000"
[ -n "$MINIO_PUBLIC_URL" ] || MINIO_PUBLIC_URL="https://${DOMAIN}/storage/${MINIO_BUCKET}/public"

# An external MinIO cannot be bootstrapped by minio-init, so a browser-facing
# URL we invented from DOMAIN would be wrong.
if [ "$BUNDLE_MINIO" = false ] && [ -z "${MINIO_PUBLIC_URL_EXPLICIT:-}" ]; then
  case "$MINIO_PUBLIC_URL" in
    "https://${DOMAIN}/storage/${MINIO_BUCKET}/public")
      echo "error: --external-minio requires --minio-public-url; a browser-facing URL" >&2
      echo "       cannot be derived from the domain when object storage is external." >&2
      exit 2 ;;
  esac
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

# A URL for a service that is not running would make the IdP advertise it.
BACKGROUND_JOB_URL=""; SOCKET_URL=""; SHORTENER_HTTP_URL=""; SHORTENER_HTTPS_URL=""; LIVELINESS_URL=""
csv_has background && BACKGROUND_JOB_URL="http://background:8070"
csv_has socket && SOCKET_URL="http://socket:3001"
if csv_has url-shortener; then
  SHORTENER_HTTP_URL="http://url-shortener:3000"
  SHORTENER_HTTPS_URL="http://url-shortener:3000"
fi
[ "$LIVENESS" = true ] && LIVELINESS_URL="http://liveness:5000"

# --- Plan --------------------------------------------------------------------
if [ "$PRINT_PLAN" = true ]; then
  cat <<EOF
Domain:       $DOMAIN
Registry:     ${REGISTRY:-<none given>}
Tag:          $TAG
TLS mode:     $TLS_MODE
Services:     idp${SERVICES:+,$SERVICES}
Liveness:     $LIVENESS
Profiles:     $PROFILES
PostgreSQL:   $( [ "$BUNDLE_POSTGRES" = true ] && echo "bundled" || echo "external at $POSTGRES_HOST:$POSTGRES_PORT" )
MinIO:        $( [ "$BUNDLE_MINIO" = true ] && echo "bundled" || echo "external at $MINIO_SERVER_URL" )
Elasticsearch:$( [ "$BUNDLE_ELASTICSEARCH" = true ] && echo " bundled" || echo " external at $ES_HOST:$ES_PORT" )
Public objects: $MINIO_PUBLIC_URL
Onboarding IPs: $ROOT_ACCESS_IP
Env file:     $ENV_OUT (would be generated at mode 600, credentials redacted here)
Command:      docker compose -f $COMPOSE_FILE up -d
EOF
  exit 0
fi

# --- Host preconditions ------------------------------------------------------
command -v docker >/dev/null 2>&1 || { echo "error: docker is required" >&2; exit 3; }
COMPOSE_VERSION="$(docker compose version --short 2>/dev/null || true)"
[ -n "$COMPOSE_VERSION" ] || { echo "error: the Docker Compose plugin is required" >&2; exit 3; }
# `depends_on: required: false` was introduced in Compose 2.20.0.
if [ "$(printf '%s\n2.20.0\n' "$COMPOSE_VERSION" | sort -V | head -n1)" != "2.20.0" ]; then
  echo "error: Docker Compose >= 2.20.0 is required (found $COMPOSE_VERSION)." >&2
  echo "       Optional-dependency handling ('required: false') needs it." >&2
  exit 3
fi

[ -n "$REGISTRY" ] || { echo "error: --registry is required; images are pulled, never built" >&2; exit 2; }

if [ "$BUNDLE_ELASTICSEARCH" = true ]; then
  CURRENT_MAX_MAP="$(sysctl -n vm.max_map_count 2>/dev/null || echo 0)"
  if [ "$CURRENT_MAX_MAP" -lt "$VM_MAX_MAP_COUNT_MIN" ]; then
    echo "error: the bundled Elasticsearch needs vm.max_map_count >= $VM_MAX_MAP_COUNT_MIN (found $CURRENT_MAX_MAP)." >&2
    echo "       Set it now:       sudo sysctl -w vm.max_map_count=$VM_MAX_MAP_COUNT_MIN" >&2
    echo "       Persist it:       echo 'vm.max_map_count=$VM_MAX_MAP_COUNT_MIN' | sudo tee /etc/sysctl.d/99-ezto-verify.conf" >&2
    echo "       Or use your own cluster with --external-elasticsearch HOST:PORT." >&2
    exit 3
  fi
fi

# --- First install vs. adoption ---------------------------------------------
# A data volume that already exists was written under the old credentials.
# Generating new ones would leave the stack unable to open its own data.
VOLUMES_EXIST=false
for vol in pgdata miniodata esdata; do
  if docker volume inspect "$(basename "$REPO_ROOT")_${vol}" >/dev/null 2>&1; then
    VOLUMES_EXIST=true
  fi
done

if [ "$VOLUMES_EXIST" = true ] && [ ! -f "$ENV_OUT" ]; then
  echo "error: this host already holds ezto Verify data volumes, but $ENV_OUT is missing." >&2
  echo "       Those volumes were written under the previous credentials, so generating" >&2
  echo "       new ones would leave the stack unable to open its own data." >&2
  echo "       Supply the existing values:" >&2
  echo "         --postgres-password-file FILE" >&2
  echo "         --minio-password-file FILE" >&2
  echo "         --admin-password-file FILE" >&2
  echo "         --shortener-key-file FILE" >&2
  echo "       A stack deployed from an older revision of this repository used the" >&2
  echo "       literals admin/admin and minioadmin/minioadmin123; see" >&2
  echo "       docs/deployment-compose.md 'Adopting an existing stack'." >&2
  if [ -z "$POSTGRES_PASSWORD_FILE" ] || [ -z "$MINIO_PASSWORD_FILE" ]; then
    exit 4
  fi
fi

if [ -e "$ENV_OUT" ] && [ "$OVERWRITE" = false ]; then
  echo "error: $ENV_OUT already exists (use --overwrite, or --env-out to choose another path)" >&2
  exit 2
fi

# --- Credentials -------------------------------------------------------------
ADMIN_PASSWORD="$(read_secret "$ADMIN_PASSWORD_FILE")"
POSTGRES_PASSWORD="$(read_secret "$POSTGRES_PASSWORD_FILE")"
MINIO_PASSWORD="$(read_secret "$MINIO_PASSWORD_FILE")"
SHORTENER_KEY="$(read_secret "$SHORTENER_KEY_FILE")"

umask 077
cat > "$ENV_OUT" <<EOF
# Generated by scripts/deploy-compose.sh on $(date -u '+%Y-%m-%dT%H:%M:%SZ').
# Mode 600. Holds every credential the stack uses — never commit it.
DOMAIN=$DOMAIN
REGISTRY=$REGISTRY
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

# --- External-store preconditions Compose cannot satisfy --------------------
# init-databases.sql is mounted into the BUNDLED container's initdb directory
# and never runs against an external server.
if [ "$BUNDLE_POSTGRES" = false ]; then
  echo "External PostgreSQL selected. These databases must already exist and be owned by '$POSTGRES_USER':" >&2
  echo "  eztoverify  jobrnr  urlshortner  socket" >&2
  echo "  (the 'urlshortner' spelling is intentional; it matches init-databases.sql)" >&2
  if command -v psql >/dev/null 2>&1; then
    for db in eztoverify jobrnr urlshortner socket; do
      if ! PGPASSWORD="$POSTGRES_PASSWORD" psql -h "$POSTGRES_HOST" -p "$POSTGRES_PORT" -U "$POSTGRES_USER" -d "$db" -c 'SELECT 1' >/dev/null 2>&1; then
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
if [ "$TLS_MODE" = self-signed ]; then
  if [ ! -s "$REPO_ROOT/nginx/certs/fullchain.pem" ] || [ ! -s "$REPO_ROOT/nginx/certs/privkey.pem" ]; then
    echo "Generating a self-signed certificate for $DOMAIN; browsers will warn." >&2
    DOMAIN="$DOMAIN" "$REPO_ROOT/nginx/gen-self-signed.sh"
  fi
else
  if [ ! -s "$REPO_ROOT/nginx/certs/fullchain.pem" ] || [ ! -s "$REPO_ROOT/nginx/certs/privkey.pem" ]; then
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

echo
echo "Deployed. The IdP is at https://$DOMAIN/auth"
echo "Admin console: https://$DOMAIN/auth/admin/  (user: $ADMIN_USER)"
echo "The admin password is in $ENV_OUT as KEYCLOAK_ADMIN_PASSWORD."
echo
echo "Master-tenant onboarding is restricted to: $ROOT_ACCESS_IP"
echo "If your browser reaches this host from another address, add it to"
echo "ROOT_ACCESS_IP in $ENV_OUT and run: docker compose up -d idp"
echo
echo "Operations, upgrade, rollback, and restore: docs/deployment-compose.md"
```

Make it executable:

```bash
chmod +x scripts/deploy-compose.sh
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./scripts/compose/test-config.sh`
Expected: PASS on all five deployer assertions.

- [ ] **Step 5: Commit**

```bash
git add scripts/deploy-compose.sh scripts/compose/test-config.sh
git commit -m "feat(compose): add the guided single-host deployer"
```

---

### Task 7: Operations documentation

The spec is explicit that backup tooling is not shipped. What replaces it is a document stating exactly which volumes hold what, and what a restore must do.

**Files:**
- Create: `docs/deployment-compose.md`
- Modify: `README.md` (the Quick Start and Kubernetes sections)

**Interfaces:**
- Consumes: the flags and profile names from Tasks 4 and 6.
- Produces: nothing consumed by later tasks.

- [ ] **Step 1: Write `docs/deployment-compose.md`**

Create the file with these sections, each stating the real limitation rather than working around it:

```markdown
# Deploying ezto Verify with Docker Compose

This is the single-host production path. For Kubernetes, see
[`charts/ezto-verify`](../charts/ezto-verify) and `scripts/install.sh`.

## Prerequisites

- Docker Engine with the Compose plugin, **Compose >= 2.20.0**. Optional-dependency
  handling (`depends_on: required: false`) needs it, and the deployer refuses to
  run below it.
- Published service images reachable from this host: `ezto-idp`, `ezto-background`,
  `ezto-socket`, `ezto-url-shortener`, and `ezto-liveness` under one registry
  prefix. This repository builds none of them and ships no pipeline that does.
- A domain resolving to this host, unless you accept a self-signed certificate.
- For the bundled Elasticsearch: `vm.max_map_count` at or above `262144`.

      sudo sysctl -w vm.max_map_count=262144
      echo 'vm.max_map_count=262144' | sudo tee /etc/sysctl.d/99-ezto-verify.conf

  The deployer checks this and refuses with that remedy rather than letting
  Elasticsearch crash-loop.

## Deploying

    ./scripts/deploy-compose.sh --domain verify.example.com --registry docker.io/myorg --tag 1.0.0

Run it with `--print-plan` first to see what it would do; that mode makes no
external call, writes no `.env`, and generates no certificate.

## What is exposed

Only nginx publishes ports, `80` and `443`. PostgreSQL, MinIO, Elasticsearch,
`background`, `socket`, and `url-shortener` reach each other over the compose
network and publish nothing. Verify it after every deploy:

    docker compose ps --format 'table {{.Service}}\t{{.Ports}}'

Anything but nginx showing a `0.0.0.0:` binding is a misconfiguration. Where a
port genuinely must be reachable for debugging, bind it to loopback explicitly
(`127.0.0.1:5432:5432`) and remove it again afterwards.

## Volumes

| Volume | Holds | Losing it means |
|---|---|---|
| `pgdata` | Identity, configuration, and jobs | Total loss of tenants and users |
| `miniodata` | Uploaded documents | Loss of every verification document |
| `esdata` | Events and audit records | Loss of audit history — this is primary data read by the IdP, not a rebuildable index |

A restore must take all three from the same point in time, with the stack down.
No backup script ships with this repository: your own tooling does the work, and
we will not ship an untested restore.

## Upgrade

Set `TAG` in `.env`, then:

    docker compose pull
    docker compose up -d

## Rollback

Set the previous `TAG` and `docker compose up -d`. Keycloak and service
migrations are forward-only: an older tag does not undo a schema change. Rolling
back across a migration requires restoring data from before the upgrade. This is
a stated limitation, not something the tooling works around.

## TLS

`TLS_MODE=self-signed` regenerates a certificate with `nginx/gen-self-signed.sh`
and browsers will warn. `TLS_MODE=files` means you own renewal: renewing is
replacing `fullchain.pem` and `privkey.pem` in `nginx/certs` and reloading nginx
(`docker compose exec nginx nginx -s reload`). ACME and automatic renewal are
deliberately out of scope for v1.

## Onboarding is IP-restricted

`ROOT_ACCESS_IP` is an exact comma-separated IP list with no CIDR support.
Behind nginx it is matched against the `X-Real-IP` header, which is the real
address of the browser doing the onboarding — not the host's own address. If
onboarding is rejected, read the address nginx recorded and add it:

    docker compose logs nginx | tail -5

Then add it to `ROOT_ACCESS_IP` in `.env` and run `docker compose up -d idp`.

## External stores

Dropping an `infra-*` profile from `COMPOSE_PROFILES` is a configuration change,
but each external store carries a precondition the deployer cannot satisfy for
you.

**PostgreSQL.** `init-databases.sql` is mounted into the *bundled* container's
`docker-entrypoint-initdb.d` and never runs against an external server. These
four databases must already exist, owned by the configured user:

    CREATE DATABASE "eztoverify";   -- idp (Keycloak fork) datastore
    CREATE DATABASE "jobrnr";       -- background (jobrunr) service
    CREATE DATABASE "urlshortner";  -- url-shortener service
    CREATE DATABASE "socket";       -- socket service

The `urlshortner` spelling is intentional and must be reproduced exactly. The
deployer verifies all four are reachable before starting the stack.

**MinIO.** `minio-init` bootstraps the bucket and belongs to `infra-minio`. An
external endpoint needs the bucket created, its `public` prefix set to allow
anonymous download, and `MINIO_PUBLIC_URL` pointed at a URL an end user's
browser can actually reach.

**Elasticsearch.** `ES_HOST` and `ES_PORT` must point at a reachable 7.x
endpoint. The bundled one runs with security disabled; a real external cluster
usually does not, and credential support for it is a gap this version records
rather than solves.

## Adopting an existing stack

If this host already holds `pgdata`, `miniodata`, or `esdata` but has no `.env`,
the deployer refuses to generate credentials: those volumes were written under
the old values, and new ones would leave the stack unable to open its own data.
Supply the existing values instead:

    ./scripts/deploy-compose.sh --domain ... --registry ... --tag ... \
      --postgres-password-file ./pg.txt \
      --minio-password-file ./minio.txt \
      --admin-password-file ./admin.txt \
      --shortener-key-file ./shortener.txt

A stack deployed from an older revision of this repository used the literals
`admin`/`admin` for PostgreSQL and `minioadmin`/`minioadmin123` for MinIO.

Actually *rotating* a credential is a separate procedure, not something `.env`
can do:

    # PostgreSQL
    docker compose exec postgres psql -U <user> -c "ALTER USER <user> WITH PASSWORD '<new>';"
    # MinIO
    docker compose exec minio mc admin user svcacct ...

Update `.env` to match and restart the dependent services.

## Known limitations

- Secrets stay readable via `docker inspect` and in each container's
  environment. The services read plain environment variables and support no
  `*_FILE` convention; changing that requires application changes in four
  services and was ruled out for v1 with that cost understood.
- The application uses MinIO **root** credentials as its access key. A leaked
  `.env` or a compromised IdP therefore yields full MinIO root access rather
  than scoped bucket access. A dedicated application key pair is follow-up work.
- No ACME or automatic certificate renewal.
- No backup or restore tooling.
- No multi-host, HA, or orchestrated deployment.
```

- [ ] **Step 2: Update `README.md`**

Replace the Quick Start body (currently `docker compose up --build` and a
"Replace every REPLACE_ME" warning) so the deployment path and the contributor
path are no longer the same command:

```markdown
## Quick Start (Docker Compose)

**Deploying** — images are pulled, nothing is built:

```bash
./scripts/deploy-compose.sh --domain verify.example.com --registry docker.io/myorg --tag 1.0.0
```

The deployer generates a mode-600 `.env` holding every credential, checks the
host preconditions, and starts the stack. Run it with `--print-plan` first to
see what it would do without changing anything. Full operations guide:
[docs/deployment-compose.md](./docs/deployment-compose.md).

**Contributing** — build every service from source:

```bash
cp .env.example .env    # then fill in DOMAIN, REGISTRY, TAG and credentials
docker compose -f docker-compose.yml -f docker-compose.build.yml up --build
```

For plain-HTTP local development against containerised infra only, use
`docker-compose.dev.yml`.
```

Remove the line `> **Replace every `REPLACE_ME` / `change_me_*` value in `docker-compose.yml`...**` — no such value remains in the file.

Update the port table and any text claiming PostgreSQL, MinIO, Elasticsearch,
`background`, `socket`, or `url-shortener` are reachable on the host: only
nginx's 80 and 443 are.

- [ ] **Step 3: Verify the documented commands match the implementation**

Run: `./scripts/deploy-compose.sh --help`
Expected: every flag used in `docs/deployment-compose.md` and `README.md` appears in the help output.

Run: `grep -n 'docker compose' docs/deployment-compose.md README.md`
Expected: no remaining `docker compose up --build` presented as a deployment command.

- [ ] **Step 4: Run the full test suite**

Run: `./scripts/compose/test-config.sh`
Expected: `All compose config assertions passed.`

- [ ] **Step 5: Commit**

```bash
git add docs/deployment-compose.md README.md
git commit -m "docs(compose): document the production deployment and its limits"
```

---

### Task 8: Codex review and remediation

The captain asked for an independent review of the finished change before it is
treated as production ready. This task is the review and the fixes it produces,
not a rubber stamp.

**Files:**
- Modify: whatever the review findings require.

**Interfaces:**
- Consumes: every file produced by Tasks 1–7.
- Produces: the final reviewed state.

- [ ] **Step 1: Produce the review input**

Run: `git diff main --stat && git diff main`
Expected: the complete change set across `docker-compose.yml`, `docker-compose.build.yml`, `.env.example`, `scripts/deploy-compose.sh`, `scripts/compose/test-config.sh`, `docs/deployment-compose.md`, and `README.md`.

- [ ] **Step 2: Dispatch the Codex review**

Use the `codex:rescue` skill with an explicit review brief covering:

- Whether any credential can still reach the tracked tree or a container's logs.
- Whether any service other than nginx can bind a host interface under any
  profile combination.
- Whether `depends_on: required: false` actually holds for every store, and what
  happens on Compose 2.20.0 exactly.
- Whether `${VAR:?}` genuinely fails closed for every required value, including
  inside `entrypoint` strings where `$$` escaping applies.
- Whether the adoption path can destroy data on a host that already holds
  volumes.
- Whether the `.env` generation can produce a value that breaks Compose
  interpolation (a `$` or newline inside a generated secret).
- Whether the assertions in `test-config.sh` can pass while the property they
  claim to test is false.

- [ ] **Step 3: Triage the findings**

For each finding, decide and record: fix now, file as follow-up, or reject with
a stated reason. A finding that contradicts a decision recorded in the spec is
rejected with a pointer to that decision, not silently implemented.

- [ ] **Step 4: Apply the accepted fixes**

Add a regression assertion to `scripts/compose/test-config.sh` for every accepted
correctness finding, so the same defect cannot return.

- [ ] **Step 5: Run the full test suite**

Run: `./scripts/compose/test-config.sh`
Expected: `All compose config assertions passed.`

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "fix(compose): apply review findings from the deployment hardening review"
```

---

## Self-Review

**Spec coverage.** Every row of the spec's Files table maps to a task:
`docker-compose.yml` → Tasks 1–5; `docker-compose.build.yml` → Task 2;
`docker-compose.dev.yml` → unchanged, stated in Global Constraints;
`.env.example` → Task 1; `scripts/deploy-compose.sh` → Task 6;
`scripts/compose/test-config.sh` → Tasks 1–6; `docs/deployment-compose.md` →
Task 7; `README.md` → Task 7. Every Testing bullet in the spec has a matching
assertion. Every Out-of-scope item is recorded in `docs/deployment-compose.md`
"Known limitations" rather than implemented.

**Gap found and closed.** The spec's `.env` contract does not list
`BACKGROUND_JOB_URL`, `SOCKET_URL`, `SHORTENER_HTTP_URL`, `SHORTENER_HTTPS_URL`,
or `LIVELINESS_URL`, but its Profiles section requires the deployer to write
them as empty when a service is off. Compose cannot make a value conditional on
a profile, so they must be `.env` variables. Task 1 adds them to `.env.example`
and Task 6 has the deployer derive them from the selected services.

**Gap found and closed.** The spec does not say where `MINIO_BUCKET` lives, but
`minio-init` needs it and an external MinIO needs it stated. Added to the `.env`
contract in Task 1.

**Naming consistency.** `run_config`, `pass`, `fail`, `full_env`, and
`ALL_PROFILES` are defined in Task 1 and used unchanged in Tasks 2–6.
`KEYCLOAK_ADMIN` (never `KEYCLOAK_ADMIN_USER`), `ES_HOST`/`ES_PORT` (never a
URL), and the `urlshortner` database spelling are consistent across the compose
file, the deployer, and the documentation.
