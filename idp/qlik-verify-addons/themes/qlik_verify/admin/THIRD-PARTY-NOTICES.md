# Third-party notices: EztoVerify admin console theme (OSS)

The OSS admin theme (`themes/qlik_verify/admin`, and the images EztoVerify's build copies into it from
`services/qlik-verify-services/theme/admin/img`) is part of EztoVerify, AGPL-3.0. It ships the third-party components
below under their own licences. This is a technical inventory for review, not legal advice. **The full licence text of
each component, taken from its own release at the shipped version, is in [`LICENSES/`](LICENSES/README.md).** The evidence
for each entry (npm comparisons, checksums, decisions) is kept in the OSS migration proof bundle, outside this repository.

## Keycloak

The admin console application (`resources/js`, `resources/partials`, `resources/templates`, `index.ftl`, `messages`) is
based on the Keycloak 18 admin console (Copyright Red Hat, Inc. and/or its affiliates and other contributors), Apache
License 2.0, with EztoVerify's changes. The Apache-2.0 licence text is in the Keycloak source tree of this repository
(`idp/keycloak/LICENSE.txt`). Select2 3.x used by the console is served by Keycloak's common resources, not by this theme.

## JavaScript

Bundled libraries (`resources/themes/js/main_vendors.js`, `vendors.js`), shipped as they were; versions identified by
comparison with the npm releases (some sections lost their licence headers in the bundle build):

| Component | Version | Licence |
|---|---|---|
| jQuery | 3.5.1 | MIT |
| AngularJS, angular-resource, angular-route, angular-cookies, angular-sanitize | 1.8.0 | MIT |
| angular-translate, angular-translate-loader-url | 2.18.2 | MIT |
| Angular UI Bootstrap (ui-bootstrap-tpls) | 0.11 / 0.12 | MIT |
| angular-file-upload (danialfarid) | 1.x | MIT |
| angular.treeview (eu81273) | not on npm | requires human review |
| CodeMirror (core, xml, htmlmixed modes) | 5.52 - 5.58 | MIT |
| Ace (ace-builds) | 1.2.3 | BSD-3-Clause |
| angular-ui-ace | 0.2.3 | MIT |
| marked | 1.2.7 | MIT |
| intl-tel-input | 17.0.0 | MIT |
| libphonenumber (Google), as built into intl-tel-input `utils.js` | n/a | Apache-2.0 |
| Popper.js | 1.12.x - 1.13.0 | MIT |
| Bootstrap 4 JavaScript plugins (util, alert, button, collapse, dropdown, modal, tab) | 4.0.0 | MIT |
| bootstrap-validator (1000hz) | 0.11.9 | MIT |
| bootstrap-daterangepicker | 2.1.26 / 2.1.27 | MIT |
| mindmup-editable-table | unknown | requires human review |
| Drawflow | ~0.0.39 (patched) | MIT |
| Moment.js | 2.29.4 | MIT |
| angular-slideout-panel | unknown | requires human review |
| highlight.js (core and grammars) | 11.0.1 | BSD-3-Clause |
| highlightjs-curl | 1.2 / 1.3 | Apache-2.0 |
| highlightjs-line-numbers.js | 2.8.0 | MIT |
| angular-ui-select2 | 0.0.5 | MIT |
| autofill-event (Google) | 0.0.1 | MIT |
| FileSaver.js | 1.3.x | MIT |
| d3 | 3.5.17 | BSD-3-Clause |
| Pickr (@simonwep/pickr), with NanoPop 2.1.0 and core-js 3.15.1 | 1.8.2 | MIT |
| Chart.js, with @kurkle/color 0.2.1 | 3.8.0 | MIT |
| **Driver.js** (replaces Intro.js; `vendors.js` / `vendors.css`) | 1.8.0 | MIT (Kamran Ahmed) |

Other script files:

| File | Component | Version | Licence |
|---|---|---|---|
| `resources/themes/vendor/flatpickr-4.6.13/flatpickr.min.js`, `.min.css` | flatpickr (licence file alongside) | 4.6.13 | MIT (Gregory Petrosyan) |
| `resources/themes/js/quill.js` | Quill (its link to quilljs.com changed to ezto.io) | 2.0.1 | BSD-3-Clause (Slab, Jason Chen) |
| `resources/themes/css/quill.snow.css` | Quill snow theme | 2.0.2 | BSD-3-Clause |
| `resources/themes/js/curl_converter.js` | webpack build (theme-root `package.json` / `package-lock.json` / `webpack.config.js`) of EztoVerify's `resources/js/custom/curl_converter.js` with curlconverter 4.9.0, curl-parser 0.0.2, web-tree-sitter 0.20.8, jsesc 3.0.2, yamljs 0.3.0, lossless-json 2.0.11, path-browserify 1.0.1 | see left | MIT (libraries); EztoVerify (its source) |
| `resources/themes/js/tree-sitter.wasm` | web-tree-sitter runtime | 0.20.8 | MIT (Max Brunsfeld) |
| `resources/themes/js/main.js` (part) | js-sha256 (emn178), embedded as `sha256_imported` | 0.9.0 | MIT (Chen Yi-Cyuan) |
| `resources/js/md/marked.js` | marked (unused by the console) | 0.2.9 | MIT (Christopher Jeffrey) |
| `resources/themes/js/region-helper.js` | country list and postal address metadata (see "Address data" below) | snapshot, est. 2020-2022 | CC-BY 4.0 (address data), Unicode License (country names); requires human review |

Loaded at run time from cdn.jsdelivr.net (pinned versions, not shipped): iso-639-1 3.1.2 (MIT), uuid-js 0.7.5
(Apache-2.0).

## Address data

`resources/themes/js/region-helper.js`: the postal address metadata (`stateList`) is derived from Google's address data
service for libaddressinput (https://github.com/google/libaddressinput; data served at
https://chromium-i18n.appspot.com/ssl-address/data), Copyright Google Inc.; the libaddressinput README states the data is
licensed under CC-BY 4.0 (https://creativecommons.org/licenses/by/4.0/; source code: Apache-2.0). The data was captured
around 2020-2022 (no exact revision was recorded), reshaped (field names, `sub_labels`, `sub_latin_names`) and modified:
the regions AS, CC, CU, CX, FM, HM, IR, KP, MH, MP, NF, PW, SD, SY, UM, VI and some subdivisions (Crimea, Sevastopol,
Donetsk, Luhansk) are not included. The country names (`countryList`) are English region names from Unicode CLDR
(https://cldr.unicode.org), Copyright (c) 1991-2026 Unicode, Inc., used under the Unicode License
(https://www.unicode.org/license.txt). How the file entered the project and why those regions were removed require
human review (the migration's provenance record has the evidence).

## Stylesheets

- `resources/themes/css/main.css`, `main-rtl.css`: stock Bootstrap 4.0.0 (MIT, Copyright The Bootstrap Authors and
  Twitter, Inc.; its notice is kept in the file) and CSS written for EztoVerify (AGPL-3.0). `main-rtl.css` is generated
  from `main.css` with rtlcss.
- `resources/themes/css/icon-fonts.css`: generated for EztoVerify; it draws Bootstrap Icons (below).
- `resources/themes/css/vendors.css`: the stylesheets of the libraries above (DataTables Bootstrap styling 1.10.13,
  intl-tel-input 17.0.0, Drawflow, angular-slideout-panel, highlight.js github-dark 11.0.1, CodeMirror 5.52.2 base and
  material theme, Pickr nano 1.8.2: MIT / BSD-3-Clause; Select2 3.5.x: Apache-2.0 or GPL-2.0; Driver.js 1.8.0: MIT).
- `roles-group-tree.css`, `roles-group-tree-rtl.css`: EztoVerify.

## Fonts (`resources/themes/fonts/`)

- **Inter** (The Inter Project Authors), **Open Sans** (The Open Sans Project Authors) and **Rubik** (The Rubik Project
  Authors): SIL Open Font License 1.1. Licence texts: `inter/OFL.txt`, `open-sans/OFL.txt`, `Rubik-OFL.txt`.
- **Bootstrap Icons** 1.11.3 (`bootstrap-icons.woff2`, `.woff`): MIT, Copyright (c) 2019-2024 The Bootstrap Authors.

## Images

Every image's origin and licence is recorded in the proof bundle's `ASSETS.md`, and in each file's own metadata. In short:
- 122 icons draw Bootstrap Icons glyphs (MIT, The Bootstrap Authors), placed and coloured for this theme.
- 71 illustrations were generated with AI (Claude, Anthropic) from text descriptions, without access to the images they
  replace, and reviewed by a person; their metadata carries IPTC `DigitalSourceType` = `trainedAlgorithmicMedia`.
- 47 neutral text tiles name third-party providers (e.g. Google, Microsoft, Twilio) to identify their services; the
  names are their owners' trademarks.
- `themes/img/flags.png`, `flags@2x.png`: intl-tel-input 17.0.0, MIT, Copyright (c) 2014-2016 Jack O'Connor.
- EztoVerify / EztoAuth logos and favicons: trademarks of their owner. The AGPL licence of the code grants no trademark
  rights.

## Removed in the OSS migration

The purchased admin template's layer: its compiled Bootstrap build and template stylesheet sections, its icon fonts
(osfont, feather, themify, The-Icon-of, Picons Thin), its start-up script in `main.js`, its bower components (incl.
CKEditor 4.8) and icon-font pack, its images; Intro.js 3.4.0 (AGPL-3.0 or commercial), replaced by Driver.js.
