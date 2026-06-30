// registry_stub.dart
// A stub that does not rely on dart:ffi.
class Registry {
  Future<void> registerForWindows(String scheme) async {
    throw UnsupportedError(
        'Registry functionality is not supported on this platform.');
  }
}
