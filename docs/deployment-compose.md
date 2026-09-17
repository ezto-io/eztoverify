# Deploying ezto Verify with Docker Compose

Single-host production path. For Kubernetes see [`charts/ezto-verify`](../charts/ezto-verify)
and `scripts/install.sh`.

## Prerequisites

- Docker Engine with the Compose plugin, **Compose >= 2.20.0**. `depends_on: required: false`
  needs it, and the deployer refuses to run below it.
- Published images reachable from this host for `idp`, `background`, `socket`, and
  `url-shortener` (plus `liveness` if you enable face biometrics). This repository builds none
  of them and ships no pipeline that does.
- A domain resolving to this host, unless you accept a self-signed certificate.
- For the bundled Elasticsearch, `vm.max_map_count` at or above `262144`:

      sudo sysctl -w vm.max_map_count=262144
      echo 'vm.max_map_count=262144' | sudo tee /etc/sysctl.d/99-ezto-verify.conf

  The deployer checks this on Linux and refuses with that remedy rather than letting
  Elasticsearch crash-loop.

## Deploying

    ./scripts/deploy-compose.sh --domain verify.example.com --registry eztovrfy --tag 1.0.0

Run it with `--print-plan` first. That mode makes no external call, writes no `.env`, and
generates no certificate.

### Image naming

Compose reads one fully resolved reference per service, so any registry layout works. Pick
whichever matches how your images are published:

| Flags | Resolves to |
|---|---|
| `--registry eztovrfy --tag 1.0.0` | `eztovrfy/idp:1.0.0` |
| `--registry myorg --image-prefix ezto- --tag 1.0.0` | `myorg/ezto-idp:1.0.0` (the Helm chart's naming) |
| `--image-repo myorg/ezto-oss` | `myorg/ezto-oss:idp` (one repo, service as tag) |
| `--idp-image other/idp:9` | exactly that, overriding any derivation |

The deployer writes the resolved references into `.env` as `IDP_IMAGE`, `BACKGROUND_IMAGE`,
`SOCKET_IMAGE`, `URL_SHORTENER_IMAGE`, and `LIVENESS_IMAGE`. Set them by hand if you prefer;
`REGISTRY`, `IMAGE_PREFIX`, and `TAG` are recorded for provenance and Compose never reads them.

Upgrading means changing the tag in those references — via `--tag` and `--overwrite`, or by
editing `.env` directly.

## What is exposed

Only nginx publishes ports, `80` and `443`. PostgreSQL, MinIO, Elasticsearch, `background`,
`socket`, and `url-shortener` reach each other over the compose network and publish nothing.
Verify after every deploy:

    docker compose ps --format 'table {{.Service}}\t{{.Ports}}'

Anything but nginx showing a `0.0.0.0:` binding is a misconfiguration. Where a port must be
reachable for debugging, bind it to loopback (`127.0.0.1:5432:5432`) and remove it afterwards.

## Volumes

| Volume | Holds | Losing it means |
|---|---|---|
| `pgdata` | Identity, configuration, jobs | Total loss of tenants and users |
| `miniodata` | Uploaded documents | Loss of every verification document |
| `esdata` | Events and audit records | Loss of audit history — primary data read by the IdP, not a rebuildable index |

A restore must take all three from the same point in time, with the stack down. No backup
script ships here: your own tooling does that work, and we will not ship an untested restore.

## Upgrade

Point the `*_IMAGE` references in `.env` at the new tag, then:

    docker compose pull
    docker compose up -d

## Rollback

Point the `*_IMAGE` references back at the previous tag and `docker compose up -d`. Keycloak and service migrations are
forward-only: an older tag does not undo a schema change. Rolling back across a migration
requires restoring data from before the upgrade. Stated, not worked around.

## TLS

`TLS_MODE=self-signed` generates a certificate with `nginx/gen-self-signed.sh`; browsers will
warn. `TLS_MODE=files` means you own renewal — replace `fullchain.pem` and `privkey.pem` in
`nginx/certs`, then `docker compose exec nginx nginx -s reload`. ACME and automatic renewal
are out of scope for v1.

## Onboarding is IP-restricted

`ROOT_ACCESS_IP` is an exact comma-separated IP list with no CIDR support. Behind nginx it is
matched against `X-Real-IP` — the real address of the browser doing the onboarding, not this
host's own address. If onboarding is rejected, read the address nginx recorded:

    docker compose logs nginx | tail -5

Add it to `ROOT_ACCESS_IP` in `.env`, then `docker compose up -d idp`.

## External stores

Dropping an `infra-*` profile from `COMPOSE_PROFILES` is a configuration change, but each
external store carries a precondition the deployer cannot satisfy for you.

**PostgreSQL.** `init-databases.sql` is mounted into the *bundled* container's
`docker-entrypoint-initdb.d` and never runs against an external server. These four databases
must already exist, owned by the configured user:

    CREATE DATABASE "eztoverify";   -- idp (Keycloak fork) datastore
    CREATE DATABASE "jobrnr";       -- background (jobrunr) service
    CREATE DATABASE "urlshortner";  -- url-shortener service
    CREATE DATABASE "socket";       -- socket service

The `urlshortner` spelling is intentional and must be reproduced exactly. The deployer
verifies all four are reachable before starting the stack.

**MinIO.** `minio-init` bootstraps the bucket and belongs to `infra-minio`. An external
endpoint needs the bucket created, its `public` prefix set to allow anonymous download, and
`MINIO_PUBLIC_URL` pointed at a URL an end user's browser can reach.

**Elasticsearch.** `ES_HOST` and `ES_PORT` must point at a reachable 7.x endpoint. The bundled
one runs with security disabled; a real external cluster usually does not, and credential
support for it is a gap this version records rather than solves.

## Adopting an existing stack

If this host already holds `pgdata`, `miniodata`, or `esdata` but has no `.env`, the deployer
refuses to generate credentials: those volumes were written under the old values, and new ones
would leave the stack unable to open its own data. Supply the existing values:

    ./scripts/deploy-compose.sh --domain ... --registry ... --tag ... \
      --postgres-password-file ./pg.txt \
      --minio-password-file ./minio.txt \
      --admin-password-file ./admin.txt \
      --shortener-key-file ./shortener.txt

A stack deployed from an older revision of this repository used the literals `admin`/`admin`
for PostgreSQL and `minioadmin`/`minioadmin123` for MinIO.

Rotating a credential is a separate procedure, not something `.env` can do:

    docker compose exec postgres psql -U <user> -c "ALTER USER <user> WITH PASSWORD '<new>';"

Update `.env` to match and restart the dependent services.

## Known limitations

- Secrets stay readable via `docker inspect` and in each container's environment. The services
  read plain environment variables and support no `*_FILE` convention; changing that needs
  application changes in four services and was ruled out for v1.
- The application uses MinIO **root** credentials as its access key, so a leaked `.env` or a
  compromised IdP yields full MinIO root access rather than scoped bucket access. A dedicated
  application key pair is follow-up work.
- No ACME or automatic certificate renewal.
- No backup or restore tooling.
- No multi-host, HA, or orchestrated deployment.
