import 'dart:io';

import 'package:eztoverify_internal/service/push_notification_service.dart';
import 'package:eztoverify_internal/service/push_service_interface.dart';
import 'package:eztoverify_internal/service/push_socket_service.dart';
import 'package:eztoverify_internal/service/web_service.dart';
import 'package:flutter/foundation.dart';

class PushFactory {
  static Future<PushServiceInterface> getInstance() async {
    if (kIsWeb) {
      return WebService();
    }
    if (Platform.isAndroid) {
      return PushNotificationService();
    } else if (Platform.isIOS) {
      return PushNotificationService();
    } else {
      return PushSocketService();
    }
  }
}
