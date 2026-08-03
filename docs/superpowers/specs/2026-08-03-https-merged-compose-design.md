# HTTPS merged into main compose — design

Date: 2026-08-03
Status: approved

## Goal

`docker compose up` serves the full stack over HTTPS (nginx TLS edge in the same
file). The separate `docker-compose.proxy.yml` overlay is removed.
`docker-compose.dev.yml` (plain-HTTP dev infra) stays untouched.

## Decision

Strict mode chosen by user: **require `DOMAIN` + certs — fail fast**, no
auto-generated localhost fallback.

## Behavior

- `docker compose up --build` → HTTPS-only at `https://$DOMAIN/auth`.
- `DOMAIN` unset → compose interpolation error (`${DOMAIN:?…}`), nothing starts.
- Certs missing in `nginx/certs/` → one-shot `certs-check` container exits 1
  with a readable message; nginx never starts.
- `docker-compose.dev.yml` unchanged (HTTP, infra only).

## Changes — docker-compose.yml

- Add `nginx` service (from the deleted overlay): ports 80/443,
  `./nginx/templates` + `./nginx/certs` mounts, `DOMAIN` env with `:?` guard,
  depends on `certs-check` (completed) and `idp` (started).
- Add `certs-check` one-shot (alpine): verifies `fullchain.pem` + `privkey.pem`
  exist and are non-empty, else exits 1 with instructions.
- `idp` → edge mode: `KC_HOSTNAME_URL=https://${DOMAIN}/auth`,
  `KC_HOSTNAME=${DOMAIN}`, `KC_HOSTNAME_STRICT=true`,
  `KC_HOSTNAME_STRICT_HTTPS=true`, `KC_PROXY=edge`,
  `DASHBOARD_URL=https://${DOMAIN}`. Published ports `8080/8443` removed —
  nginx is the only entry point. (`idp-init` unaffected: runs in the idp's
  network namespace, talks to `localhost:8080`.)
- `idp-init`: `MASTER_SSL_REQUIRED=external` (secure default kept).
- `url-shortener`: `FALLBACK_URL=https://${DOMAIN}`.

## Docs

- Delete `docker-compose.proxy.yml`.
- Root `README.md`, `nginx/README.md`, `.env.example`: drop overlay wording;
  document one-command HTTPS flow (set `DOMAIN` → certs → `docker compose up`);
  update ROOT_ACCESS_IP note (gate now always sees `X-Real-IP`).
- `nginx/gen-self-signed.sh` stays for local/testing certs.

## Error handling

| Failure | Result |
|---|---|
| `DOMAIN` unset | compose parse error with message |
| certs missing | `certs-check` exits 1 with instructions |
| bad certs | nginx crash-loop with openssl error (standard) |

## Testing

`docker compose config` validates; full `up` with self-signed cert for a test
domain; check `https://<domain>/auth` responds and master realm
`sslRequired=external`.
