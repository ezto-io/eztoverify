# Third-party notices: EztoVerify login theme (OSS)

The OSS login theme (`themes/qlik_verify/login`, the back-office pages deployed into it, and the copies served from
`/auth/assets`) is part of EztoVerify, AGPL-3.0. It ships the third-party components below under their own licences.
This is a technical inventory for review, not legal advice.

## Keycloak

The pages that share their name with a page of the Keycloak 18 login theme (`code.ftl`, `delete-account-confirm.ftl`,
`error.ftl`, `info.ftl`, `login.ftl`, `login-config-totp.ftl`, `login-config-totp-text.ftl`, `login-idp-link-confirm.ftl`,
`login-idp-link-email.ftl`, `login-oauth-grant.ftl`, `login-otp.ftl`, `login-page-expired.ftl`, `login-password.ftl`,
`login-reset-password.ftl`, `login-update-password.ftl`, `login-update-profile.ftl`, `login-verify-email.ftl`,
`login-verify-email-code-text.ftl`, `login-x509-info.ftl`, `logout-confirm.ftl`, `register.ftl`, `saml-post-form.ftl`,
`select-authenticator.ftl`, `template.ftl`, `terms.ftl`, `webauthn-authenticate.ftl`, `webauthn-error.ftl`,
`webauthn-register.ftl`), the pages EztoVerify derived from them under other names (`fido-register.ftl`,
`fido-authenticate.ftl` and `fido-error.ftl` from the WebAuthn pages, `mfa-otp-reg.ftl` from `login-config-totp.ftl`,
`login-broker-update-profile.ftl` from `login-update-profile.ftl`) and the message bundles follow the Keycloak 18 login
theme (Copyright Red Hat, Inc. and/or its affiliates and other contributors), Apache License 2.0, with EztoVerify's
changes. The other pages are EztoVerify's own, written on Keycloak's page layout.
The Apache-2.0 licence text is in the Keycloak source tree of this repository (`idp/keycloak/LICENSE.txt`).

## JavaScript libraries
Shipped as released (verified against the npm releases, migration record `migration/js/analysis/libraries.json` in the
harness); where a shipped copy differs from the release, the difference is noted.

| File (`resources/js/`) | Component | Version | Licence |
|---|---|---|---|
| `jquery.js` | jQuery | 3.7.1 | MIT |
| `jquery-migrate.js` | jQuery Migrate (re-minified, with `jQuery.migrateMute` set to silence its console warnings) | 3.0.1 | MIT |
| `bootstrap.js` | Bootstrap (JavaScript) | 4.5.0 | MIT |
| `popper.js` | Popper.js (Federico Zivolo) | 1.14.7 | MIT |
| `select2.js` | Select2 | 4.1.0-rc.0 | MIT |
| `flatpickr.js`, `flatpickr.min.js` | flatpickr | 4.6.13 | MIT |
| `quill.js` | Quill (its default link `quilljs.com` changed to `ezto.io`); copyright notices in `quill.js.LICENSE.txt` next to `quill.js` and `/auth/assets/js/quill.min.js`, licence text below | 2.0.1 | BSD-3-Clause, Copyright (c) 2017-2024 Slab, (c) 2014 Jason Chen, (c) 2013 salesforce.com |
| `socket.js` | Socket.IO client | 4.3.2 | MIT |
| `pdf.js` | PDF.js (pdfjs-dist, Mozilla) | 2.0.385 | Apache-2.0 |
| `tele-input/tele-input.js`, `tele-input/tele-input.min.css`, `tele-input/flags*.png` | intl-tel-input | 17.0.0 | MIT |
| `tele-input/utils.js` | libphonenumber (Google), as bundled by intl-tel-input | n/a | Apache-2.0 |
| `iso.js` | iso-639-1 | 3.1.2 | MIT |
| `base64url.js` | adapted from rfc4648.js (William Swanson) | n/a | MIT |

Loaded at run time from CDNs (not shipped):
- jQuery 3.7.1 slim (code.jquery.com, `template.ftl`), MIT;
- PDF.js, pdfjs-dist 2.0.385 (`cdn.jsdelivr.net/npm/pdfjs-dist@2.0.385/build/pdf.min.js`, `bo-terms.ftl`), Apache-2.0;
- MediaPipe Tasks Vision 0.10.18 (`cdn.jsdelivr.net/npm/@mediapipe/tasks-vision@0.10.18`, used by `detector.js`),
  Apache-2.0;
- the MediaPipe vision WASM files and the Google MediaPipe Face Landmarker and Hand Landmarker models
  (`face_landmarker.task`, `hand_landmarker.task`) that `detector.js` loads from
  `cdn.jsdelivr.net/gh/ezto-io/ezto-js@1.0.3/liveness/mediapipe/`, Apache-2.0.

EztoVerify's own code: `main.js`, `ezto.js`, `liveness.js`, `selfieRegister.js`, `detector.js`, `dynamicForm.js`,
`ocrTemplate.js`, `otpTemplate.js`, `inputFieldTemplate.js`, `common.js`. The purchased template's script code that
`main.js` and some pages carried was removed or rewritten before the OSS release (JS migration record in the harness).

## Stylesheets
- `normalize.css` 8.0.1, MIT.
- `flatpickr.css`, `select2.css`, `quill.snow.css`: from the libraries above, under the same licences.
- `main*.css`, `bootstrap*.css`, `icons.css`, `liveness*.css`, `backoffice.css`: written for the OSS theme (AGPL-3.0).
- `dynamic-form.css`: EztoVerify's own stylesheet for the dynamic forms (AGPL-3.0).
- The previous purchased template stylesheets and icon font were removed.

## Fonts
- **Inter** (The Inter Project Authors), SIL Open Font License 1.1. It is served from
  `services/qlik-verify-services/src/main/resources/META-INF/resources/assets/fonts/text-font/inter/`, and the licence text
  is in `OFL.txt` in that folder. The login theme's own copy (`resources/fonts/text-font/inter/`) has the same `OFL.txt`.

## Images and icons
The origin, licence and decision for every image the theme references, and the evidence behind each decision, are kept in
the OSS migration proof bundle (`evidence/5-ip-and-assets/`), outside this repository. In short:
- **Icon-set matches:**
  - `camera.svg`: Bootstrap Icons (MIT, Copyright The Bootstrap Authors);
  - `retry.svg`: System UIcons (Unlicense).
  - `upload.svg` matched Unicons by IconScout, whose licence could not be confirmed for this copy: it is replaced with an
    AI-generated drawing (below), 2026-10-09.
- **AI-generated images** (drawn for this theme, AGPL-3.0): each carries IPTC/XMP `DigitalSourceType` =
  `trainedAlgorithmicMedia` in its metadata, and they are listed in the proof bundle's `ASSETS.md`. This includes the neutral
  replacements for images that showed government emblems or official logos, and for two icons of unknown origin
  (PR #10 review, 2026-10-09).
- **EztoVerify / EztoAuth logos and favicons:** trademarks of their owner. The AGPL licence of the code grants no
  trademark rights.

## Removed
- `vendor/jquery.counterup.js` and its `/auth/assets/js/vendor/jquery.counterup.min.js` copy (jquery.counterup 1.0, GPL-2.0).
  GPL-2.0-only code in an AGPL-3.0 project is a potential licence-compatibility concern. No page or script used it, so
  removing it changes no behaviour (decision by grootan, 2026-10-09).
- `/auth/assets/js/video-kyc.min.js`: VideoSDK's prebuilt meeting loader (`@videosdk.live/rtc-js-prebuilt` 0.3.43, with
  branding options added), a client of the paid hosted service videosdk.live (`embed.videosdk.live`, `api.videosdk.live`).
  Nothing in this repository loaded it; the Video KYC flow uses its own iframe (`EZTO_VERIFY_VIDEO_SDK_URL`). Dropped
  from the OSS release (PR review decision, 2026-10-14).

## Licence texts

### Quill (BSD-3-Clause)

From `quill@2.0.1` (`LICENSE`):

```text
Copyright (c) 2017-2024, Slab
Copyright (c) 2014, Jason Chen
Copyright (c) 2013, salesforce.com
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:

1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its
   contributors may be used to endorse or promote products derived from
   this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
```
