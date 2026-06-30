import 'dart:async';
import 'dart:convert';

import 'package:eztoverify/enums/close_reason.dart';
import 'package:eztoverify/service/ezto_result_service.dart';
import 'package:eztoverify/service/navigation_service.dart';
import 'package:flutter/material.dart';
import 'package:eztoverify/model/payload_data_model.dart';
import 'package:eztoverify/service/api_service.dart';
import 'package:eztoverify/service/service_locator.dart';
import 'package:socket_io_client/socket_io_client.dart';
import 'package:socket_io_client/socket_io_client.dart' as io;

class WebSocketService {
  io.Socket? socket;
  final bool closeImmediatelyOnResult;

  WebSocketService({
    required this.closeImmediatelyOnResult,
  });

  Future<bool> tryConnect(
    DataPayload dataPayload,
  ) async {
    final completer = Completer<bool>();
    if (socket != null) {
      dispose();
    }
    final url = dataPayload.payload.data.socket.url.trim();
    final requestId = dataPayload.payload.data.socket.requestId.trim();

    debugPrint(url);

    if (requestId.isNotEmpty && url.isNotEmpty) {
      debugPrint("opening socket connection");
      final data = OptionBuilder()
          .setTransports(['websocket'])
          .setAuth({"chatcode": requestId})
          .setExtraHeaders({"chatcode": requestId})
          .enableForceNew()
          .enableForceNewConnection()
          .enableReconnection()
          .setReconnectionAttempts(100)
          .disableAutoConnect()
          .build();
      socket = io.io(
        url,
        data,
      );

      socket?.on("error", (data) {
        debugPrint("error $data");
        if (!completer.isCompleted) {
          completer.complete(false);
        }
        dispose();
      });
      socket?.on("connect_error", (data) {
        debugPrint("connect_error $data");
        if (!completer.isCompleted) {
          dispose();
          completer.complete(false);
        }
      });

      socket?.on(
        requestId,
        (data) {
          final result = jsonEncode(data);
          final Map<String, dynamic> json = jsonDecode(result);
          if (json.containsKey("status")) {
            if (json["status"] ==
                ConnectionStatus.connectionReset.value.toString()) {
              dispose();
              sl<NavigationService>().popAll();
              sl<EztoResultService>().onClose(CloseReason.byUser);
            }
          }
          if (json.containsKey("auth_req_id")) {
            dispose();
            sl<ApiService>().sendConnectionStatus(
              status: ConnectionStatus.completed,
              socketUrl: url,
              requestId: requestId,
            );
            sl<EztoResultService>().onResult(
              dataPayload.payload.data.socket.requestId.trim(),
              json["auth_req_id"],
            );
            if (closeImmediatelyOnResult) {
              sl<NavigationService>().popAll();
              sl<EztoResultService>().onClose(CloseReason.transactionCompleted);
            }
          }
        },
      );
      socket?.onConnect((data) {
        debugPrint("connect called");
        if (!completer.isCompleted) {
          completer.complete(true);
        }
      });
      socket?.onDisconnect((data) {
        debugPrint("disconnect called");
        if (!completer.isCompleted) {
          dispose();
          completer.complete(false);
        }
      });
      socket?.connect();
    } else {
      //No hook available, return true
      if (!completer.isCompleted) {
        completer.complete(true);
      }
    }
    return completer.future;
  }

  void dispose() {
    debugPrint("Disposing socket");
    socket?.dispose();
    socket = null;
  }
}
