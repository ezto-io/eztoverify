# ezto-verify Helm chart

Deploys the ezto Verify platform (idp + background + socket + url-shortener,
optionally liveness) to Kubernetes, with optional bundled PostgreSQL and
MinIO. Elasticsearch is **external-by-default** — see below.

> **Bare `helm install` with default values fails by design.** `values.yaml`
> ships `elasticsearch.enabled: false` (external-by-default) with
> `elasticsearch.external.host` left empty and `required`-guarded in the
> template. You must either point it at a real cluster
> (`--set elasticsearch.external.host=...`) or use an overlay that bundles
> Elasticsearch (`values-kind.yaml` / `values-minikube.yaml` both set
> `elasticsearch.enabled: true`).

## Prerequisites

| Requirement | Notes |
|---|---|
| Kubernetes ≥ 1.25 | any conformant cluster (kind/minikube locally; managed clusters in production) |
| Helm 3 (or 4) | chart uses `apiVersion: v2`, no Helm-4-only features |
| kubectl | configured for the target cluster |
| Container images | The chart ships no default registry: every `image.repository` is `REPLACE_ME/ezto-<service>`, so an unconfigured install fails visibly instead of pulling an unknown image. Supply your own `linux/amd64` images — built with `scripts/helm/build-and-load.sh`, or published to a registry you control — and point each service's `image.repository`/`tag` at them (`scripts/install.sh` asks once and writes them for you). Private registries also need `imagePullSecrets`. |
| Default StorageClass | bundled PostgreSQL / MinIO / Elasticsearch claim PVCs; set `global.storageClass` to pick a specific one |
| Elasticsearch 7.x | external endpoint (`elasticsearch.external.host`) unless an overlay bundles it — see [Elasticsearch](#elasticsearch) |
| Ingress controller (optional) | `ingress.className: nginx` assumed for TLS/public exposure; keep `use-forwarded-headers` off (default) — see [ROOT_ACCESS_IP](#root_access_ip--master-tenant-onboarding) |
| cert-manager (optional) | only when `ingress.tls.certManager: true`; otherwise provide `ingress.tls.secretName` |

## Installing

The guided installer uses an existing Kubernetes context, asks for your image
registry and which services to enable, generates protected credentials, and
validates the Helm render before touching the cluster. Liveness is skipped by
default.

Give the installer an image source one of three ways: `--registry PREFIX` for
one repository per service (`PREFIX/ezto-idp:TAG`), `--image-repo REPO` for a
single repository whose tags are the service names, or an explicit
`--idp-image`/`--background-image`/`--socket-image`/`--url-shortener-image`.

```bash
./scripts/install.sh
```

For automation, pass the context and confirmation explicitly:

```bash
./scripts/install.sh --non-interactive --yes --context my-context
```

Manual Helm remains supported:

```bash
# Local/dev overlay bundles Elasticsearch + MailDev and leaves liveness off.
helm upgrade --install ezto-verify charts/ezto-verify \
  -f charts/ezto-verify/values-kind.yaml --timeout 15m --wait

kubectl get pods
helm status ezto-verify

# Uninstall keeps PVCs and the namespace.
helm uninstall ezto-verify
```

Step-by-step walkthroughs: [Local flow](#local-flow-kind-or-minikube) and
[Production flow](#production-flow).

## Values

Every top-level key in `values.yaml`, one line each. `image.*` fields shown
as `repository/tag/pullPolicy`.

| Key | Default | Description |
|-----|---------|-------------|
| `global.storageClass` | `""` | StorageClass for all PVCs; `""` = cluster default. |
| `imagePullSecrets` | `[]` | Image pull secrets applied to every pod spec in the chart (e.g. `[{name: regcred}]` for a private registry). |
| `image.pullPolicy` | `IfNotPresent` | Chart-wide default pull policy; each service's `image.pullPolicy` overrides it when non-empty. |
| `idp.image` | `REPLACE_ME/ezto-idp:latest` | idp (Keycloak 18 fork) image; the placeholder must be replaced. |
| `idp.resources` | `500m/1Gi` req, `2Gi` limit | idp container resources. |
| `idp.localUrl` | `http://localhost:8080` | Public base URL used verbatim when `ingress.enabled: false` (local port-forward). Ignored when ingress is on — the URL is then derived from `ingress.host` + TLS state. |
| `idp.admin.user` / `idp.admin.password` | `admin` / `change_me_admin` | Keycloak bootstrap admin credentials. Rotate before non-local use. |
| `idp.db.database` | `eztoverify` | idp's Postgres database name. |
| `idp.db.user` / `idp.db.password` | `""` / `""` | idp's Postgres credentials. `""` => inherit `postgres.user`/`postgres.password` — see "Credential coupling" below. |
| `idp.variant` | `"ezto verify"` | `VARIANT` env var — must be non-empty or master-realm bootstrap NPEs. |
| `idp.envName` | `dev` | `ENV` env var. |
| `idp.dashboardUrl` | `""` | `""` => derived from the public URL. |
| `idp.docsUrl` / `idp.docsLink` | `""` | Docs links surfaced in the idp UI. |
| `idp.rootAccessIp` | `127.0.0.1,0.0.0.0,::1` | Exact-IP allowlist (no CIDR) for master-tenant onboarding — see note below. Must never be empty (`required`-guarded). |
| `idp.shortenerKey` | `change_me_key` | Must equal `urlShortener.apiKeys`. |
| `idp.appPushVersion` | `9.3.1` | `APP_PUSH_VERSION` env var (authenticator app compatibility). |
| `idp.masterSslRequired` | `external` | Keycloak `sslRequired` for the `master` realm. Overlay to `none` for plain-HTTP local clusters (kind/minikube overlays do this). |
| `idp.sslInitJob.enabled` | `""` | `""` => auto: on when `ingress.tls.enabled` is false. |
| `idp.sslInitJob.backoffLimit` | `3` | Job retry limit. |
| `ingress.enabled` | `false` | Create an Ingress for the idp. |
| `ingress.className` | `nginx` | IngressClass name. |
| `ingress.host` | `verify.example.com` | Ingress hostname; also becomes the public hostname/URL when enabled. |
| `ingress.annotations` | `{}` | Extra annotations merged onto the Ingress. |
| `ingress.tls.enabled` | `false` | Terminate TLS at the ingress; flips derived public scheme to `https`. |
| `ingress.tls.certManager` | `false` | `true` adds the `cert-manager.io/cluster-issuer` annotation. |
| `ingress.tls.clusterIssuer` | `letsencrypt-prod` | cert-manager ClusterIssuer name. |
| `ingress.tls.secretName` | `""` | Non-empty => use this pre-existing TLS secret instead of cert-manager. |
| `postgres.enabled` | `true` | Bundle a PostgreSQL StatefulSet. `false` requires `postgres.external.*`. |
| `postgres.image` | `postgres:14-alpine` | Bundled Postgres image. |
| `postgres.user` / `postgres.password` | `admin` / `change_me_pg` | Bundled Postgres superuser credentials — also the *default* idp credentials (see "Credential coupling" below), and (with `postgres.enabled: false`) the credentials every service uses against the external host. |
| `postgres.storage` | `8Gi` | Bundled Postgres PVC size. |
| `postgres.resources` | `100m/256Mi` req, `1Gi` limit | Bundled Postgres resources. |
| `postgres.external.host` / `postgres.external.port` | `""` / `5432` | Used when `postgres.enabled: false`; host is `required`-guarded. |
| `minio.enabled` | `true` | Bundle a MinIO StatefulSet. `false` requires `minio.external.*`. |
| `minio.image` / `minio.mcImage` | `minio/minio:latest` / `minio/mc:latest` | Bundled MinIO server + `mc` (bucket-provisioning Job) images. |
| `minio.rootUser` / `minio.rootPassword` | `minioadmin` / `change_me_minio` | Bundled MinIO root credentials. |
| `minio.bucket` | `ezto-verify` | Bucket the provisioning Job creates and the idp uses. |
| `minio.storage` | `10Gi` | Bundled MinIO PVC size. |
| `minio.publicUrl` | `""` | `""` => derived as `<public URL>/minio/<bucket>/public`. **Non-functional unless `minio.publicIngress.enabled: true`** (or you expose MinIO yourself) — see "MinIO public assets" below. |
| `minio.publicIngress.enabled` | `false` | Create a second Ingress that routes `<ingress.host>/minio/*` to the bundled MinIO, making the derived `minio.publicUrl` above actually work. Requires `ingress.enabled: true` and an nginx-class ingress controller (uses `rewrite-target`). |
| `minio.resources` | `100m/256Mi` req, `1Gi` limit | Bundled MinIO resources. |
| `minio.external.url` / `.accessKey` / `.secretKey` / `.publicUrl` | `""` | Used when `minio.enabled: false`; `url` is `required`-guarded and must include an explicit port. |
| `elasticsearch.enabled` | `false` | Bundle an Elasticsearch StatefulSet. **Default is external** — see the warning above. |
| `elasticsearch.image` | `docker.elastic.co/elasticsearch/elasticsearch:7.17.18` | Bundled Elasticsearch image. |
| `elasticsearch.storage` | `8Gi` | Bundled Elasticsearch PVC size. |
| `elasticsearch.javaOpts` | `-Xms512m -Xmx512m` | Bundled Elasticsearch JVM heap. |
| `elasticsearch.resources` | `200m/1Gi` req, `1536Mi` limit | Bundled Elasticsearch resources. |
| `elasticsearch.external.host` / `.port` / `.scheme` | `""` / `9200` / `http` | **Required** when `elasticsearch.enabled: false` — `host` is `required`-guarded. |
| `maildev.enabled` | `false` | Bundle a MailDev SMTP catcher (dev/test only, never for real mail). |
| `maildev.image` | `maildev/maildev:latest` | MailDev image. |
| `background.enabled` | `true` | Render the background Deployment, Service, Secret, and idp endpoint. |
| `background.image` | `REPLACE_ME/ezto-background:latest` | Background job runner image; the placeholder must be replaced. |
| `background.db.database` | `jobrnr` | Background service's Postgres database name. |
| `background.resources` | `100m/512Mi` req, `1Gi` limit | Background container resources. |
| `socket.enabled` | `true` | Render the socket Deployment, Service, Secret, and idp endpoint. |
| `socket.image` | `REPLACE_ME/ezto-socket:latest` | Realtime push channel image; the placeholder must be replaced. |
| `socket.db.database` | `socket` | Socket service's Postgres database name. |
| `socket.resources` | `50m/128Mi` req, `256Mi` limit | Socket container resources. |
| `urlShortener.enabled` | `true` | Render the URL-shortener Deployment, Service, Secret, and idp endpoints. |
| `urlShortener.image` | `REPLACE_ME/ezto-url-shortener:latest` | Short-link service image; the placeholder must be replaced. |
| `urlShortener.db.database` | `urlshortner` | Database name (sic — matches `files/init-databases.sql`). |
| `urlShortener.apiKeys` | `change_me_key` | Must equal `idp.shortenerKey`. |
| `urlShortener.resources` | `50m/128Mi` req, `256Mi` limit | url-shortener container resources. |
| `liveness.enabled` | `false` | Opt-in — heavy face-match service (dlib/face_recognition). Compose-profile parity. |
| `liveness.image` | `REPLACE_ME/ezto-liveness:latest` | Liveness image reference; enabling liveness requires building or publishing this image first. |
| `liveness.faceTolerance` | `0.68` | Face-match tolerance threshold passed to the service. |
| `liveness.resources` | `500m/1Gi` req, `2Gi` limit | Liveness container resources. |

Every service also accepts `<service>.image.pullPolicy`; leave it `""` to
fall back to the chart-wide `image.pullPolicy`.

## Local flow (kind or minikube)

Use any existing Kubernetes context. Both local overlays set cluster behavior
only and carry no image references, so the images come from your registry (or
from a local source build).

```bash
# Guided path (select the existing context when prompted).
./scripts/install.sh

# Or install manually into the current context.
helm upgrade --install ezto-verify charts/ezto-verify \
  -f charts/ezto-verify/values-kind.yaml \
  --set idp.image.repository=docker.io/myorg/ezto-idp \
  --set background.image.repository=docker.io/myorg/ezto-background \
  --set socket.image.repository=docker.io/myorg/ezto-socket \
  --set urlShortener.image.repository=docker.io/myorg/ezto-url-shortener \
  --timeout 15m --wait
kubectl port-forward svc/ezto-verify-idp 8080:8080
# browse http://localhost:8080/auth
```

`values-minikube.yaml` behaves the same way. Both overlays bundle
Elasticsearch + MailDev, set plain-HTTP local Keycloak behavior, and leave
liveness disabled.

Building from source is still available as an explicit developer workflow:
run `scripts/helm/build-and-load.sh`, then add
`-f charts/ezto-verify/values-local-build.yaml` after the kind/minikube
overlay. Add `--with-liveness` and `--set liveness.enabled=true` only when a
local liveness image is intentionally required.

Before onboarding the master tenant through the browser at
`http://localhost:8080/auth`, make sure the client IP is covered by
`idp.rootAccessIp` — see the dedicated section below. The kind/minikube
overlays' default `rootAccessIp` (`127.0.0.1,0.0.0.0,::1`) covers
`kubectl port-forward` traffic (it always originates from localhost), so no
change is needed for the pure local flow above.

## Production flow

- Image repositories default to the `REPLACE_ME/ezto-<service>` placeholders. Set each service's `image.repository`/`tag` to your own registry and pinned version tags — never `latest` in production (see `values-prod.yaml` for the shape).
- Set `ingress.enabled: true`, `ingress.host` to your real domain, and
  either:
  - `ingress.tls.certManager: true` + `ingress.tls.clusterIssuer` (cert-manager
    issues and renews the certificate automatically), or
  - `ingress.tls.secretName` pointing at a TLS secret you manage and renew
    yourself.
- Elasticsearch **must** be external in production
  (`elasticsearch.enabled: false` + `elasticsearch.external.host/port/scheme`)
  — there is no supported bundled-ES production path.
- Rotate every `change_me_*` value (`idp.admin.password`, `idp.db.password`,
  `postgres.password`, `minio.rootPassword`, `idp.shortenerKey` /
  `urlShortener.apiKeys`) via `--set` or a secrets-manager-backed values
  file — never commit real secrets.
- `helm upgrade --install ezto charts/ezto-verify -f <your-values.yaml>` is
  safe to re-run for config/image changes. cert-manager renews certificates
  automatically before expiry; if you supplied `ingress.tls.secretName`
  yourself, you own its renewal — Helm does not manage that secret's
  lifecycle.
- See `values-prod.yaml` for a starting overlay to copy and edit.

## Scaling

`idp` is fixed at **1 replica** — there is no `idp.replicas` knob. The idp
(Keycloak fork) needs a session/cache clustering design (e.g. Infinispan
cross-site replication) before it's safe to run more than one instance;
running 2+ replicas today would produce inconsistent auth sessions. Treat
this as a prerequisite project, not a values override.

## ROOT_ACCESS_IP / master-tenant onboarding

`idp.rootAccessIp` is an **exact-IP allowlist, comma-separated, no CIDR**.
The idp's welcome page compares it against the `X-Real-IP` header it
receives. Through the chart's Ingress, that header is set by the
ingress-nginx controller from the real client connection — **not** trusted
verbatim from whatever the client sends — because `use-forwarded-headers`
defaults to `false` on the controller. Keep it that way: flipping it to
`true` would let any client set its own `X-Forwarded-*`/`X-Real-IP` headers
and spoof the allowlist check, defeating the gate entirely.

Before performing master-tenant onboarding through an Ingress:
1. Find the exact public IP the operator's browser will present to the
   ingress controller.
2. Add it to `idp.rootAccessIp` (comma-separated, no CIDR) and
   `helm upgrade` the release.
3. Onboard, then optionally tighten the list back down afterward.

Without a matching entry, the "create master admin" onboarding step is
rejected. For the pure local port-forward flow, traffic always looks like
it originates from `127.0.0.1`, already covered by the chart's default.

## Elasticsearch

External by default (`elasticsearch.enabled: false`). Set
`elasticsearch.external.host` (required), `.port` (default `9200`), and
`.scheme` (default `http`) to point at a real cluster, or set
`elasticsearch.enabled: true` to bundle a single-node Elasticsearch
StatefulSet for local/dev use (as the kind/minikube overlays do). The
bundled StatefulSet is not a supported production configuration.

## MinIO public assets

`MINIO_PUBLIC_URL` (the URL browsers use to fetch public MinIO objects, e.g.
avatars/documents) defaults to a *derived* value:
`<public scheme+host>/minio/<bucket>/public`. **That derived URL is not
functional out of the box** — the chart's main Ingress only routes `/` to
the idp; nothing serves `/minio/*` unless you turn one of these on:

- Set `minio.publicIngress.enabled: true` (also requires `ingress.enabled:
  true` and an ingress-nginx-class controller, since it relies on
  `nginx.ingress.kubernetes.io/rewrite-target`). This creates a second
  Ingress that routes `<ingress.host>/minio/*` to the bundled MinIO, and
  makes the default `MINIO_PUBLIC_URL` derivation above actually resolve.
- Or expose MinIO yourself (a different Ingress/LoadBalancer/CDN in front of
  it) and set `minio.publicUrl` (bundled MinIO) or
  `minio.external.publicUrl` (external MinIO) explicitly to wherever that
  is reachable.

Until one of those is done, anything that renders `MINIO_PUBLIC_URL` links
(e.g. avatar/document URLs) will point at a dead path.

## Credential coupling

`idp.db.user` / `idp.db.password` default to `""`, which means "inherit
`postgres.user` / `postgres.password`". This isn't just a convenience
default — with the bundled PostgreSQL, it's required: `files/init-databases.sql`
only runs `CREATE DATABASE`, it never creates a separate role, so every
database (`eztoverify`, `jobrnr`, `urlshortner`, `socket`) is only reachable
as the one superuser Postgres was bootstrapped with. Set `idp.db.user`/
`idp.db.password` explicitly only if you also arrange for that role to exist
(e.g. via a custom `init-databases.sql`, or against an external Postgres
where you manage roles yourself).

With `postgres.enabled: false` (external Postgres), there is no separate
"external credential" key — `postgres.user`/`postgres.password` are the
credentials every service (idp, background, socket, url-shortener) uses to
connect to `postgres.external.host`. Provision that role on the external
server with exactly those values before pointing the chart at it.

## Background bootstrap (idp -> background)

At boot, the idp makes a one-time call to background to bootstrap its
management-keys scheduler. That call is forked and its failure is swallowed
by the idp, so it never blocks or fails idp startup — this is why the idp
Deployment intentionally has no `wait-for-background` initContainer (see
the comment in `templates/idp-deployment.yaml`). To confirm the bootstrap
actually happened, check the background pod's logs for the idp's first
callback shortly after the idp becomes ready; it's not otherwise surfaced
in idp readiness/health.

## Hook Jobs (minio-provision, idp-sslinit)

Two post-install/post-upgrade hook Jobs run after the main workloads are
created:

- `minio-provision` (weight `-1`, `activeDeadlineSeconds: 300`) creates the
  MinIO bucket and sets its public-download policy. It runs *before*
  `idp-sslinit` so the bucket exists before anything on the idp boot path
  needs it, mirroring the ordering compose gave for free.
- `idp-sslinit` (default weight, `activeDeadlineSeconds: 600`) waits for the
  idp to come up, then pins the `master` realm's `sslRequired` via `kcadm`
  the same way compose's `idp-init` one-shot did.

`idp-sslinit`'s `kcadm` call reaches the idp over the pod network, not
loopback (compose's equivalent ran over loopback). Keycloak's
`sslRequired=external` exempts loopback **and RFC1918 site-local**
addresses from the HTTPS requirement, so this works as long as your
cluster's pod CIDR is RFC1918 (which is the common case for kind/minikube
and most on-prem/cloud CNIs). Clusters with a non-RFC1918 pod CIDR (e.g.
carrier-grade NAT `100.64.0.0/10`, or IPv6-only pod networks) will see this
Job's `kcadm` call rejected as non-SSL; such clusters need
`idp.masterSslRequired` set to something that tolerates that (e.g. `none`)
or a NetworkPolicy-era revisit of this assumption.

If either Job doesn't reach `Completed` within its deadline, `helm
install`/`upgrade` will report the release as failed — see also `--timeout
15m` in the local-flow instructions above, since the idp's slow first boot
is usually the bottleneck both Jobs are waiting behind.

## MailDev (dev SMTP)

With `maildev.enabled: true`, point each realm's SMTP settings at
`<fullname>-maildev:1025` — no auth, no TLS. Browse caught mail at the
MailDev web UI (`kubectl port-forward svc/<fullname>-maildev 1080:1080`,
then `http://localhost:1080`). Dev/test only — never wire this up for real
outbound mail.
