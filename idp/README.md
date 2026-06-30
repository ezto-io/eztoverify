## QLIKVERIFY

The Verify IdP — a **Keycloak 18 (Quarkus)** fork extended with verification workflows, multi-tenant
"workspaces", pluggable notification providers, and event streaming. Standard OIDC/SAML from the
Keycloak core still applies; the sections below list what this fork adds (with code locations under
`qlik-verify-addons/`).

### Capabilities

**Authentication & verification workflows**

Composed into per-tenant flows from the auth-template marketplace
(`qlik-verify-addons/services/qlik-verify-marketplace/.../templates/authentication/`) and the
backoffice required-actions/authenticators (`qlik-verify-addons/services/qlik-verify-backoffice/`):

- Email + password / username + password
- OTP — email, SMS (phone), WhatsApp
- Email magic link (passwordless)
- Push authentication via **CIBA** (`backoffice/authenticators/CibaAuthenticator.java`) — async approve
  on the authenticator app
- **FIDO2 / WebAuthn passkeys** (`backoffice/requiredactions/FidoPasswordless{Register,Authenticator}.java`,
  marketplace `EmailWebAuthTemplate`)
- **Biometric** fingerprint/face (`backoffice/requiredactions/Biometric.java`)
- **Selfie** capture + **liveness / face-match** (`backoffice/requiredactions/Selfie*.java`; ML in
  `services/liveness/`)
- Device registration/verification & binding (`backoffice/requiredactions/Device{Register,Verify}.java`)
- WhatsApp QR / push (marketplace `Whatsapp{Qr,Push}Template`), Telegram, secure messaging,
  proximity & geo-tagging (`backoffice/requiredactions/`)
- Email / phone / WhatsApp validation required-actions

**Notification / delivery channels**

- Email providers (`services/qlik-verify-services/.../emails/`): **SMTP, SparkPost, Amazon SES,
  Mandrill, SendGrid, Mailgun**
- SMS providers (`services/qlik-verify-services/.../sms/`): **Twilio, Vonage, Retarus**, plus
  **WhatsApp**
- Web / native **push** (`services/qlik-verify-extensions/.../webPush/`; Firebase / Huawei assets via
  the FIDO/push workspace attributes)

**Result & event delivery**

- Realtime push to web/mobile via the **socket** service; CIBA results delivered by **webhook**
  (`backoffice/notifiers/HookNotificationProvider.java`) or **Kafka queue**
  (`backoffice/notifiers/QueueNotificationProvider.java`)
- Event-streaming fan-out (`services/qlik-verify-services/.../eventlisteners/`): **Apache Kafka,
  API & WebHooks, AWS EventBridge, Datadog, Splunk**, plus anomaly / last-login / blacklist and
  Azure AD / Google / Okta / Duo directory-sync listeners

**Multi-tenancy** — isolated workspaces (realms) with per-tenant branding, providers, audit settings,
brute-force + anomaly defenses, and a guided onboarding wizard for the root workspace.

### Prerequisites

- [**jenv**](https://www.jenv.be/) — manages the active Java version. **Build AND run require JDK 11**
  (JDK 21 breaks Byte Buddy / Quarkus augmentation). Install: `brew install jenv`, then register a
  JDK 11 and pin it in this dir:
  ```
  jenv add /path/to/jdk-11-home
  jenv local 11
  java -version   # must report 11
  ```

### How to build from scratch

Arguments:
v - version
c - customer name - valid input eztoverify - if this argument is unset it will build for qlikverify
p - perform packaging - This argument is used for pipeline. by default it is set to false

```
./build.sh -v <ver> -c <build for> -p false
```

eg: To build qv
```
./build.sh -v 2.6.1 -p false
```

### How run the project

Run MINIO in docker

```
./minio-docker.sh
```

Set Environment Variables

```
source evn.sh
```

Goto qlik_verify_<ver>/build<ver>/keycloak-<ver>/keycloak
and run

```
./bin/standalone.sh --debug

```

### How to debug code with intellij

- Goto Edit Configurations of the project

- Add new Configuration and select Remote JVM Debug

- Provide Host as localhost and port 8787 (Default debug port for the project)

- Click debug icon after running the project

