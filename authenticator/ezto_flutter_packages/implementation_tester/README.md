# implementation_tester

A new Flutter project.

## Getting Started

This project is a starting point for a Flutter application.

A few resources to get you started if this is your first Flutter project:

- [Lab: Write your first Flutter app](https://docs.flutter.dev/get-started/codelab)
- [Cookbook: Useful Flutter samples](https://docs.flutter.dev/cookbook)

For help getting started with Flutter development, view the
[online documentation](https://docs.flutter.dev/), which offers tutorials,
samples, guidance on mobile development, and a full API reference.

## Dart → Rust Integration Tests

These tests validate the full call flow from **Dart → FFI → Rust core** and ensure
that the returned responses match the expected Rust logic.

### 1. Dart → Rust Logic Match

Validates that all NFC operations in Dart match the expected outputs from Rust.

```bash
flutter drive \
  --driver=test_driver/integration_test.dart \
  --target=integration_test/nfc_dart_match_rust_test.dart
```

### 2. Dart → FFI → Rust Parity Test

Checks that the FFI layer returns the exact same values as the Rust core.

```bash
flutter drive \
  --driver=test_driver/integration_test.dart \
  --target=integration_test/nfc_ffi_match_rust_test.dart
```

### 3. Full NFC Flow (Dart → FFI → Rust)

Runs the complete NFC flow using the same test vectors as Rust.

```bash
flutter drive \
  --driver=test_driver/integration_test.dart \
  --target=integration_test/nfc_integration_test.dart
```
