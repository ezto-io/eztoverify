# Branding & Flavor Generator

A Dart command-line tool that **white-labels** the Verify Authenticator app. It turns a
declarative per-flavor config (app name, bundle IDs, colors, logos, signing, push configs,
deeplinks) into the concrete platform files Flutter needs — `AndroidManifest.xml`,
`build.gradle`, `Info.plist`, launcher icons, `google-services.json`, keystores, entitlements,
and the Dart flavor constants under `lib/generated/`.

The active flavor is **baked in at generate time**, not chosen at `flutter run`. You generate a
flavor once, then build/run normally.

---

## How it works

```
lib/branding/config/flavor/<flavor>/   ← you author these (source of truth per flavor)
   ├── app_info.dart        app name, bundle IDs, terms/privacy URLs, encryption keys, update URLs
   ├── app_signing.dart     iOS provisioning profile id, team id, macOS profile name
   ├── keystore_info.dart   Android keystore alias / env-var names
   ├── permissions.dart     platform permission set
   ├── theme.dart           primary color, font
   ├── deeplinks.dart       associated domains (App Links / Universal Links)
   ├── index.dart           re-exports the above
   ├── assets/              logos (transparent / full / simple / white)
   ├── keystore/            android_keystore.keystore        (NOT in repo — see Security)
   └── push/                google-services.json,
                            GoogleService-Info.plist         (NOT in repo — see Security)
                ↓
        dart run bin/branding.dart --generate --flavor=<flavor>
                ↓
lib/branding/template/*    ← skeletons (manifest, gradle, plist, entitlements, icons.yaml)
                ↓
   writes into the live project:
     ../android/app/{google-services.json, <flavor>.keystore, build.gradle, AndroidManifest.xml}
     ../ios/Runner/{GoogleService-Info.plist, Info.plist, Runner.entitlements}
     ../macos/Runner/*    ../windows/runner/*
     ../lib/generated/flavor/*   (Dart constants the app reads at runtime)
```

`base_flavor/` is the template flavor copied when you create a new one. `base/` holds the abstract
base classes (`BaseAppInfo`, `BaseAppSigning`, …) each flavor extends.

---

## Commands

All commands run from this `branding/` directory.

```bash
cd branding
```

| Action | Command |
|---|---|
| List flavors | `dart run bin/branding.dart --list` |
| Generate (switch active flavor) | `dart run bin/branding.dart --generate --flavor=<flavor>` |
| Show flavor details | `dart run bin/branding.dart --details --flavor=<flavor>` |
| Create new flavor from JSON | `dart run bin/branding.dart --create --path=<requirement.json>` |
| Build/release a platform | `dart run bin/branding.dart --build --platform=<platform> --flavor=<flavor>` |
| Help | `dart run bin/branding.dart --help` |

`<platform>` ∈ `android` · `ios` · `macos` · `windows`.

Interactive menu (same actions, prompted):

```bash
dart run            # then pick 1–5
# 1 List  2 Build/Switch  3 Details  4 Create  5 Release
```

CI flag: append `--cicd` (`-a`) to `--create` to skip interactive Apple-store steps.

---

## Extending — add a new flavor (white-label)

1. Author a requirement JSON. Binary inputs (firebase config, keystore, logos) are **base64**.

   ```json
   {
     "app_name": "Acme Verify",
     "flavor_name": "acme_prod",
     "android_app_bundle_id": "com.acme.verify",
     "ios_app_bundle_id": "com.acme.verify",
     "macos_app_bundle_id": "com.acme.verify",
     "windows_app_bundle_id": "com.acme.verify",
     "support_platforms": ["android", "ios", "macos", "windows"],
     "primary_color": "#0A5CFF",
     "font_theme": "Inter",
     "terms_and_condition": "https://acme.com/terms",
     "privacy_policy": "https://acme.com/privacy",
     "android_firebase_json": "<base64 google-services.json>",
     "android_keystore_file": "<base64 .keystore>",
     "android_key_alias": "acme",
     "android_key_password": "…",
     "android_store_password": "…",
     "app_transparent_logo": "<base64 png>",
     "app_full_logo": "<base64 svg>",
     "app_logo_simple": "<base64 svg>",
     "app_logo_white": "<base64 svg>"
   }
   ```

2. Create it:

   ```bash
   dart run bin/branding.dart --create --path=./acme_prod.json
   ```

   This scaffolds `lib/branding/config/flavor/acme_prod/` from `base_flavor/`, writes the config
   Dart files, drops logos into `assets/`, and decodes the keystore + firebase json into place.

3. Fill the secrets the JSON does **not** carry — edit `acme_prod/app_info.dart`:
   - `encryption` → `mobileKey` / `desktopKey` (obtained out-of-band; placeholders `{MOBILE_KEY}`/`{DESKTOP_KEY}`)
   - `urls.macUpdate` / `urls.windowsUpdate` → your desktop auto-update feed URLs
   - `acme_prod/deeplinks.dart` → your associated domain(s)
   - `acme_prod/app_signing.dart` → iOS provisioning profile id + team id, macOS profile name

4. Generate and run:

   ```bash
   dart run bin/branding.dart --generate --flavor=acme_prod && cd ..
   flutter run
   ```

---

## Adding the keystore & google-services.json (and how they get copied)

These files are **not in the repo**. You drop them into the flavor's `keystore/` and `push/`
folders, then `--generate` copies them into the live Android/iOS project. The copy is done by
`PushService` (`lib/service/push_service.dart`) every time you generate that flavor.

**1. Place the files** (create the folders if missing):

```
lib/branding/config/flavor/<flavor>/keystore/android_keystore.keystore   ← Android signing key
lib/branding/config/flavor/<flavor>/push/google-services.json            ← Android FCM
lib/branding/config/flavor/<flavor>/push/GoogleService-Info.plist        ← iOS APNs/Firebase
```

The filenames are fixed — name them exactly as above or the copy step fails.

**2. Generate** — copies them into the real app:

```bash
cd branding
dart run bin/branding.dart --generate --flavor=<flavor>
```

**3. What `--generate` copies** (source → destination, relative to repo):

| Source (in branding) | Destination (live app) | Done by |
|---|---|---|
| `…/<flavor>/push/google-services.json` | `android/app/google-services.json` | `moveAndroidFirebaseAndKeystoreConfig` |
| `…/<flavor>/keystore/android_keystore.keystore` | `android/app/<flavor>.keystore` (`_`→`-`, e.g. `acme_prod`→`acme-prod.keystore`) | `moveAndroidFirebaseAndKeystoreConfig` |
| `…/<flavor>/push/GoogleService-Info.plist` | `ios/Runner/GoogleService-Info.plist` | `moveIosFirebasePushConfig` |

Notes:
- The Android keystore is renamed on copy: underscores in the flavor name become hyphens. The
  generated `build.gradle` references `app/<flavor-with-hyphens>.keystore`, so keep the source file
  named `android_keystore.keystore` and let the tool rename it.
- The iOS plist copy is **skipped when `isIosCiCd == true`** (CI injects the plist) — set this in
  `<flavor>/app_info.dart`.
- These destinations (`android/app/google-services.json`, `ios/Runner/GoogleService-Info.plist`,
  `android/app/*.keystore`) are gitignored — they are *build outputs* of generate, not committed.
- Re-running `--generate` for another flavor **overwrites** the destination files. The live app
  always reflects the last-generated flavor.

---

## Push services

The app supports Firebase push on mobile plus a desktop socket fallback. The branding tool only
*places the config files*; the runtime wiring lives in
`lib/core/services/notification_service/`.

| Platform | Transport | Config placed by `--generate` | Runtime |
|---|---|---|---|
| Android (GMS) | Firebase Cloud Messaging | `push/google-services.json` → `android/app/` | `push_notification_service.dart` |
| iOS | APNs via Firebase | `push/GoogleService-Info.plist` → `ios/Runner/` | `push_notification_service.dart` |
| macOS / Windows | App **socket** + local notification | — (uses socket URL from `lib/main.dart`) | `push_socket_service.dart` |

Notes & recommendations:

- **Firebase is the default everywhere mobile.** Supply a valid `google-services.json` /
  `GoogleService-Info.plist` per flavor or push silently no-ops.
- **Huawei / HMS is not supported.** The Huawei Push Kit path was removed (no proprietary HMS
  SDKs in OSS). Huawei devices without Google Play Services will not receive push.
- **Desktop has no FCM.** macOS/Windows receive transactions over the app socket
  (`PushSocketService`) and raise a *local* OS notification — Windows via `windows_notification`
  (toast `applicationId`), macOS/Linux via `flutter_local_notifications`. The Windows toast
  launches the app back via the `eztovrfy://` deeplink with a base64 payload. **Set your own
  socket URL** in `lib/main.dart` (placeholder `{your_socket_url}`) and your own
  `applicationId`/scheme if rebranding.
- **iOS CI/CD:** when `isIosCiCd == true`, the plist copy is skipped (CI injects it) — see
  `PushService.moveIosFirebasePushConfig`.

---

## Building & running

> Generate the flavor **before** building. Set Android signing env vars first (below).

### Android
```bash
dart run bin/branding.dart --generate --flavor=<flavor> && cd ..
export <FLAVOR>_KEY_ALIAS=…  <FLAVOR>_KEY_PASSWORD=…  <FLAVOR>_STORE_PASSWORD=…
flutter build apk          # → build/app/outputs/flutter-apk/app-release.apk
flutter build appbundle    # → build/app/outputs/bundle/release/app-release.aab
```
Env-var prefix = flavor name upper-cased (e.g. `EZTO_PROD_KEY_ALIAS`). FIDO/Passkey needs a
**release** build on Android 13+ (debug keystore fingerprint isn't in `assetlinks.json`).

### iOS
Set provisioning profile id + team id in `<flavor>/app_signing.dart`, then:
```bash
flutter build ipa --release --export-options-plist=/path/ExportOptions.plist
```

### macOS (desktop)
```bash
dart run bin/branding.dart --generate --flavor=<flavor> && cd ..
flutter run -d macos                 # run
flutter build macos                  # → build/macos/Build/Products/Release/<App>.app
# or via the tool:
cd branding && dart run bin/branding.dart --build --platform=macos --flavor=<flavor>
```
Requires the macOS profile name in `app_signing.dart` and notification + network entitlements
(generated from `template/macos_release.entitlements`). Auto-update reads `urls.macUpdate`.

### Windows (desktop)
```bash
dart run bin/branding.dart --generate --flavor=<flavor> && cd ..
flutter run -d windows               # run
flutter build windows                # → build/windows/x64/runner/Release/
# or:
cd branding && dart run bin/branding.dart --build --platform=windows --flavor=<flavor>
```
Windows push uses the toast `applicationId` in `push_socket_service.dart` and the `eztovrfy://`
URI scheme — register the scheme and match the `applicationId` to your rebrand. Auto-update reads
`urls.windowsUpdate`.

> Build the whole desktop app + installer pipeline via menu **option 5 → 3/4** or the
> `--build --platform=macos|windows` command, which runs `flutter clean`, `pub get`, then the
> release build.

---

## Security — files NOT in the repo

These are gitignored and **must be supplied per flavor** before building. Never commit real ones.

```
lib/branding/config/flavor/<flavor>/keystore/android_keystore.keystore
lib/branding/config/flavor/<flavor>/push/google-services.json
lib/branding/config/flavor/<flavor>/push/GoogleService-Info.plist
```

Plus secrets edited directly into `<flavor>/app_info.dart` (`encryption` keys) and signing values
passed via environment variables (never hard-coded). See the top-level
`authenticator/README.md` for the full required-files checklist and device-support matrix.
