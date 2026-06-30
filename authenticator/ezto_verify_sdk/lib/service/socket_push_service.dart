import 'dart:convert';
import 'dart:io';

import 'package:eztoverify/enums/push_support.dart';
import 'package:eztoverify/model/get_token_model.dart';
import 'package:eztoverify/service/encryption_service.dart';
import 'package:eztoverify/service/ezto_result_service.dart';
import 'package:eztoverify/service/service_locator.dart';
import 'package:eztoverify/service/storage_service.dart';
import 'package:flutter/foundation.dart';
import 'package:socket_io_client/socket_io_client.dart';

//Socket push service is required for desktop platforms
class SocketPushService {
  Future<GetTokenModel> getPushToken() async {
    final deviceId = await sl<StorageService>().createOrGetDeviceId();
    final pushToken = sl<EncryptionService>().toSha512(utf8.encode(deviceId));
    return GetTokenModel(
      pushToken: pushToken,
      pushType: PushSupport.socket,
    );
  }

  void registerSocketPush(socketUrl) async {
    if (Platform.isAndroid || Platform.isIOS) return;
    final push = await getPushToken();
    debugPrint("connecting to push socket with URL: $socketUrl");
    final Socket socket = io(
      socketUrl,
      OptionBuilder()
          .setTransports(['websocket'])
          .setAuth({"chatcode": push.pushToken})
          .setExtraHeaders({"chatcode": push.pushToken, "type": "socketPush"})
          .enableReconnection()
          .enableForceNew()
          .enableForceNewConnection()
          .setReconnectionAttempts(double.infinity)
          .setReconnectionDelay(10000)
          .setReconnectionDelayMax(30000)
          .disableAutoConnect()
          .build(),
    );

    socket.on(push.pushToken, (data) {
      debugPrint("on data $data");
      sl<EztoResultService>().onTransactionRequest(data, false);
    });
    socket.on("error", (data) {
      debugPrint("error $data");
    });
    socket.on("connect_error", (data) {
      debugPrint("connect_error $data");
    });
    socket.onConnect(
      (data) {
        debugPrint("onConnect $data");
      },
    );
    socket.onDisconnect(
      (data) {
        debugPrint("onDisconnect $data");
      },
    );
    socket.connect();
  }
}
