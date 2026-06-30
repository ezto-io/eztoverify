import 'dart:io';

import 'package:eztoverify/interface/js_handler.dart';
import 'package:eztoverify/interface/push_token_interface.dart';
import 'package:eztoverify/service/service_locator.dart';
import 'package:eztoverify/service/socket_push_service.dart';

class PushTokenHandler extends JsHandler {
  final PushTokenInterface pushTokenInterface;

  PushTokenHandler({
    required this.pushTokenInterface,
  });
  Future<Map<String, dynamic>> getToken(List<dynamic> args) async {
    //For mobile platform, request push token from user device
    //For Desktop platform, generate push token for socket service
    final push = (Platform.isAndroid || Platform.isIOS)
        ? await pushTokenInterface.getToken()
        : await sl<SocketPushService>().getPushToken();
        
    final response = {
      "pushToken": push.pushToken,
      "pushType": push.pushType.name,
      "device": await getDeviceInfo(),
    };
    return response;
  }
}
