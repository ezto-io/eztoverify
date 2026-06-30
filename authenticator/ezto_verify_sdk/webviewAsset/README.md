# WebView2 runtime asset (Windows desktop only)

The Windows build of `ezto_verify_sdk` installs the **Microsoft Edge WebView2
Runtime** by shelling out to a bundled installer:

```dart
// lib/bloc/web_view/web_view_cubit.dart
await Process.run('webviewAsset/WebView2Runtime.exe', ['/silent', '/install']);
```

The installer is **not vendored in this repo** — it is a ~170 MB Microsoft
redistributable that exceeds GitHub's file-size limit and does not belong in
source control. Fetch it yourself before building for Windows.

## Get the installer

Download the **Evergreen Standalone Installer** from Microsoft:

- https://go.microsoft.com/fwlink/p/?LinkId=2124703
- (landing page: https://developer.microsoft.com/microsoft-edge/webview2/)

Save it in this directory as `WebView2Runtime.exe`:

```bash
# from repo root
curl -L "https://go.microsoft.com/fwlink/p/?LinkId=2124703" \
  -o authenticator/ezto_verify_sdk/webviewAsset/WebView2Runtime.exe
```

Or run the helper: `scripts/fetch-webview2.sh`.

The file is git-ignored, so it will not be committed.

> Only needed for Windows desktop builds. Android/iOS builds and pub.dev
> publishing ignore this directory (see `.pubignore`).
