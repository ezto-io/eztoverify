# Third-party notices: EztoVerify account theme (OSS)

The OSS account theme (`themes/qlik_verify/account`) is part of EztoVerify, AGPL-3.0. It ships the third-party components
below under their own licences. This is a technical inventory for review, not legal advice.

## Keycloak

The account pages (`account.ftl`, `password.ftl`, `totp.ftl`, `sessions.ftl`, `applications.ftl`, `federatedIdentity.ftl`,
`log.ftl`, `resources.ftl`, `resource-detail.ftl`, `template.ftl`) and the message bundles follow the Keycloak 18 account
theme (Copyright Red Hat, Inc. and/or its affiliates and other contributors), Apache License 2.0, with EztoVerify's
changes; `mfa.ftl`, `subscription.ftl`, `preview_invoice.ftl`, `purchase-status.ftl` and `group-list.ftl` are EztoVerify's
own. The Apache-2.0 licence text is in the Keycloak source tree of this repository (`idp/keycloak/LICENSE.txt`).

## JavaScript libraries

Shipped as released (verified against the npm releases); where a shipped copy differs from the release, the difference
is noted.

| File (`resources/js/`) | Component | Version | Licence |
|---|---|---|---|
| `jquery.min.js` | jQuery (re-minified; the shipped copy has no licence banner) | 3.5.0 | MIT, Copyright OpenJS Foundation and other contributors |
| `bootstrap.min.js` | Bootstrap (JavaScript) | 4.0.0 | MIT, Copyright The Bootstrap Authors, Twitter, Inc. |
| `tele-input/tele-input.min.js`, `tele-input/flags.png`, `tele-input/flags@2x.png` | intl-tel-input | 17.0.0 | MIT, Copyright Jack O'Connor |
| `tele-input/tele-input.min.css` | intl-tel-input stylesheet, with EztoVerify's edits (`.iti` as a block, the flag images next to the file, a country-list position rule) | 17.0.0 | MIT, Copyright Jack O'Connor |
| `tele-input/utils.js` | libphonenumber (Google), as bundled by intl-tel-input | n/a | Apache-2.0, Copyright The Libphonenumber Authors |

Loaded at run time from a CDN (not shipped): iso-639-1 3.1.2 (cdn.jsdelivr.net), MIT, Copyright Meng Ting.

EztoVerify's own code: `main.js` and the pages' inline scripts. The password show/hide of `password.ftl` was rewritten
for the OSS release (it followed the purchased template's code).

## Stylesheets

- `bootstrap.min.css`: Bootstrap 4.0.0, MIT, Copyright The Bootstrap Authors, Twitter, Inc. (stock build; its notice is
  kept in the file).
- `account.css`, `account-rtl.css`, `icon-fonts.css`: written for the OSS theme (AGPL-3.0). `icon-fonts.css` draws each
  icon as an SVG outline made for this theme; the purchased template's icon fonts (osfont, feather, Picons Thin) were
  removed, and their `os-icon-*` class names are kept only as names in the markup.

## Fonts (`resources/fonts/`)

- **Inter** (`inter/`), Copyright 2020 The Inter Project Authors, SIL Open Font License 1.1. Licence text: `inter/OFL.txt`.
- **Rubik** (`Rubik-*.ttf`), Copyright 2015 The Rubik Project Authors, SIL Open Font License 1.1. Licence text:
  `Rubik-OFL.txt`.

## Images (`resources/img/`)

- `eztovrfy-logo.svg`, `eztoauth-logo.svg`, `logo.png`, `eztovrfy-favicon.ico`, `eztoauth-favicon.ico`: EztoVerify /
  EztoAuth logos and favicons, trademarks of their owner. The AGPL licence of the code grants no trademark rights.
- `failure.svg`, `success.svg`: EztoVerify's own.
- `no-policy.svg`: generated with AI (Claude, Anthropic) for the OSS theme and reviewed by a person; its metadata carries
  IPTC `DigitalSourceType` = `trainedAlgorithmicMedia`. AGPL-3.0.
