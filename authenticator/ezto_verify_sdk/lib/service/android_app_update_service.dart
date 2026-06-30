import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:in_app_update/in_app_update.dart';

class AndroidAppUpdateService {
  Future<AppUpdateResult> checkForUpdates(bool isForceUpdate) async {
    if (Platform.isAndroid && isForceUpdate) {
      try {
        final info = await InAppUpdate.checkForUpdate();
        debugPrint("update info -> $info");
        if (info.updateAvailability == UpdateAvailability.updateAvailable) {
          final result = await InAppUpdate.performImmediateUpdate();
          if (result == AppUpdateResult.userDeniedUpdate ||
              result == AppUpdateResult.inAppUpdateFailed) {
            SystemNavigator.pop();
          }
          return result;
        }
      } catch (e) {
        debugPrint("InAppUpdate exception: $e");
      }
    }
    return AppUpdateResult.inAppUpdateFailed;
  }
}
