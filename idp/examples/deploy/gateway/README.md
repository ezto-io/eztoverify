# Gateway (reference deployment)

Example **nginx reverse proxy** for an EZTO Verify deployment. Reference only — not
built or required by `build.sh`; the IdP runs standalone on `8443` without it.

Use it as a starting point for a production front door.

## What it does

TLS terminator + reverse proxy in front of two upstreams:

```
upstream qlikverify → qlikverify:8443   # the Keycloak / EZTO Verify app
upstream minio      → minio:9000        # object storage (public tenant assets)
```

- **TLS termination** on 443 (cert at `/etc/ssl/ssl.crt`, key `/etc/ssl/ssl.key`).
- **Multi-tenant wildcard host** — `server_name "~(.*).your-domain.example"`; each
  subdomain is a tenant.
- **Routing**
  - `(.*)/public/*.{md,png,svg,jpg,jpeg}` → MinIO per-tenant public path
  - `/public/*.{md,png,svg}` → MinIO global public path
  - `/` → the Keycloak app
- **Security headers** — HSTS, CSP, X-Frame-Options, X-Content-Type-Options,
  XSS-Protection; hides `Server` / `X-Powered-By` / `X-Generator`.

## Before using

You MUST adapt:

| Placeholder / assumption | Change to |
|---|---|
| `server_name ~(.*).your-domain.example` | your real wildcard domain |
| `ssl_certificate` / `ssl_certificate_key` | your TLS cert + key paths |
| `upstream qlikverify` → `qlikverify:8443` | your app host/port (compose service, k8s svc, etc.) |
| `upstream minio` → `minio:9000` | your object-store host/port |
| CSP `frame-src` allowlist | your own embedded third-parties |

> The `qlikverify` / `minio` upstream names assume a docker-compose / orchestrator
> where those are resolvable service names.
