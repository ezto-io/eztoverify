import 'dart:convert';
import 'dart:io';
import 'package:eztoverify/controller/ezto_verify.dart';
import 'package:eztoverify/model/get_token_model.dart';
import 'package:eztoverify/service/service_locator.dart';
import 'package:eztoverify/service/socket_push_service.dart';
import 'package:eztoverify_internal/service/push_service_interface.dart';
import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:windows_notification/notification_message.dart';
import 'package:windows_notification/windows_notification.dart';

class PushSocketService implements PushServiceInterface {
  static FlutterLocalNotificationsPlugin flutterLocalNotificationsPlugin =
      FlutterLocalNotificationsPlugin();
  final _winNotifyPlugin =
      WindowsNotification(applicationId: "io.ezto.verify.flutter.internal");

  @override
  Future<void> init() async {}

  @override
  void checkNotificationDataOnLaunch() {}

  @override
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

  @override
  void showTransactionFromDeeplink(Map<String, dynamic> data) async {
    final SharedPreferences prefs = await SharedPreferences.getInstance();
    final key = prefs.getString("push_encryption_key");
    EztoVerify(
      encryptionKey: key,
    ).onPushReceived(
      data: data,
    );
  }

  @override
  Future<GetTokenModel> getToken() {
    return sl<SocketPushService>().getPushToken();
  }

  String base64Convert(Map<String, dynamic> data) {
    String jsonString = jsonEncode(data);
    String result = base64Encode(utf8.encode(jsonString));
    return result;
  }
}
