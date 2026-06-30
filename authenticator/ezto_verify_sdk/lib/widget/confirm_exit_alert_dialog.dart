import 'dart:async';

import 'package:eztoverify/enums/close_reason.dart';
import 'package:eztoverify/service/ezto_result_service.dart';
import 'package:eztoverify/service/navigation_service.dart';
import 'package:flutter/material.dart';
import 'package:eztoverify/model/payload_data_model.dart';
import 'package:eztoverify/service/api_service.dart';
import 'package:eztoverify/service/service_locator.dart';
import 'package:eztoverify/service/websocket_service.dart';

Future<bool> confirmExitAlertDialog(
  BuildContext context, {
  Socket? socketInfo,
}) async {
  final Completer<bool> completer = Completer();
  AlertDialog alert = AlertDialog(
    title: const Text("Cancel Transaction"),
    content: const Text("Do you wish to cancel current transaction?"),
    actions: [
      TextButton(
        child: const Text("OK"),
        onPressed: () {
          completer.complete(true);
          if (socketInfo != null) {
            sl<WebSocketService>().dispose();
            sl<ApiService>().sendConnectionStatus(
              status: ConnectionStatus.userClosed,
              socketUrl: socketInfo.url,
              requestId: socketInfo.requestId,
            );
          }
          sl<EztoResultService>().onClose(CloseReason.byUser);
          sl<NavigationService>().popAll();
        },
      ),
      TextButton(
        child: const Text("CANCEL"),
        onPressed: () {
          Navigator.pop(eztoNavigatorKey.currentContext!);
          completer.complete(false);
        },
      ),
    ],
  );

  showDialog(
    context: eztoNavigatorKey.currentContext!,
    useRootNavigator: false,
    builder: (BuildContext context) {
      return alert;
    },
  );
  return completer.future;
}
