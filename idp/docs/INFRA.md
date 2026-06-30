# EZTO Verify — Infrastructure & Service Dependencies

What you need to run EZTO Verify, and what each feature depends on. Derived from the
codebase (env vars read at runtime + integration code).

> **Build prerequisite:** JDK **11**. The Keycloak 18 base + hibernate-enhance / Byte
> Buddy break on JDK 17/21. Build with `JAVA_HOME` pointing at JDK 11.

---

## 1. Core infrastructure

### Required to boot + basic operation
| Service | Purpose | Notes |
|---|---|---|
| **PostgreSQL** (or MySQL/MariaDB) | Keycloak datastore | any Keycloak-supported RDBMS |
| **MinIO** (or S3-compatible) | object storage — selfie/biometric/OCR images, tenant public assets | `MINIO_*` env |
| **Elasticsearch** | event / audit / **transaction-history** store (`ElasticEventStoreProvider`) | `ES_HOST`, `ES_PORT` |
| **SMTP** | email OTP + notifications | real provider per-tenant; maildev catcher in `docker-compose.dev.yml` for local testing |

### Optional — safe to omit
| Service | Purpose |
|---|---|
| **Vault** | external secret store. **Not required** — Keycloak vault is lazy, only used when realm config references `${vault.x}`. Skip for OSS; put secrets in config. |
| Kafka / Datadog / AWS EventBridge / Segment | event sinks / observability |
| LDAP | user federation |
| Exchange-rate API, debugger | pricing helper / dev tooling |
| **Infinispan** | embedded in Keycloak; external server only for HA clustering (no separate service single-node) |

---

## 2. EZTO sibling microservices

Separate repos under `github.com/ezto-io/ezto-verify`. The IdP **boots without them**;
the dependent feature fails only when invoked.

| Service | Repo | Powers | Required for |
|---|---|---|---|
| **jobrunr / background** | `ezto-background` | scheduled / background jobs | async workflows, scheduled tasks |
| **url-shortener** | `ezto-url_shortner` | short links | deep-link / callback URLs |
| **socket** | `ezto-socket` | realtime device push | Web2Device, push approval |
| **liveness** | `ezto-liveliness` | face liveness | Liveness / Selfie verification |
| **VideoKYC** | `ezto-video-kyc-postprocessing` + `ezto-video-kyc` + `ezto-janus-gateway` (WebRTC) | live video + processing | VideoKYC |
| **api-connector** | `ezto-api-connector-backend` (+ frontend / portal) | verifier catalog + execution backend | data/API verifications (Pattern A), verify-over-CIBA — _deferred_ |

---

## 3. Verification mechanisms

Verifications are **not** one thing. Four distinct patterns with different deps.

### Pattern A — Data / document API verification (KYC)
Server-to-server data lookup: user enters an ID, backend calls a KYC API, returns a
verified record.

**Flow:** required-action → `getProviderName()` → `Verification.doVerification(..., providerName, ...)`
→ POST `api-connector` `/api/<verifier>/verify` (legacy path: service impl → `SUREPASS_HOST/api/v1/...`).

**Verifiers:** PAN, PAN-OCR, Aadhaar, Aadhaar-OTP, Aadhaar-OCR, Pan-Aadhaar-link,
GSTIN, GST-with-OTP, Bank, Driving-License (+OCR), Voter-ID (+OCR), TAN, Udyam, FSSAI,
EPFO, ITR, Form26AS, TDS206, IEC, Business.

**Provider-pluggable:** SurePass / Decentro / Setu / Signzy (selected per verifier).
**Status:** moved to `../private-connectors/eztoverify-kyc/` (pending partner IP clearance).
**Needs:** api-connector + KYC vendor subscription + MinIO (OCR images) + ES (audit).

### Pattern B — Redirect / federation verification (connectors)
OAuth/OIDC redirect: user sent to a gov/national service, consents, identity/docs pulled
back. Each is a standalone Keycloak provider jar — **no api-connector**.

| Connector | Mechanism | Needs | Location |
|---|---|---|---|
| **DigiLocker** | OAuth → `pull-doc` gov documents | DigiLocker gov creds | `private-connectors/eztoverify` |
| **UAE Pass** | OAuth national-ID federation | UAE Pass creds | `private-connectors/eztoverify` |
| **Finema** | SSI / DID wallet (Thai ID) | Finema contract | `private-connectors/eztoverify` |
| **Sekura** | phone OTP / insights | Sekura account | `private-connectors/eztoverify` |
| **iProov** | face biometric SDK | iProov account | `private-connectors/shared` |

### Pattern C — Biometric / liveness / media
| Type | Backing service | Needs |
|---|---|---|
| Liveness / Selfie | `ezto-liveliness` | `LIVELINESS_URL` + MinIO |
| VideoKYC | `ezto-video-kyc-postprocessing` | service + MinIO |
| iProov face | iProov SDK (connector) | iProov account |

### Pattern D — Communication / possession
Email OTP (SMTP), Phone OTP (Sekura / Twilio-IVR connectors), WhatsApp. Email is core;
phone/WhatsApp lean on connectors.

---

## 4. Authentication features (NO verification backend)

Standard Keycloak credential primitives — run **entirely inside the IdP** (Postgres only).
No api-connector, no KYC, no MinIO/ES.

| Feature | Extra deps |
|---|---|
| **Biometric** (register/verify) | none |
| **Passkeys / FIDO** | none (FIDO asset-link realm attrs are config) |
| **Device** register / verify | none |
| **Web2Device** (web login → approve on phone) | socket server |
| Push-to-device approval | socket + Firebase (mobile push) |

---

## 5. Deployment profiles

**Lean passwordless IdP** (biometric + passkeys + device + login/MFA):
> PostgreSQL + SMTP `(+ socket for web-to-device)`

**IdP + federation verifications** (DigiLocker / UAE Pass):
> above + the connector jar(s) + each provider's gov/OAuth credentials

**Full verify platform** (KYC data lookups + liveness + videoKYC):
> PostgreSQL + MinIO + Elasticsearch + SMTP + api-connector + ezto-background +
> ezto-socket + ezto-liveliness + ezto-video-kyc + KYC vendor subscription

---

## 6. Reference deployment

`examples/deploy/gateway/` — nginx reverse proxy (TLS, multi-tenant wildcard host,
MinIO public-asset routing). Reference only; adapt domain/certs/upstreams.
