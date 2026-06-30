import 'dart:async';
import 'dart:io';

import 'package:eztoverify/service/service_locator.dart';
import 'package:eztoverify/service/window_manager_service.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:geolocator/geolocator.dart';
import 'package:permission_handler/permission_handler.dart';

final GlobalKey<NavigatorState> eztoNavigatorKey = GlobalKey<NavigatorState>();

class NavigationService {
  bool popAll() {
    var canPop = false;
    if (eztoNavigatorKey.currentContext != null) {
      canPop = Navigator.maybeOf(eztoNavigatorKey.currentContext!)?.canPop() ??
          false;
      Navigator.maybeOf(eztoNavigatorKey.currentContext!)
          ?.popUntil((route) => route.isFirst);
    }
    return canPop;
  }

  Future<bool> showPermissionDeniedAlert({
    required String title,
    required String content,
  }) async {
    if (eztoNavigatorKey.currentContext != null) {
      await sl<WindowManagerService>().bringToFront();
      bool shouldHandleLifecycleMessages = true;
      Completer<bool> completer = Completer<bool>();
      showDialog(
        context: eztoNavigatorKey.currentContext!,
        builder: (BuildContext context) {
          return CupertinoAlertDialog(
            title: Text(title),
            content: Text(content),
            actions: <Widget>[
              CupertinoDialogAction(
                child: const Text("Go to Settings"),
                onPressed: () async {
                  if (Platform.isMacOS || Platform.isWindows) {
                    final result = await Geolocator.openAppSettings();
                    if (result) {
                      SystemChannels.lifecycle.setMessageHandler((msg) async {
                        if (shouldHandleLifecycleMessages &&
                            msg == "AppLifecycleState.resumed") {
                          Navigator.pop(eztoNavigatorKey.currentContext!, true);
                          shouldHandleLifecycleMessages = false;
                        }
                        return msg;
                      });
                    } else {
                      Navigator.pop(eztoNavigatorKey.currentContext!, false);
                    }
                  } else if (Platform.isAndroid || Platform.isIOS) {
                    final result = await openAppSettings();
                    if (result) {
                      SystemChannels.lifecycle.setMessageHandler((msg) async {
                        if (shouldHandleLifecycleMessages &&
                            msg == "AppLifecycleState.resumed") {
                          Navigator.pop(eztoNavigatorKey.currentContext!, true);
                          shouldHandleLifecycleMessages = false;
                        }
                        return msg;
                      });
                    } else {
                      Navigator.pop(eztoNavigatorKey.currentContext!, false);
                    }
                  }
                },
              ),
              CupertinoDialogAction(
                child: const Text("Cancel"),
                onPressed: () {
                  Navigator.pop(eztoNavigatorKey.currentContext!, false);
                },
              ),
            ],
          );
        },
      ).then((value) {
        completer.complete(value ?? false);
      });
      return completer.future;
    } else {
      return false;
    }
  }
}
