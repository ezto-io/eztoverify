import 'dart:convert';
import 'package:eztoverify/enums/push_support.dart';
import 'package:eztoverify/model/get_token_model.dart';
import 'package:eztoverify_internal/firebase_options.dart';
import 'package:eztoverify_internal/service/push_service_interface.dart';
import 'package:firebase_core/firebase_core.dart';
import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import 'package:eztoverify/controller/ezto_verify.dart';
import 'package:shared_preferences/shared_preferences.dart';

Future<void> _firebaseMessagingBackgroundHandler(RemoteMessage message) async {
  await Firebase.initializeApp(
    options: DefaultFirebaseOptions.currentPlatform,
  );
}

class PushNotificationService implements PushServiceInterface {
  AndroidNotificationChannel? channel;

  static FlutterLocalNotificationsPlugin flutterLocalNotificationsPlugin =
      FlutterLocalNotificationsPlugin();

  String? newMessageId = '';

  @override
  Future<void> init() async {
    await Firebase.initializeApp(
      options: DefaultFirebaseOptions.currentPlatform,
    );
    FirebaseMessaging.onBackgroundMessage(_firebaseMessagingBackgroundHandler);
    _requestPermission();
  }

  Future<NotificationSettings> _requestPermission() async {
    return FirebaseMessaging.instance.requestPermission();
  }

  @override
  void checkNotificationDataOnLaunch() async {
    final message = await FirebaseMessaging.instance.getInitialMessage();
    if (message != null) {
      showTransaction(message);
    }

    FirebaseMessaging.onMessage.listen((message) async {
      RemoteNotification? notification = message.notification;
      if (notification != null) {
        showNotification(
          id: message.hashCode,
          payload: message.data,
          title: message.notification?.title,
          body: message.notification?.body,
        );
        showTransaction(message);
      }
    });

    FirebaseMessaging.onMessageOpenedApp.listen((message) async {
      showTransaction(message);
    });
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

  void showTransaction(RemoteMessage message) async {
    final SharedPreferences prefs = await SharedPreferences.getInstance();
    final key = prefs.getString("push_encryption_key");
    if (message.messageId != newMessageId) {
      EztoVerify(
        encryptionKey: key,
      ).onPushReceived(
        data: message.data,
      );
    }
    newMessageId = message.messageId;
  }

  @override
  void showNotification(
      {required int id,
      required Map<String, dynamic> payload,
      required String? title,
      required String? body}) {
    flutterLocalNotificationsPlugin.initialize(
      const InitializationSettings(
        android: AndroidInitializationSettings(
          'notification',
        ),
        iOS: DarwinInitializationSettings(),
      ),
      onDidReceiveNotificationResponse: mobileNotificationResponse,
      onDidReceiveBackgroundNotificationResponse: mobileNotificationResponse,
    );

    String data = jsonEncode(payload);

    flutterLocalNotificationsPlugin.show(
      id,
      title,
      body,
      const NotificationDetails(
        iOS: DarwinNotificationDetails(
          presentAlert: true,
          presentBanner: true,
          presentSound: true,
        ),
        android: AndroidNotificationDetails(
          'flutter_notification',
          'flutter_notification_title',
          priority: Priority.high,
          importance: Importance.max,
          setAsGroupSummary: true,
          styleInformation: DefaultStyleInformation(true, true),
          channelShowBadge: true,
          autoCancel: true,
          icon: '@mipmap/launcher_icon',
        ),
      ),
      payload: data,
    );
  }

  @override
  Future<GetTokenModel> getToken() async {
    return GetTokenModel(
      pushToken: (await FirebaseMessaging.instance.getToken()) ?? "",
      pushType: PushSupport.firebase,
    );
  }
}
