import 'dart:io';

import 'package:win32_registry/win32_registry.dart' as winReg;

// This implementation uses win32_registry and dart:ffi.
class Registry {
  Future<void> registerForWindows(String scheme) async {
    try {
      String appPath = Platform.resolvedExecutable;

      String protocolRegKey = 'Software\\Classes\\$scheme';
      winReg.RegistryValue protocolRegValue =
          const winReg.RegistryValue.string('URL Protocol', '');
      String protocolCmdRegKey = 'shell\\open\\command';
      winReg.RegistryValue protocolCmdRegValue = winReg.RegistryValue.string(
        '',
        '"$appPath" "%1"',
      );

      // Refer to the package's Registry using the alias.
      final regKey = winReg.Registry.currentUser.createKey(protocolRegKey);
      regKey.createValue(protocolRegValue);
      regKey.createKey(protocolCmdRegKey).createValue(protocolCmdRegValue);
    } catch (e) {
      print(e);
    }
  }
}
