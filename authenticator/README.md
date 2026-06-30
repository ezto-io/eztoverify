# ezto Verify Authenticator

## Self-hosting: published app vs your own build

If you self-host the Verify platform, the first question is usually: can users install the
**published ezto Verify app** (App Store / Play Store) and point it at *my* backend, or do I
need to ship my own build?

Short answer: **the published app can do live push-approval against your backend, but not
passkeys or background notifications.** Two things are compiled into the binary per flavor and
cannot be changed in an already-installed app:

| Concern | Configurable per install? | Where it comes from |
|---|---|---|
| Backend / API host | **Yes — runtime** | `base_url` carried in the enrollment deeplink, stored per account (`ezto_verify_sdk/lib/service/app_links_service.dart`) |
| Socket URL (live delivery) | **Yes — runtime** | stored value, editable in-app (About → *Update Socket Url*); the built-in value is only a default |
| Live approval delivery | **Yes** | arrives over the socket (`PushSocketService.onPushReceived`) |
| Background / killed-state push | **No — baked** | `google-services.json` / `GoogleService-Info.plist` fix the FCM project; the app's push token is valid only for *that* project |
| Passkeys (FIDO/WebAuthn) + universal/app links | **No — baked** | `ios/Runner/Runner.Entitlements` (`webcredentials:` / `applinks:`) and `android/app/src/main/AndroidManifest.xml` (`android:host`), set per flavor at build |

### Using the published app against your backend

Works, with caveats:

- ✅ Enrollment (the QR/deeplink supplies your `base_url`) and **live approvals over the socket** —
  set your socket URL once via **About → Update Socket Url**.
- ❌ **Background / app-killed notifications** — your backend pushes through *your* FCM project
  (the realm `firebase-json`), but the published app only receives from ezto's FCM project. With the
  app backgrounded or closed it won't wake; the user must open it to see pending requests.
- ❌ **Passkeys and universal/app links** — the published app trusts only ezto's domain, not yours.

### Recommended: build your own flavor

For full functionality on your own domain — background push **and** passkeys — build your own
flavor with your own app identity:

1. Create a flavor with your bundle IDs, branding, and domain — see [**Branding & Flavors**](#branding--flavors).
2. Supply your own `google-services.json` / `GoogleService-Info.plist` (your FCM project), keystore,
   deeplink domains, and signing — see [**Required Files (Not in Repository)**](#required-files-not-in-repository).
3. **After building and signing the app**, update the root-realm attributes on the IdP so they match
   the built app — these values only exist once it is built/signed, so seed them **post-build**:
   - `fido-asset-links-android` — needs the **release signing cert SHA-256** of the built APK/AAB.
   - `fido-asset-links-ios` — needs the final **team ID + bundle ID**.
   - `firebase-json` — the FCM service account for the **same** Firebase project as the app's
     `google-services.json` / `GoogleService-Info.plist`.

   Seed via the platform README's *Seeding push & FIDO config* section (or
   `scripts/seed-realm-attributes.sh`).

> Order matters: **build & sign first, then seed.** Passkeys fail until the realm's
> `fido-asset-links-*` match the signed app's identity, and push fails unless the realm
> `firebase-json` and the app's `google-services.json` are the **same** Firebase project.

---

## Prerequisites

- [**fvm**](https://fvm.app/) (Flutter Version Management) — pins the Flutter SDK version per project.
  Install: `brew tap leoafarias/fvm && brew install fvm` (or `dart pub global activate fvm`).
- Flutter SDK — install/select via fvm from the project root: `fvm install && fvm use`
  (prefix commands with `fvm`, e.g. `fvm flutter pub get`).

> Commands below are shown as bare `flutter` / `dart run`; with fvm, prefix them (`fvm flutter …`).

---

## Setup

```bash
flutter pub get
flutter pub global activate intl_utils
flutter pub global run intl_utils:generate
```

---

## Running the App

The active flavor is baked in by the branding generator, so generate a flavor **before** running. Pick a flavor from `branding/lib/branding/config/flavor/` (e.g. `ezto_dev`, `ezto_prod`).

```bash
# 1. Generate the flavor config (run once per flavor switch)
cd branding
dart run bin/branding.dart --generate --flavor=ezto_dev
cd ..

# 2. Run on a connected device or emulator
flutter run                       # debug build
flutter run --release             # release build (required for FIDO/Passkey — see Device Support)
```

List connected devices with `flutter devices`. Target a specific one with `flutter run -d <device_id>`.

> Note: Before running on Android you must set the signing environment variables for the flavor (see [Building → Android](#android)). FIDO / Passkey features require a **release** build on Android 13+.

---

## Branding & Flavors

All flavor commands are run from the `branding/` directory using `dart run bin/branding.dart`.


> Note: The branding script also supports an interactive menu:
> ```bash
> cd branding
> dart run
> ```
> ```
> *****Please choose an option******
> 1. List Flavors
> 2. Build Flavor
> 3. Show Flavor details
> 4. Create a new Flavor
> 5. Release App
> Enter option number:
> ```

**List available flavors**
```bash
cd branding
dart run bin/branding.dart --list
```

**Generate flavor** (copies keystore, google-services.json, and config files into the project)
```bash
cd branding
dart run bin/branding.dart --generate --flavor={flavor_name}
```

**Show flavor details**
```bash
cd branding
dart run bin/branding.dart --details --flavor={flavor_name}
```

**Create a new flavor from a requirements JSON**
```bash
cd branding
dart run bin/branding.dart --create --path={json_path}
```

 
The requirements JSON must be saved as a `.json` file. When using the interactive menu (option 4), you will be prompted to enter the file path:
 
```
Enter option number: 4
Requirement Json Path
./<Path>.json
``` 
The requirements JSON file must follow this structure:
```json
{
  "app_name": "Your App Name",
  "flavor_name": "your_flavor_name",
  "android_app_bundle_id": "com.your.app.id",
  "ios_app_bundle_id": "com.your.app.id",
  "macos_app_bundle_id": "com.your.app.id",
  "windows_app_bundle_id": "com.your.app.id",
  "support_platforms": [
    "android",
    "ios"
  ],
  "primary_color": "#000000",
  "font_theme": "Inter",
  "terms_and_condition": "https://your-domain.com/terms",
  "privacy_policy": "https://your-domain.com/privacy",
  "android_firebase_json": "base64_encoded_google-services.json",
  "android_keystore_file": "base64_encoded_keystore_file",
  "android_key_alias": "your-key-alias",
  "android_key_password": "your-key-password",
  "android_store_password": "your-store-password",
  "app_transparent_logo": "base64_encoded_transparent_png",
  "app_full_logo": "base64_encoded_full_logo_svg",
  "app_logo_simple": "base64_encoded_simple_logo_svg",
  "app_logo_white": "base64_encoded_white_logo_svg"
}
```
**Release app** (runs flutter clean, pub get, and builds the release artifact)
```bash
cd branding
dart run bin/branding.dart --build --platform={platform_name} --flavor={flavor_name}
```
Platform options: `android`, `ios`, `macos`, `windows`
---

## Building

### Android

Before building, set the signing environment variables for your flavor. For example, for `ezto_prod`:

```bash
export EZTO_PROD_KEY_ALIAS="your-key-alias"
export EZTO_PROD_KEY_PASSWORD="your-key-password"
export EZTO_PROD_STORE_PASSWORD="your-store-password"
```

To persist these across sessions, add them to `~/.zshrc` or store them in a `.env` file and source it before building.

Generate the flavor config first, then build:

```bash
cd branding 
dart run bin/branding.dart --generate --flavor={flavor_name} && cd ..
or
dart run # with Menu choose option no. 2 to enter flavour name and press enter
&& cd ..
flutter build apk       # APK
flutter build appbundle # AAB (for Play Store)
```

Output locations:
- APK: `build/app/outputs/flutter-apk/app-release.apk`
- AAB: `build/app/outputs/bundle/release/app-release.aab`

### iOS

1. Generate an Apple Distribution certificate
2. Generate a provisioning profile
3. Install both on Mac
4. Find the provisioning profile UUID:
   ```bash
   security cms -D -i '/path/to/profile.mobileprovision'
   ```
5. Update `branding/lib/branding/config/flavor/{flavor_name}/app_signing.dart` with the UUID and team ID
6. Build:
   ```bash
   flutter build ipa --release --verbose \
     --export-options-plist='/path/to/ExportOptions.plist'
   ```

---

## Required Files (Not in Repository)

These files are excluded from the repository and must be added manually before building.

### Keystore

Place the keystore file for each flavor here:

```
branding/lib/branding/config/flavor/ezto_dev/keystore/android_keystore.keystore
branding/lib/branding/config/flavor/ezto_prod/keystore/android_keystore.keystore
```

### Push / Firebase Config

Place the Firebase config files for each flavor here:

```
branding/lib/branding/config/flavor/ezto_dev/push/google-services.json
branding/lib/branding/config/flavor/ezto_dev/push/GoogleService-Info.plist

branding/lib/branding/config/flavor/ezto_prod/push/google-services.json
branding/lib/branding/config/flavor/ezto_prod/push/GoogleService-Info.plist
```

After placing the files, run `--generate --flavor={flavor_name}` to copy them into the correct build locations.

### Encryption Keys

Each flavor requires encryption keys set in its `app_info.dart`. Open the file for your flavor and fill in the values:

```
branding/lib/branding/config/flavor/ezto_dev/app_info.dart
branding/lib/branding/config/flavor/ezto_prod/app_info.dart
```

```dart
@override
EncryptionModel get encryption => EncryptionModel(
      mobileKey: "{MOBILE_KEY}",
      desktopKey: "{DESKTOP_KEY}",
    );
```

Replace `{MOBILE_KEY}` and `{DESKTOP_KEY}` with the actual keys for your environment. These are not stored in the repository and must be obtained separately.

---
### Deeplink Domains
 
Each flavor's `deeplinks.dart` and the `base_deeplink.dart` use placeholder domains. Replace `{your.domain.com}` with your actual domain before building:
 
```
branding/lib/branding/config/flavor/ezto_dev/deeplinks.dart
branding/lib/branding/config/flavor/ezto_prod/deeplinks.dart
branding/lib/branding/config/base/base_deeplink.dart
```
 
```dart
class Deeplinks extends BaseDeeplink {
  @override
  List<String> get deepLinkDomains => [
    "{your.domain.com}"   // replace with your actual domain
  ];
}
```
 
Also update the iOS provisioning profile and team ID in `app_signing.dart` for each flavor:
 
```
branding/lib/branding/config/flavor/ezto_dev/app_signing.dart
branding/lib/branding/config/flavor/ezto_prod/app_signing.dart
```
 
```dart
class AppSigning implements BaseAppSigning {
  @override
  String get iosProvisioningProfileId => "{iosProvisioningProfileId}"; // replace
 
  @override
  String get appTeamId => "{appTeamId}"; // replace
 
  @override
  String get macOsProvisioningProfileName => "your macos profile name"; // replace
}
```
 
### App Update URLs
 
Each flavor's `app_info.dart` contains app update check URLs with internal UUIDs. Replace the placeholders with your own app update endpoint URLs:
 
```
branding/lib/branding/config/flavor/ezto_dev/app_info.dart
branding/lib/branding/config/flavor/ezto_prod/app_info.dart
```
 
```dart
'{app_update_url_mac}'  // replace with your Mac app update URL
'{app_update_url_window}'      // replace with your Window app update URL
```
 
### Socket URL
 
The socket URL in `lib/main.dart` is used for real-time communication. Replace the placeholder with your own socket server URL:
 
```dart
// lib/main.dart
return "{your_socket_url}";
```
 
### Generated Files
 
`lib/generated/` contains auto-generated flavor files. These are regenerated when you run:
 
```bash
flutter pub global run intl_utils:generate
```
 
And flavor constants are regenerated when you run `--generate --flavor={flavor_name}`. Do not edit these files manually.
 
---

## Android Signing

The signing config reads from environment variables set per flavor. Variable names follow the pattern `{FLAVOR_NAME_UPPERCASE}_KEY_ALIAS`, etc.

To inspect a keystore's alias:

```bash
/Applications/Android\ Studio.app/Contents/jbr/Contents/Home/bin/keytool \
  -list -v -keystore android/app/{keystore-name}.keystore
```

To verify the fingerprint of a built APK:

```bash
/Applications/Android\ Studio.app/Contents/jbr/Contents/Home/bin/keytool \
  -printcert -jarfile build/app/outputs/flutter-apk/app-release.apk
```

---

## Device Support

| Feature | Minimum Android Version |
|---------|------------------------|
| General app usage | Android 6.0 (API 23) |
| FIDO / Passkey registration & verification | Android 13 (API 33) |

FIDO passkey support via Android Credential Manager requires Android 13 or higher. Devices running Android 12 or below will receive an unsupported error when attempting FIDO registration or verification. The app handles this gracefully — users on unsupported devices should use an alternative authentication method.

### FIDO requires a Release build

FIDO / Passkey registration and verification **will not work on debug builds**. This is because Android's Credential Manager validates the app's identity against the server's `/.well-known/assetlinks.json` file using the APK's SHA256 certificate fingerprint. Debug builds are signed with a different debug keystore whose fingerprint is not registered in `assetlinks.json`, causing the error:

```
[50152] RP ID cannot be validated.
```
