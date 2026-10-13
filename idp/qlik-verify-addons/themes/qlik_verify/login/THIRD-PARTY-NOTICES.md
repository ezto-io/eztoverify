# Third-party notices: EztoVerify login theme (OSS)

The OSS login theme (`themes/qlik_verify/login`, the back-office pages deployed into it, and the copies served from
`/auth/assets`) is part of EztoVerify, AGPL-3.0. It ships the third-party components below under their own licences.
This is a technical inventory for review, not legal advice.

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
| `quill.js` | Quill (its default link `quilljs.com` changed to `ezto.io`) | 2.0.1 | BSD-3-Clause |
| `socket.js` | Socket.IO client | 4.3.2 | MIT |
| `pdf.js` | PDF.js (pdfjs-dist, Mozilla) | 2.0.385 | Apache-2.0 |
| `tele-input/tele-input.js`, `tele-input/tele-input.min.css`, `tele-input/flags*.png` | intl-tel-input | 17.0.0 | MIT |
| `tele-input/utils.js` | libphonenumber (Google), as bundled by intl-tel-input | n/a | Apache-2.0 |
| `iso.js` | iso-639-1 | 3.1.2 | MIT |
| `base64url.js` | adapted from rfc4648.js (William Swanson) | n/a | MIT |
| `/auth/assets/js/video-kyc.min.js` | VideoSDK prebuilt meeting SDK (`@videosdk.live/rtc-js-prebuilt`, videosdk.live), with EztoVerify's branding options added | 0.3.42 - 0.3.43 | MIT (as declared by its npm package; the package ships no licence file) |

Loaded at run time from CDNs (not shipped):
- jQuery 3.7.1 slim (code.jquery.com), MIT;
- MediaPipe Tasks Vision 0.10.18 (cdn.jsdelivr.net, used by `detector.js`), Apache-2.0.

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
