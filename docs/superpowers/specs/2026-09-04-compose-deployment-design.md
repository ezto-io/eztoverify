# Docker Compose deployment strategy

Date: 2026-09-04
Status: approved design, not yet implemented

## Problem

`docker-compose.yml` is a contributor stack presented as a deployment path. Deploying it today means:

- **Building every service on the host.** `idp`, `background`, `socket`, and `url-shortener` all carry `build:` contexts. A deployer needs a full toolchain, and `liveness` is a ~15 GB build.
- **Editing a tracked file to set secrets.** `POSTGRES_PASSWORD: admin`, `MINIO_ROOT_PASSWORD: minioadmin123`, and `SHORTENER_KEY: change_me_key` are literals inside `docker-compose.yml`, so credentials land in the working tree and every `git status` is dirty.
- **No way to point at existing infrastructure.** `KC_DB_URL_HOST: postgres`, `MINIO_SERVER_URL: http://minio:9000`, and the Elasticsearch host are hardcoded to internal service names.
- **No service selection.** `background`, `socket`, and `url-shortener` always run, while the Helm chart now makes each one independently optional.
- **Every internal port is published on all interfaces.** `postgres` publishes 5432, `minio` 9000 and 9001, `elasticsearch` 9200, `background` 8070, `socket` 3001, and `url-shortener` 3000. Elasticsearch also runs with `xpack.security.enabled: "false"`. On a host with a public address this exposes an unauthenticated event/audit store, the database, and object storage to the internet. Only nginx's 80 and 443 belong on a public interface.
- **`MINIO_PUBLIC_URL` is `http://localhost:9000/ezto-verify/public`**, which is unreachable for any real user and plain HTTP besides.

Meanwhile the Helm chart (PR #4) moved to operator-supplied registry images, an `enabled` flag per optional service, and a guided installer. Compose diverged from it.

## Decisions

Each of these was decided explicitly; the rationale matters more than the choice.

| Decision | Choice | Rationale |
|---|---|---|
| Image source | Registry images; `build:` moves to a contributor overlay | A deployer needs no toolchain and no 15 GB build; the deployed version becomes a tag |
| Target environment | Production single host with real tenants | Drives credential generation, pinned tags, restart behavior, documented restore |
| TLS at v1 | Self-signed automated; bring-your-own certs work by dropping files in `nginx/certs` | Bringing certs is already how nginx reads them, so it needs no new machinery |
| Script shape | Standalone `scripts/deploy-compose.sh`, no shared library | Each script reads top-to-bottom; accepted cost is duplicated credential helpers |
| Credentials | Generated `.env` at mode 600; compose references `${VAR:?}` with no fallback | Secrets leave the tracked tree, and a missing value stops the stack instead of silently using `admin` |
| Backup | Documented volumes, no backup script shipped | The customer's own tooling does the work; we will not ship an untested restore |
| Versioning | Single pinned `TAG` in `.env`, forward-only schema | One tested combination per deploy; rollback limits stated honestly |
| Infrastructure | Bundled by default, each store switchable to an external endpoint | Environment decisions belong to the deployer |

Environment-specific choices (TLS mode, registry, external endpoints) are install-time options with documented defaults. The project does not pick them.

## Prerequisites

- Docker Engine with the Compose plugin, **Compose >= 2.20.0** (required by the optional-dependency handling described under Profiles).
- Published service images reachable from the host, and a domain resolving to it when TLS is not self-signed.
- Host tuning for the bundled Elasticsearch: `vm.max_map_count` at or above Elasticsearch 7's required value and enough memory headroom for its heap. The operations doc states the value and how to persist it; the deployer checks it and refuses with the exact remedy rather than letting Elasticsearch crash-loop.

## Files

| File | Change |
|---|---|
| `docker-compose.yml` | Registry images, no `build:`, no literal secrets, profiles for optional services and bundled stores, restart policy and log caps, and **no published ports except nginx's 80/443** |
| `docker-compose.build.yml` | New. Restores `build:` contexts for all five buildable services (`idp`, `background`, `socket`, `url-shortener`, `liveness`), for contributors |
| `docker-compose.dev.yml` | Unchanged. Plain-HTTP infra only |
| `.env.example` | Grows from `DOMAIN` alone to the documented contract below |
| `scripts/deploy-compose.sh` | New. Guided deployer |
| `scripts/compose/test-config.sh` | New. `docker compose config` assertions; starts no containers |
| `docs/deployment-compose.md` | New. Production operations: volumes, upgrade, rollback, TLS, restart behavior |
| `README.md` | Compose section points at the deployer and the operations doc |

## The `.env` contract

Every required value uses `${VAR:?message}` so `docker compose up` refuses to start when one is unset. No `change_me` default survives anywhere in the compose file.

```
# Identity of the deployment
DOMAIN=verify.example.com
REGISTRY=docker.io/myorg
TAG=1.0.0

# TLS: self-signed | files
TLS_MODE=self-signed

# Credentials (generated by scripts/deploy-compose.sh)
POSTGRES_USER=ezto
POSTGRES_PASSWORD=
MINIO_ROOT_USER=ezto
MINIO_ROOT_PASSWORD=
KEYCLOAK_ADMIN=admin
KEYCLOAK_ADMIN_PASSWORD=
SHORTENER_KEY=

# Endpoints — internal names when bundled, your own when a profile is dropped
POSTGRES_HOST=postgres
POSTGRES_PORT=5432
MINIO_SERVER_URL=http://minio:9000
MINIO_PUBLIC_URL=https://verify.example.com/storage/ezto-verify/public
ES_HOST=elasticsearch
ES_PORT=9200

# Active services and bundled stores
COMPOSE_PROFILES=infra-postgres,infra-minio,infra-elasticsearch,background,socket,url-shortener
```

Variable names follow what the code already consumes, verified against the repo rather than invented:

- **`KEYCLOAK_ADMIN`**, not `KEYCLOAK_ADMIN_USER`. The compose file, the `idp-init` bootstrap command, and the chart's `secret-idp.yaml` all read `KEYCLOAK_ADMIN`; a renamed variable would leave the IdP bootstrap without an admin username.
- **`ES_HOST` is a bare hostname with a separate `ES_PORT`**, not a URL. The current stack sets `ES_HOST: elasticsearch` and `ES_PORT: "9200"`.
- **`MINIO_PUBLIC_URL` is a distinct browser-facing URL**, not derivable from `MINIO_SERVER_URL`. Today it is hardcoded to `http://localhost:9000/ezto-verify/public`, so public object links are broken for any real deployment. It must be a URL the end user's browser can actually reach — served through nginx on `DOMAIN`, or the customer's own object-storage endpoint — and it is required whenever MinIO is external.
- **`SHORTENER_KEY`** is consumed by both the IdP and the URL shortener and must be identical in both; one variable enforces that.
- **`MINIO_ROOT_USER`/`MINIO_ROOT_PASSWORD` also populate the application's `MINIO_ACCESS_KEY`/`MINIO_SECRET_KEY`** in v1. That is today's behavior preserved deliberately, and it is a known weakness recorded under Out of scope.

`.env` is already ignored by the `*.env` rule in `.gitignore`.

## Profiles

Compose cannot define a service conditionally, so every optional component sits behind a profile that `COMPOSE_PROFILES` activates.

| Profile | Services | Default |
|---|---|---|
| `infra-postgres` | `postgres` | on |
| `infra-minio` | `minio`, `minio-init` | on |
| `infra-elasticsearch` | `elasticsearch` | on |
| `background` | `background` | on |
| `socket` | `socket` | on |
| `url-shortener` | `url-shortener` | on |
| `face-biometrics` | `liveness` | off (~15 GB image); its image comes from `REGISTRY`/`TAG` like the rest, and `docker-compose.build.yml` restores its build context |

`idp`, `idp-init`, `nginx`, and `certs-check` carry no profile and always run. MailDev is deliberately absent from the production stack: SMTP is configured per tenant through the onboarding wizard against a real provider, and the MailDev catcher stays in `docker-compose.dev.yml` for local testing.

Two consequences to handle in implementation:

1. **`depends_on` must tolerate a disabled store.** A dependency on a service excluded by profiles otherwise fails. Infrastructure dependencies use `depends_on: <service>: {condition: service_started, required: false}`, which Compose reduces to a warning when the dependency is absent. The `required` field was introduced in Docker Compose 2.20.0, so **Docker Compose >= 2.20.0 becomes a documented prerequisite**. Verified on Compose v5.1.3: with the store's profile off the dependent service still resolves, and with it on both appear.
2. **The IdP must not advertise absent services.** When `background`, `socket`, `url-shortener`, or `liveness` is off, the deployer writes the corresponding `BACKGROUND_JOB_URL`, `SOCKET_URL`, `SHORTENER_HTTP_URL`, `SHORTENER_HTTPS_URL`, or `LIVELINESS_URL` as empty, mirroring what `templates/configmap-idp.yaml` does in the chart.

`minio-init` bootstraps buckets and belongs to the `infra-minio` profile. A customer using external object storage must create the bucket themselves; the operations doc states this and names the bucket.

## `scripts/deploy-compose.sh`

Flags mirror the Kubernetes installer where the concepts match: `--registry`, `--tag`, `--domain`, `--services CSV`, `--tls self-signed|files`, `--env-out`, `--overwrite`, `--non-interactive`, `--yes`, `--print-plan`, `--dry-run`, `--uninstall`, `--purge-volumes`, and `--<name>-password-file` for each credential.

Order of operations, which is itself a requirement:

1. Parse flags. `--help` prints and exits.
2. Prompt for registry, tag, domain, services, and TLS mode when interactive. Skip every install-only prompt when `--uninstall` is set.
3. Validate the domain, the service list, and the TLS mode.
4. **Handle `--uninstall` here and exit** — before image resolution, before credential generation, before any TLS work. `--uninstall` runs `docker compose down` and keeps volumes. Destroying data requires `--purge-volumes` plus an explicit confirmation naming the exact volumes. This ordering is not cosmetic: the Kubernetes installer shipped with the reverse order, and its uninstall path demanded an image source it had no use for.
5. Resolve images from `REGISTRY` + `TAG`, refusing when either is missing. Pure computation, no writes.
6. Check the state of the host: does `.env` already exist, do the data volumes already exist, is `vm.max_map_count` sufficient for a bundled Elasticsearch. Decide install-versus-upgrade here (next section).
7. Print the plan, credentials redacted, including the resolved images, active profiles, TLS mode, and whether this is a first install or an upgrade. **`--print-plan` exits here, having written nothing and generated no certificate.**
8. Generate credentials, honoring any `--*-password-file`, and write `.env` at mode 600. Refuse to overwrite an existing `.env` without `--overwrite`.
9. Prepare TLS: `self-signed` runs `nginx/gen-self-signed.sh` for `DOMAIN`; `files` verifies `nginx/certs/fullchain.pem` and `privkey.pem` exist and refuses otherwise.
10. Run `docker compose config` to validate. `--dry-run` exits here.
11. `docker compose pull`, then `up -d`, then wait on healthchecks, then print the access URL.

Steps 7 and 8 are in this order on purpose. A preview must not create a `.env` or a certificate as a side effect of being asked what it would do.

Credential generation reuses the pattern already proven in `scripts/install.sh`: read from a file when given, otherwise generate, and never emit a secret to stdout, the plan, or the process arguments.

## First install versus upgrade

An existing deployment has credentials baked into initialized data volumes, and generating fresh ones locks it out of its own data. PostgreSQL sets its superuser password only when `pgdata` is first initialized, MinIO likewise for `miniodata`, and `init-databases.sql` runs only on that first start. Writing a new `POSTGRES_PASSWORD` into `.env` does not rotate the password inside an already-initialized volume — it just makes the stack fail to authenticate.

The deployer therefore distinguishes two cases from the host state it inspected in step 6:

- **First install** — no `.env` and no data volumes. Generate every credential and proceed.
- **Upgrade or adoption** — data volumes exist. Refuse to generate credentials, and require the existing values through `--postgres-password-file`, `--minio-password-file`, and the rest, naming exactly which ones are needed. The refusal message states why: the volume already holds data written under the old password.

For the current stack those old values are the literals in the shipped compose file (`admin`/`admin` for PostgreSQL, `minioadmin`/`minioadmin123` for MinIO), so the operations doc gives a concrete adoption recipe. Actually rotating a credential is a separate procedure — `ALTER USER` in PostgreSQL, `mc admin user` for MinIO — and the doc describes it rather than pretending `.env` can do it.

## External stores

Dropping an `infra-*` profile is a configuration change, but each external store carries a precondition the deployer cannot satisfy for the customer:

- **PostgreSQL.** `init-databases.sql` is mounted into the bundled container's `docker-entrypoint-initdb.d` and never runs against an external server. The four databases — `eztoverify`, `jobrnr`, `urlshortner` (spelled as in the file), and `socket` — must already exist, and the configured user must own them. The operations doc reproduces the exact `CREATE DATABASE` statements, and the deployer verifies they exist before starting the stack rather than letting each service fail its own migration.
- **MinIO.** `minio-init` bootstraps the bucket and belongs to `infra-minio`. An external endpoint needs the bucket created, its public prefix policy set, and `MINIO_PUBLIC_URL` pointed at a browser-reachable URL.
- **Elasticsearch.** `ES_HOST` and `ES_PORT` must point at a reachable 7.x endpoint. The bundled one runs with security disabled; a real external cluster usually does not, so credential support for it is a gap this design records rather than solves.

## Operations doc

`docs/deployment-compose.md` covers what we deliberately do not automate.

**Volumes.** `pgdata` holds identity, configuration, and jobs. `miniodata` holds uploaded documents. `esdata` holds events and audit records — this is primary data read by the IdP, not a rebuildable index, and losing it loses audit history. A restore must take all three from the same point in time, with the stack down.

**Upgrade.** Set `TAG`, `docker compose pull`, `docker compose up -d`.

**Rollback.** Set the previous `TAG` and `up -d`. Keycloak and service migrations are forward-only: an older tag does not undo a schema change, so rolling back across one requires restoring data from before the upgrade. This limitation is stated, not worked around.

**TLS.** `self-signed` regenerates with `nginx/gen-self-signed.sh` and browsers will warn. `files` means the customer owns renewal; renewing is replacing the two files in `nginx/certs` and reloading nginx.

**Resilience.** Every long-running service gets `restart: unless-stopped`, a healthcheck, and a log-size cap so a single host cannot fill its disk with container logs.

**Exposure.** Only nginx publishes ports, 80 and 443. PostgreSQL, MinIO, Elasticsearch, `background`, `socket`, and `url-shortener` reach each other over the compose network and publish nothing; the current file publishes all of them on every interface, which on a public host means an unauthenticated Elasticsearch, an exposed database, and exposed object storage. Where a port genuinely must be reachable for debugging, it is bound to `127.0.0.1` explicitly. The doc states this and tells the operator to verify with `docker compose ps` that nothing but nginx is listening publicly.

## Testing

`scripts/compose/test-config.sh` starts no containers and asserts, via `docker compose config`:

- An empty environment fails closed: with no `.env`, compose refuses rather than substituting a default.
- `REGISTRY` and `TAG` reach every service image reference.
- Dropping `background`, `socket`, or `url-shortener` from `COMPOSE_PROFILES` removes that service and blanks the matching IdP URL.
- Dropping `infra-postgres` removes `postgres` while the rest of the graph still resolves, proving the optional-dependency handling works.
- `docker-compose.build.yml` restores the four `build:` contexts.
- `--print-plan` and `--uninstall --print-plan` both succeed with no image source and no credentials present, and leave no `.env` and no generated certificate behind.
- No service other than nginx declares a published port, and nginx declares exactly 80 and 443.
- Every variable the compose file references is present in `.env.example`, and every variable in `.env.example` is referenced — this is the check that would have caught `KEYCLOAK_ADMIN_USER`.

## Out of scope

- **Publishing the service images.** This design assumes `REGISTRY`/`TAG` point at published images and fails with a clear message when they do not. Building and publishing them is separate work and currently has no pipeline in this repository.
- ACME/automatic certificate renewal and upstream TLS termination. Deliberately excluded from v1.
- Backup and restore tooling.
- Multi-host, HA, or orchestrated compose deployments.
- Whether the application should keep using MinIO **root** credentials as its access key. Today the IdP receives the root user and password, so a leaked `.env` or a compromised IdP yields full MinIO root access rather than scoped bucket access. This design moves those values into `.env` but deliberately does not change the trust model; a dedicated application key pair, with root reserved for provisioning, is a follow-up worth filing on its own.
- Authentication for an external Elasticsearch cluster.
- Keeping secrets out of container environments. They stay readable via `docker inspect` and the container's own environment, because the services read plain environment variables and support no `*_FILE` convention. Changing that requires application changes in four services and was ruled out for v1 with that cost understood.

## Risks

- **A stale `.env` after an upgrade.** New required variables introduced by a later version will stop the stack until the customer adds them. The deployer should name the missing variable, and `.env.example` stays the reference.
- **Profile combinations we never test.** Seven profiles are a large matrix; `test-config.sh` covers the meaningful ones, not all of them.
- **External-store paths are config-only.** Pointing at managed PostgreSQL or Elasticsearch is supported by configuration but will not be exercised against real managed services by us.
