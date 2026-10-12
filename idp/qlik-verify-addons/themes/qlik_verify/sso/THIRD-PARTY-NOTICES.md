# Third-party notices: SSO theme (`themes/qlik_verify/sso`)

The SSO dashboard theme is part of EztoVerify, released under AGPL-3.0. It includes or loads the third-party components
below, each under its own licence. These are technical records for review, not legal advice. The evidence for every
image and font decision is kept in the OSS migration proof bundle (`evidence/5-ip-and-assets/`), outside this repository.

## JavaScript and CSS shipped in this theme
- **Alpine.js 3.13.10** (`resources/js/alpine.min.js`, loaded by `index.ftl`), MIT, Copyright Caleb Porzio and contributors.
- **Tailwind CSS 3.4.1** (`resources/css/dist.css`, `dist-rtl.css`: a build of EztoVerify's `style.css` with
  `tailwind.config.js`), MIT, Copyright Tailwind Labs, Inc. Its base styles include modern-normalize (MIT).

## Loaded from a CDN by `index.ftl` (not shipped)
- **iso-639-1 3.1.2** (cdn.jsdelivr.net), MIT, Copyright Meng Ting.

## Fonts
- **Inter 3.019** (The Inter Project Authors), SIL Open Font License 1.1: `resources/fonts/inter/`, licence text in `OFL.txt` there.

## Images
- **AI-generated images** (drawn for this theme, AGPL-3.0): each carries IPTC/XMP `DigitalSourceType` =
  `trainedAlgorithmicMedia` in its own metadata. This includes the app-download icons, which are neutral device
  drawings (laptop, monitor, phones) rather than the Apple, Microsoft, App Store or Google Play marks.
- **EztoVerify / EztoAuth logos and favicons:** trademarks of their owner. The AGPL licence of the code grants no
  trademark rights.

## Removed (2026-10-09, decisions by grootan)
- `template.ftl`: imported by no SSO page (only `index.ftl` is rendered); it was the only file that loaded the purchased
  admin-template stylesheets below.
- `resources/css/account.css` and `account-rtl.css` (the purchased admin-dashboard template stylesheet) and
  `resources/css/bootstrap.min.css`: loaded only by `template.ftl`.
- `resources/js/main.js`, `jquery.min.js`, `bootstrap.min.js`, `jquery-migrate.min.js`: loaded by no SSO page.
- Fonts and images the theme never references (Rubik, Inter Black / ExtraLight / Thin, unused icons).
- `theme.properties` is unchanged: its `styles` and `scripts` entries were read only by `template.ftl`.
