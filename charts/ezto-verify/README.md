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

## Values

Every top-level key in `values.yaml`, one line each. `image.*` fields shown
as `repository/tag/pullPolicy`.

| Key | Default | Description |
|-----|---------|-------------|
| `global.storageClass` | `""` | StorageClass for all PVCs; `""` = cluster default. |
| `image.pullPolicy` | `IfNotPresent` | Chart-wide default pull policy; each service's `image.pullPolicy` overrides it when non-empty. |
| `idp.image` | `REPLACE_ME/ezto-idp:latest` | idp (Keycloak 18 fork) image. Set to `ezto-idp:latest` + `pullPolicy: Never` for local kind/minikube. |
| `idp.resources` | `500m/1Gi` req, `2Gi` limit | idp container resources. |
| `idp.localUrl` | `http://localhost:8080` | Public base URL used verbatim when `ingress.enabled: false` (local port-forward). Ignored when ingress is on — the URL is then derived from `ingress.host` + TLS state. |
| `idp.admin.user` / `idp.admin.password` | `admin` / `change_me_admin` | Keycloak bootstrap admin credentials. Rotate before non-local use. |
| `idp.db.database` / `idp.db.user` / `idp.db.password` | `eztoverify` / `admin` / `change_me_pg` | idp's Postgres database + credentials. |
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
| `postgres.user` / `postgres.password` | `admin` / `change_me_pg` | Bundled Postgres superuser credentials. |
| `postgres.storage` | `8Gi` | Bundled Postgres PVC size. |
| `postgres.resources` | `100m/256Mi` req, `1Gi` limit | Bundled Postgres resources. |
| `postgres.external.host` / `postgres.external.port` | `""` / `5432` | Used when `postgres.enabled: false`; host is `required`-guarded. |
| `minio.enabled` | `true` | Bundle a MinIO StatefulSet. `false` requires `minio.external.*`. |
| `minio.image` / `minio.mcImage` | `minio/minio:latest` / `minio/mc:latest` | Bundled MinIO server + `mc` (bucket-provisioning Job) images. |
| `minio.rootUser` / `minio.rootPassword` | `minioadmin` / `change_me_minio` | Bundled MinIO root credentials. |
| `minio.bucket` | `ezto-verify` | Bucket the provisioning Job creates and the idp uses. |
| `minio.storage` | `10Gi` | Bundled MinIO PVC size. |
| `minio.publicUrl` | `""` | `""` => derived as `<public URL>/minio/<bucket>/public`; set explicitly if MinIO is reachable at a different public path. |
| `minio.resources` | `100m/256Mi` req, `1Gi` limit | Bundled MinIO resources. |
| `minio.external.url` / `.accessKey` / `.secretKey` / `.publicUrl` | `""` | Used when `minio.enabled: false`; `url` is `required`-guarded and must include an explicit port. |
| `elasticsearch.enabled` | `false` | Bundle an Elasticsearch StatefulSet. **Default is external** — see the warning above. |
| `elasticsearch.image` | `elasticsearch:7.17.18` | Bundled Elasticsearch image. |
| `elasticsearch.storage` | `8Gi` | Bundled Elasticsearch PVC size. |
| `elasticsearch.javaOpts` | `-Xms512m -Xmx512m` | Bundled Elasticsearch JVM heap. |
| `elasticsearch.resources` | `200m/1Gi` req, `1536Mi` limit | Bundled Elasticsearch resources. |
| `elasticsearch.external.host` / `.port` / `.scheme` | `""` / `9200` / `http` | **Required** when `elasticsearch.enabled: false` — `host` is `required`-guarded. |
| `maildev.enabled` | `false` | Bundle a MailDev SMTP catcher (dev/test only, never for real mail). |
| `maildev.image` | `maildev/maildev:latest` | MailDev image. |
| `background.image` | `REPLACE_ME/ezto-background:latest` | Background job runner image. |
| `background.db.database` | `jobrnr` | Background service's Postgres database name. |
| `background.resources` | `100m/512Mi` req, `1Gi` limit | Background container resources. |
| `socket.image` | `REPLACE_ME/ezto-socket:latest` | Realtime push channel image. |
| `socket.db.database` | `socket` | Socket service's Postgres database name. |
| `socket.resources` | `50m/128Mi` req, `256Mi` limit | Socket container resources. |
| `urlShortener.image` | `REPLACE_ME/ezto-url-shortener:latest` | Short-link service image. |
| `urlShortener.db.database` | `urlshortner` | Database name (sic — matches `files/init-databases.sql`). |
| `urlShortener.apiKeys` | `change_me_key` | Must equal `idp.shortenerKey`. |
| `urlShortener.resources` | `50m/128Mi` req, `256Mi` limit | url-shortener container resources. |
| `liveness.enabled` | `false` | Opt-in — heavy face-match service (dlib/face_recognition). Compose-profile parity. |
| `liveness.image` | `REPLACE_ME/ezto-liveness:latest` | Liveness image. |
| `liveness.faceTolerance` | `0.68` | Face-match tolerance threshold passed to the service. |
| `liveness.resources` | `500m/1Gi` req, `2Gi` limit | Liveness container resources. |

Every service also accepts `<service>.image.pullPolicy`; leave it `""` to
fall back to the chart-wide `image.pullPolicy`.

## Local flow (kind or minikube)

```bash
# 1. Create a cluster (skip if you already have one)
kind create cluster --name ezto

# 2. Build service images and load them into the cluster's node cache
./scripts/helm/build-and-load.sh kind ezto
# add --with-liveness to also build the face-liveness image (heavy)

# 3. Install using the kind overlay (bundles Elasticsearch + MailDev,
#    sets idp.masterSslRequired=none so plain HTTP works, pullPolicy: Never)
helm install ezto charts/ezto-verify -f charts/ezto-verify/values-kind.yaml

# 4. Reach the idp
kubectl port-forward svc/ezto-ezto-verify-idp 8080:8080
# browse http://localhost:8080/auth
```

For minikube, swap step 1/2/3 for `minikube start -p ezto`,
`./scripts/helm/build-and-load.sh minikube ezto`, and
`-f charts/ezto-verify/values-minikube.yaml`.

Before onboarding the master tenant through the browser at
`http://localhost:8080/auth`, make sure the client IP is covered by
`idp.rootAccessIp` — see the dedicated section below. The kind/minikube
overlays' default `rootAccessIp` (`127.0.0.1,0.0.0.0,::1`) covers
`kubectl port-forward` traffic (it always originates from localhost), so no
change is needed for the pure local flow above.

## Production flow

- Build and push images to your registry, then point each service's
  `image.repository`/`tag` at it (see `values-prod.yaml` for the shape).
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
