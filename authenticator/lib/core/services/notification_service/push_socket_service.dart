import 'dart:convert';
import 'dart:io';
import 'package:eztoverify/controller/ezto_verify.dart';
import 'package:eztoverify_getting_started/core/services/encryption_service/enc_key_service.dart';
import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import 'package:windows_notification/notification_message.dart';
import 'package:windows_notification/windows_notification.dart';

@pragma('vm:entry-point')
void desktopNotificationResponse(NotificationResponse response) async {
  var encryptionKey = await EncKeyService.getEncKey();
  EztoVerify(
    encryptionKey: encryptionKey,
  ).onPushReceived(
    data: jsonDecode(response.payload ?? "{}"),
  );
}

class PushSocketService {
  static FlutterLocalNotificationsPlugin flutterLocalNotificationsPlugin =
      FlutterLocalNotificationsPlugin();
  final _winNotifyPlugin = WindowsNotification(applicationId: "ezto verify");

  Future<void> showNotification(
      {required int id,
      required Map<String, dynamic> payload,
      required String? title,
      required String? body}) async {
    if (Platform.isMacOS || Platform.isLinux) {
      flutterLocalNotificationsPlugin.initialize(
        const InitializationSettings(
          macOS: DarwinInitializationSettings(),
        ),
        onDidReceiveNotificationResponse: desktopNotificationResponse,
        onDidReceiveBackgroundNotificationResponse: desktopNotificationResponse,
      );

      String data = jsonEncode(payload);

      flutterLocalNotificationsPlugin.show(
        id,
        title,
        body,
        const NotificationDetails(
          macOS: DarwinNotificationDetails(
            presentAlert: true,
            presentBanner: true,
            presentSound: true,
          ),
        ),
        payload: data,
      );
    } else {
      NotificationMessage message = NotificationMessage.fromPluginTemplate(
        id.toString(),
        title ?? '',
        body ?? '',
        launch: "eztovrfy://sdk?data=${base64Convert(payload)}",
      );
      _winNotifyPlugin.showNotificationPluginTemplate(message);
    }
  }

  void showTransactionFromDeeplink(Map<String, dynamic> data) async {
    var encryptionKey = await EncKeyService.getEncKey();
    EztoVerify(
      encryptionKey: encryptionKey,
    ).onPushReceived(
      data: data,
    );
  }

  String base64Convert(Map<String, dynamic> data) {
    String jsonString = jsonEncode(data);
    String result = base64Encode(utf8.encode(jsonString));
    return result;
  }
}
