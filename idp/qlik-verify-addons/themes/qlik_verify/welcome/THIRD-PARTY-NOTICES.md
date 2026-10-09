# Third-party notices: welcome theme (`themes/qlik_verify/welcome`)

The welcome theme is part of EztoVerify, released under AGPL-3.0. It includes or loads the third-party components below,
each under its own licence. These are technical records for review, not legal advice. The evidence for every image and
font decision is kept in the OSS migration proof bundle (`evidence/5-ip-and-assets/`), outside this repository.

## JavaScript and CSS shipped in this theme
- **Alpine.js 3.10.0** (`resources/js/alpine.min.js`), MIT, Copyright Caleb Porzio and contributors.
- **Alpine.js 2.8.2, IE11 build** (`resources/js/alpine.ie11.min.js`, minified by jsDelivr), MIT, Copyright Caleb Porzio and contributors.
  It bundles a Polymer Project polyfill (BSD-3-Clause, "Copyright (c) 2016 The Polymer Project Authors. All rights reserved.";
  licence: http://polymer.github.io/LICENSE.txt), whose notice is kept in the file.
- **intl-tel-input 17.0.0** (`resources/js/tele-input/tele-input.min.js`, `tele-input.min.css`, `flags.png`, `flags@2x.png`),
  MIT, Copyright Jack O'Connor.
- **libphonenumber, intl-tel-input utilities build** (`resources/js/tele-input/utils.js`), Apache-2.0, Copyright Google Inc.
- **Tailwind CSS 3.2.2** (`resources/css/dist.css`, `dist-rtl.css`: a build of EztoVerify's `source.css` with
  `tailwind.config.js`), MIT, Copyright Tailwind Labs, Inc. Its base styles include modern-normalize (MIT).

## Loaded from CDNs by `index.ftl` (not shipped)
- **jQuery 3.6.0** (code.jquery.com), MIT, Copyright OpenJS Foundation and other contributors.
- **Select2 4.1.0-rc.0** (cdn.jsdelivr.net), MIT, Copyright Kevin Brown, Igor Vaynberg and contributors.
- **iso-639-1 3.1.2** (cdn.jsdelivr.net), MIT, Copyright Meng Ting.

## Fonts
- **Inter 3.019** (The Inter Project Authors), SIL Open Font License 1.1: `resources/fonts/inter/`, licence text in `OFL.txt` there.
- **Open Sans 3.000** (The Open Sans Project Authors), SIL Open Font License 1.1: `resources/fonts/open-sans/`, licence text in `OFL.txt` there.

## Images
- **AI-generated images** (drawn for this theme, AGPL-3.0): each carries IPTC/XMP `DigitalSourceType` =
  `trainedAlgorithmicMedia` in its own metadata. This includes the provider logos, which are neutral artwork rather than
  the providers' own marks, and the menu and status icons.
- **EztoVerify / EztoAuth logos and favicons:** trademarks of their owner. The AGPL licence of the code grants no
  trademark rights.

## Removed (2026-10-09, decisions by grootan)
- The purchased template's stylesheets, fonts and scripts that the page never loaded: `main.css` / `main-rtl.css` (with
  their `.scss` and `.map`), `welcome.css`, `animate.css`, `aos.css`, `bootstrap*.css`, `normalize.css`, `swiper.min.css`,
  `icons.css` and its `The-Icon-of` font, `main.js`, `bootstrap.min.js`, `popper.min.js`, `jquery-3.5.0.js`,
  `jquery-migrate.min.js` and `js/vendor/*` (including `jquery.counterup.min.js`, GPL-2.0).
- `resources/fonts/password.ttf`: a Monotype font whose embedded licence forbids copying or distribution. The secret-key
  inputs are masked with CSS (`-webkit-text-security: disc`) instead.
- Images the page never references.
