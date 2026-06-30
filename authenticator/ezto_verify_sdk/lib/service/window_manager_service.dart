import 'dart:io';

import 'package:flutter/material.dart';
import 'package:window_manager/window_manager.dart';

class WindowManagerService {
  Future<void> init() async {
    if (Platform.isMacOS || Platform.isWindows || Platform.isLinux) {
      await windowManager.ensureInitialized();
    }
  }

  Future bringToFront() async {
    if (Platform.isWindows || Platform.isMacOS || Platform.isLinux) {
      await windowManager.show();
      await windowManager.focus();
    }
  }

  Future<Size> getSize() async {
    return await windowManager.getSize();
  }

  Future<double> getPixelRatio() async {
    return windowManager.getDevicePixelRatio();
  }

  Future<Offset> getPosition() async {
    return windowManager.getPosition();
  }
}
