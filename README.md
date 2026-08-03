<p align="center">
  <picture>
    <source media="(prefers-color-scheme: dark)" srcset="docs/verify/static/img/brand-name-dark.svg">
    <img alt="ezto Verify" src="docs/verify/static/img/brand-name.svg" width="300">
  </picture>
</p>

# ezto Verify

[![License: AGPL v3](https://img.shields.io/badge/License-AGPL_v3-blue.svg)](LICENSE)

An open-source identity verification and authentication platform. ezto Verify is a forked **Keycloak 18** (Quarkus) identity core extended with verification workflows — CIBA push-auth, OTP, biometric and face-liveness, FIDO/passkey, and document checks — plus a Flutter authenticator app and supporting microservices for self-hosting.

## Architecture

ezto Verify follows a microservices architecture. Unlike a stock Keycloak deployment, the **idp** here *is* the Keycloak fork (with verification SPIs, themes and CIBA built in) — it is a first-party service, not external infrastructure.

| Service | Port | Description | Stack |
|---------|------|-------------|-------|
| [idp](./idp) | 8080 (internal; served via nginx 443) | Verification & auth core — Keycloak 18 fork with verification SPIs, custom themes, and CIBA push-auth | Java 11, Quarkus, Maven |
| [services/background](./services/background) | 8070 | Background job runner — scheduled jobs and async processing | Java 21, Spring Boot, JobRunr |
| [services/socket](./services/socket) | 3001 | Real-time push channel for web/mobile SDK delivery | Node.js, Socket.io |
| [services/url-shortener](./services/url-shortener) | 3000 | Short-link service for verification links (with TTL) | Node.js, Express, Prisma |
| [services/liveness](./services/liveness) | 5000 | Face-liveness / face-match inference (opt-in) | Python, Flask |
| [authenticator](./authenticator) | — | Companion mobile app: push approval, biometric, device binding, FIDO/passkey | Flutter / Dart |
| [docs/verify](./docs/verify) | 3000 | Product documentation site | Docusaurus 2 |
| [scripts](./scripts) | — | Operational helpers (root-realm push / FIDO seeding) | Bash |

## Service Dependency Map

```
                    ┌──────────────┐   ┌──────────────┐   ┌──────────────────┐
                    │  PostgreSQL  │   │    MinIO     │   │  Elasticsearch   │
                    │   (5432)     │   │ (9000/9001)  │   │     (9200)       │
                    └──────┬───────┘   └──────┬───────┘   └────────┬─────────┘
        ┌───────────┬──────┴────┬──────┐      │                    │
        │           │           │      │      │                    │
        v           v           v      v      v                    v
  ┌──────────┐ ┌─────────┐ ┌──────────┐ ┌────────────────────────────────────┐
  │background│ │ socket  │ │   url-   │ │             idp (8080)              │
  │  (8070)  │ │ (3001)  │ │ shortener│ │   Keycloak 18 fork + verify SPIs    │
  └────┬─────┘ └────┬────┘ │  (3000)  │ │   (events/audit -> Elasticsearch)   │
       │            │      └──────────┘ └──┬────────────────┬─────────────────┘
       │            │                      │ calls          │ calls
       └────────────┴──────────────────────┤                v
              idp calls these              │          ┌────────────┐
              (jobs, push, links)          │          │ liveness   │ ◄── MinIO
                                           │          │  (5000)    │     (selfie/doc)
                                           v          └────────────┘
                                 ┌──────────────────┐
                                 │  authenticator   │  enrolls against idp;
                                 │  push/FIDO/bio    │  approvals via socket + FCM
                                 └──────────────────┘

  Dev only:  MailDev SMTP catcher (1025 / 1080 UI)
  Compose:   nginx TLS reverse proxy (80/443) fronts the idp — the only published entry point
```

## Prerequisites

### Required Software

| Tool | Version | Used By |
|------|---------|---------|
| Docker & Docker Compose | Latest | All services (recommended path) |
| Java JDK | 11 (idp), 21 (background) | idp, background — JDK 11 is a hard constraint for the idp build |
| Node.js | 18+ | socket, url-shortener, docs site |
| Python | 3.10+ | liveness |
| Maven | 3.8+ | Java services |
| Flutter | 3.4x | authenticator (optional — only to build the app) |

### Required Infrastructure

| Service | Port | Purpose |
|---------|------|---------|
| PostgreSQL 14+ | 5432 | Primary datastore (databases: `eztoverify`, `jobrnr`, `urlshortner`, `socket`) |
| MinIO | 9000 / 9001 | S3-compatible object storage for selfie / document / biometric files |
| Elasticsearch 7 | 9200 | Event / audit / transaction store (read by the idp) |

> **Email (dev only):** a MailDev SMTP catcher (1025 / 1080 UI) ships in `docker-compose.dev.yml` for local email-flow testing. Not part of the core stack — replace with a real provider (configured per-tenant in the onboarding wizard) in production.

## Quick Start (Docker Compose)

The fastest path. The full core stack is **built from source** on each run (no prebuilt images shipped) and served **over HTTPS** behind the bundled nginx TLS proxy. One-time setup — the stack refuses to start without a domain and certs:

```bash
# 1. Domain
cp .env.example .env                                   # set DOMAIN=verify.example.com

# 2. TLS certs in nginx/certs/ (fullchain.pem + privkey.pem) — real CA cert,
#    or a throwaway self-signed one for testing:
DOMAIN=verify.example.com ./nginx/gen-self-signed.sh

# 3. Point the domain at this host (DNS A record, or /etc/hosts -> 127.0.0.1 locally)

# Build + start everything (infra + idp + services + nginx)
docker compose up --build

# Stop / wipe:
docker compose down            # stop
docker compose down -v         # stop + wipe volumes
```

The idp comes up at **https://\<your-domain\>/auth** (admin console `/auth/admin/`, default `admin` / `admin` — change before any non-local use). Missing `DOMAIN` fails at compose parse; missing certs fail the `certs-check` one-shot with instructions. The idp build is heavy (compiles Keycloak + verification modules + themes); give Docker ample RAM/disk.

> Replace every `REPLACE_ME` / `change_me_*` value in `docker-compose.yml` before any non-local use.

For plain-HTTP local development, use `docker-compose.dev.yml` (infra only — see Step 1 below) and run the app code locally.

## Step-by-Step Deployment Guide

Use this for running services individually (development) or as a reference for production wiring.

### Step 1: Infrastructure Services

Start the shared infra (PostgreSQL, MinIO, Elasticsearch, and the dev MailDev) with the dev compose file:

```bash
docker compose -f docker-compose.dev.yml up -d
```

This creates the required databases (`eztoverify`, `jobrnr`, `urlshortner`, `socket`) and the MinIO bucket. To stand the infra up by hand instead:

```bash
# PostgreSQL
docker run -d --name ezto-postgres \
  -e POSTGRES_USER=admin -e POSTGRES_PASSWORD=admin \
  -p 5432:5432 -v pgdata:/var/lib/postgresql/data \
  postgres:14-alpine
# then create the databases listed above

# MinIO
docker run -d --name ezto-minio \
  -e MINIO_ROOT_USER=minioadmin -e MINIO_ROOT_PASSWORD=minioadmin123 \
  -p 9000:9000 -p 9001:9001 -v miniodata:/data \
  minio/minio server /data --console-address ":9001"
# create a bucket: ezto-verify (public download on /public)

# Elasticsearch
docker run -d --name ezto-elasticsearch \
  -e discovery.type=single-node -e xpack.security.enabled=false \
  -p 9200:9200 docker.elastic.co/elasticsearch/elasticsearch:7.17.18
```

### Step 2: idp (Verification & Auth Core)

The idp is the Keycloak 18 fork. Build it from source via Docker (recommended — pins JDK 11):

```bash
docker compose up --build idp
# or build the image standalone:
docker build -t ezto-idp ./idp
```

Key environment (see `idp/.env.example` for the full list):

```
KC_DB_URL_HOST / KC_DB_USERNAME / KC_DB_PASSWORD   # PostgreSQL
KC_HOSTNAME_URL=http://localhost:8080/auth         # public base URL
KEYCLOAK_ADMIN / KEYCLOAK_ADMIN_PASSWORD           # master-realm admin (first boot)
DOCS_URL / DOCS_LINK                               # self-hosted docs links (empty = bundled fallback)
```

Verify: http://localhost:8080/auth/admin/

> **Master-tenant onboarding is IP-gated.** Creating the first master-realm admin
> (the welcome-page "create admin" step) is only allowed from IPs listed in
> `ROOT_ACCESS_IP`. This gate applies **only to the master realm** and is never
> reached again once onboarding completes.
> - `ROOT_ACCESS_IP` **must be set** — an empty/unset value throws an NPE (HTTP 500
>   on the welcome page). Matching is exact per IP; **no CIDR ranges**.
> - The compose stack fronts the idp with nginx (`KC_PROXY=edge`), so the gate
>   compares the `X-Real-IP` header. **Browser on the docker host:** works out of
>   the box — nginx sees the docker gateway as the client, which is already
>   whitelisted (`127.0.0.1,0.0.0.0,::1,172.20.0.1,192.168.65.1`).
> - **Remote browser:** arrives with its **real public IP**, which is *not* in the
>   default list. Add it to `ROOT_ACCESS_IP` or master onboarding will be blocked.

### Step 3: Supporting Services

Start in any order once infra is up.

#### 3.1 services/background (Job Runner)

```bash
cd services/background
cp .env.example .env   # set JOB_DB_URL / JOB_DB_USER / JOB_DB_PASSWORD
mvn clean install -DskipTests
mvn spring-boot:run
```
Verify: http://localhost:8070

#### 3.2 services/socket (Real-time Push)

```bash
cd services/socket
cp .env.example .env   # PostgreSQL (socket DB) credentials
npm install
npm start
```
Verify: service on port 3001

#### 3.3 services/url-shortener (Short Links)

```bash
cd services/url-shortener
cp .env.example .env   # DATABASE_URL=postgresql://admin:admin@localhost:5432/urlshortner?schema=public
                       # VALID_API_KEYS=your-generated-api-key
npm install
npx prisma migrate deploy
npm start
```
Verify: http://localhost:3000

#### 3.4 services/liveness (Face Liveness — optional)

```bash
cd services/liveness
cp .env.example .env   # MINIO_HOST / MINIO_ACCESS_KEY / MINIO_SECRET
pip3 install -r requirements.txt
python3 app.py
```
Verify: http://localhost:5000

Via compose this service is opt-in: `docker compose --profile face-biometrics up -d`.

### Step 4: authenticator (Mobile App)

The Flutter authenticator approves push/CIBA requests, does on-device biometrics and FIDO/passkey. It uses a flavor/branding generator and per-flavor Firebase config. See [authenticator/README.md](./authenticator/README.md) for build, branding, and the "published app vs your own build" guidance.

After building and signing the app, seed the root-realm push & FIDO attributes so they match the build (see below).

### Step 5: docs/verify (Documentation Site)

The docs site explains each component and the verification flows.

```bash
cd docs/verify
npm install
npm start -- --port 4001     # default 3000 collides with the url-shortener
```
Open http://localhost:4001. Static build: `npm run build && npm run serve`.

## Seeding push & FIDO config (root realm)

Firebase push and the FIDO app-association payloads live as **attributes on the root tenant** (the `master` realm). Seed them via the admin REST API:

```bash
KC_ADMIN_PASSWORD=<admin-pw> ./scripts/seed-realm-attributes.sh \
    --firebase     ./scripts/seed-data/firebase-sa.json \
    --fido-ios     ./scripts/seed-data/apple-app-site-association.json \
    --fido-android ./scripts/seed-data/assetlinks.json
```

Each flag is optional; re-running overwrites only those keys. Needs `curl` + `jq`. The default `KC_BASE=http://localhost:8080/auth` fits a locally-run idp (dev setup); for the compose stack set `KC_BASE=https://<your-domain>/auth` (realm `master`, user `admin` by default).

> `firebase-json` is stored as a plaintext realm attribute (it lands in the DB and any realm export) — never commit realm exports of `master` to git.

## Custom Domain + HTTPS

HTTPS is built into `docker-compose.yml` — the bundled nginx terminates TLS for
`https://<your-domain>/auth` and is the stack's only published entry point. Setup
(domain, certs, renewals, exposing extra services on the domain) is covered in the
[Quick Start](#quick-start-docker-compose) and [nginx/README.md](./nginx/README.md).

> **Note:** master-tenant onboarding sees the `X-Real-IP` the proxy forwards. A
> remote browser's real IP must be added to `ROOT_ACCESS_IP` before onboarding
> or the "create master admin" step is blocked — see Step 2.

## Kubernetes (Helm)

Docker Compose above is the fastest way to run everything on one host; a
Helm chart at [`charts/ezto-verify`](./charts/ezto-verify) deploys the same
services to a Kubernetes cluster instead. Quick local flow:

```bash
kind create cluster --name ezto
./scripts/helm/build-and-load.sh kind ezto
helm install ezto charts/ezto-verify -f charts/ezto-verify/values-kind.yaml
kubectl port-forward svc/ezto-ezto-verify-idp 8080:8080
# browse http://localhost:8080/auth
```

A bare `helm install` with the chart's default values fails on purpose —
Elasticsearch is external-by-default and `required`-guarded; the
`values-kind.yaml`/`values-minikube.yaml` overlays used above bundle it for
local use, and production deploys must point at a real cluster. See the
[chart README](./charts/ezto-verify/README.md) for the full values
reference, the production (registry images, ingress + cert-manager)
flow, the `idp` single-replica scaling note, and `ROOT_ACCESS_IP` onboarding
details for the Kubernetes ingress path.

## Production Deployment

For production, ensure:
- TLS termination at the edge (the bundled nginx, or your own ingress/load balancer with `KC_PROXY=edge`).
- Secrets managed via environment / a vault (e.g. HashiCorp Vault) — never commit real credentials.
- Persistent volumes for PostgreSQL, MinIO and Elasticsearch.
- A real email provider (configured per-realm), not MailDev.
- Resource limits and autoscaling; restricted inter-service network policies.

## Environment Configuration

Each service includes a `.env.example` documenting the required variables. To get started:

```bash
cp .env.example .env
# edit .env with your values
```

> **Important:** Never commit actual credentials. The `.env.example` files contain only placeholders and are safe to commit.

## Troubleshooting

Common self-hosting issues and where they're covered in full:

| Symptom | Cause | Fix |
|---------|-------|-----|
| Welcome page returns **HTTP 500**; can't reach onboarding | `ROOT_ACCESS_IP` unset/empty → NPE in the master-realm gate | Set `ROOT_ACCESS_IP` (see [Step 2](#step-2-idp-verification--auth-core)). Never leave it blank. |
| **"Create master admin" / onboarding blocked** (page loads, submit rejected) | Your client IP isn't in `ROOT_ACCESS_IP`. Behind the proxy the gate sees your real browser IP (`X-Real-IP`); direct compose sees the docker gateway | Add the right IP to `ROOT_ACCESS_IP` — [Step 2](#step-2-idp-verification--auth-core) / [Custom Domain](#custom-domain--https-optional). Exact IPs only, no CIDR. |
| Master onboarding never re-prompts after first setup | The gate short-circuits once onboarding is complete (by design) | Not an error — reset onboarding state only if you intend to re-run it. |
| **"HTTPS required"** on login/welcome | Master realm enforces `sslRequired=external` (the secure default, pinned by the `idp-init` one-shot) and the request reached the idp as plain HTTP from a non-local IP — e.g. the idp was accessed directly instead of through the nginx proxy | Use `https://<your-domain>/auth` through the bundled proxy. For a throwaway plain-HTTP experiment only: set `MASTER_SSL_REQUIRED=none` on `idp-init` and re-run it, or `docker exec ezto-postgres psql -U admin -d eztoverify -c "update realm set ssl_required='NONE' where name='master';"` then `docker compose restart idp`. Never in production. |

## Hosting & Support

For managed hosting, enterprise support, or deployment assistance, contact the ezto team:

- **Website:** [ezto.io](https://ezto.io)
- **Email:** lokesh.ravichandru@ezto.io

## License

This project is licensed under the GNU Affero General Public License v3.0 (AGPL-3.0) — see the [LICENSE](./LICENSE) file for details. AGPL-3.0 is network copyleft: running a modified version as a service requires releasing the source to its users. The `idp/keycloak/` tree is a fork of Keycloak (Apache-2.0); see [NOTICE](./NOTICE) for upstream attribution.
