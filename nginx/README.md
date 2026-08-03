# nginx reverse proxy — custom domain + HTTPS

The TLS edge of the compose stack. `docker compose up` serves the IdP at
`https://<your-domain>/auth` through this proxy; the stack does not start
without a domain and certs (a `certs-check` one-shot fails fast if they are
missing). For plain-HTTP local development use `docker-compose.dev.yml`
(infra only, run the app code locally).

## Files
- `templates/default.conf.template` — nginx vhost (`${DOMAIN}` filled at container start);
  TLS terminates here, 80→443 redirect, proxies to `idp:8080` with forwarded headers.
- `certs/` — put your TLS `fullchain.pem` + `privkey.pem` here (git-ignored).
- `gen-self-signed.sh` — make a throwaway self-signed cert for local testing.

## Setup
1. Set the domain (copy the root env template once):
   ```bash
   cp .env.example .env        # then edit DOMAIN=verify.example.com
   ```
2. Provide certs in `nginx/certs/` — either:
   - **Real CA cert** (recommended): drop `fullchain.pem` + `privkey.pem` (e.g. from Let's Encrypt).
   - **Self-signed** (testing only): `DOMAIN=verify.example.com ./nginx/gen-self-signed.sh`
3. Point the domain at the host (real DNS A record, or `/etc/hosts` → `127.0.0.1` for local).
4. Start the stack:
   ```bash
   docker compose up --build -d
   ```

Now: `https://<your-domain>/auth/admin/` (admin/admin by default).

## How TLS is wired
- The `nginx` service owns ports 80/443; the idp publishes no ports.
- The idp runs in edge mode: `KC_PROXY=edge`, `KC_HOSTNAME=<domain>`,
  `KC_HOSTNAME_URL=https://<domain>/auth`, strict hostname on. nginx↔idp stays
  plain HTTP inside the Docker network; TLS is only at the edge.
- The master realm keeps `sslRequired=external` (set by the `idp-init` one-shot).

## Notes
- Renewals: replace the files in `certs/` and `docker compose restart nginx`.
- To also expose a service (e.g. socket) on the domain, add a `location` block in the template.
