# nginx reverse proxy (optional) — custom domain + HTTPS

Front the stack with nginx so the IdP is served at `https://<your-domain>/auth` instead of
`http://localhost:8080/auth`. Opt-in via the `docker-compose.proxy.yml` overlay; the base
`docker compose up` is unaffected.

## Files
- `templates/default.conf.template` — nginx vhost (`${DOMAIN}` filled at container start);
  TLS terminates here, 80→443 redirect, proxies to `idp:8080` with forwarded headers.
- `certs/` — put your TLS `fullchain.pem` + `privkey.pem` here (git-ignored).
- `gen-self-signed.sh` — make a throwaway self-signed cert for local testing.

## Enable
1. Set the domain (copy the root env template once):
   ```bash
   cp .env.example .env        # then edit DOMAIN=verify.example.com
   ```
2. Provide certs in `nginx/certs/` — either:
   - **Real CA cert** (recommended): drop `fullchain.pem` + `privkey.pem` (e.g. from Let's Encrypt).
   - **Self-signed** (testing only): `DOMAIN=verify.example.com ./nginx/gen-self-signed.sh`
3. Point the domain at the host (real DNS A record, or `/etc/hosts` → `127.0.0.1` for local).
4. Start with the overlay:
   ```bash
   docker compose -f docker-compose.yml -f docker-compose.proxy.yml up -d
   ```

Now: `https://<your-domain>/auth/admin/` (admin/admin by default).

## What the overlay changes
- Adds the `nginx` service (ports 80/443).
- Overrides the idp to public-TLS mode: `KC_PROXY=edge`, `KC_HOSTNAME=<domain>`,
  `KC_HOSTNAME_URL=https://<domain>/auth`, strict hostname on. nginx↔idp stays plain HTTP
  inside the Docker network; TLS is only at the edge.

## Notes
- Renewals: replace the files in `certs/` and `docker compose ... restart nginx`.
- To also expose a service (e.g. socket) on the domain, add a `location` block in the template.
