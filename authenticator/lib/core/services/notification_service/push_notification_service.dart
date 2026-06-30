import 'dart:convert';
import 'dart:io';
import 'package:eztoverify/controller/ezto_verify.dart';
import 'package:eztoverify_getting_started/core/services/encryption_service/enc_key_service.dart';
import 'package:firebase_core/firebase_core.dart';
import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:flutter/material.dart';
import 'package:flutter_local_notifications/flutter_local_notifications.dart';

@pragma('vm:entry-point')
Future<void> notificationResponse(NotificationResponse response) async {
  await Firebase.initializeApp();
}

@pragma('vm:entry-point')
Future<void> _onBackgroundMessage(RemoteMessage response) async {
  await Firebase.initializeApp();
}

class PushNotificationService {
  PushNotificationService._();
  static PushNotificationService? _instance;

  static PushNotificationService get I {
    return _instance ??= PushNotificationService._();
  }

  Future init() async {
    localNotification();
    PushNotificationService.I.getToken();
  }

  AndroidNotificationChannel? channel;
  static final FlutterLocalNotificationsPlugin
      _flutterLocalNotificationsPlugin = FlutterLocalNotificationsPlugin();
  final FirebaseMessaging _firebaseMessaging = FirebaseMessaging.instance;

  Future<void> requestPermission() async =>
      await _firebaseMessaging.requestPermission();

  void backgroundNotification() async =>
      FirebaseMessaging.onBackgroundMessage(_onBackgroundMessage);

  Future<void> localNotification() async {
    await FirebaseMessaging.instance
        .setForegroundNotificationPresentationOptions(
      alert: true,
      badge: true,
      sound: true,
    );

    showFirebaseNotification();
  }

  void handleNotification(RemoteMessage? message) async {
    var encryptionKey = await EncKeyService.getEncKey();
    EztoVerify(encryptionKey: encryptionKey).onPushReceived(
      data: message!.data,
    );
  }

  Future<void> showFirebaseNotification() async {
    FirebaseMessaging.onMessage.listen((message) async {
      _flutterLocalNotificationsPlugin.initialize(
        const InitializationSettings(
            android: AndroidInitializationSettings('@mipmap/launcher_icon'),
            iOS: DarwinInitializationSettings()),
        onDidReceiveNotificationResponse: notificationResponse,
        onDidReceiveBackgroundNotificationResponse: notificationResponse,
      );
      RemoteNotification? notification = message.notification;
      if (notification != null) {
        String action = jsonEncode(message.data);

        if (Platform.isAndroid) {
          _flutterLocalNotificationsPlugin.show(
              notification.hashCode,
              notification.title,
              notification.body,
              const NotificationDetails(
                iOS: DarwinNotificationDetails(
                    presentAlert: true,
                    presentBanner: true,
                    presentSound: true),
                android: AndroidNotificationDetails(
                    'flutter_notification', 'flutter_notification_title',
                    priority: Priority.high,
                    importance: Importance.max,
                    setAsGroupSummary: true,
                    styleInformation: DefaultStyleInformation(true, true),
                    icon: '@mipmap/launcher_icon'),
              ),
              payload: action);
        }
        handleNotification(message);
      }
    });

    FirebaseMessaging.onMessageOpenedApp.listen((message) async {
      handleNotification(message);
    });

    final message = await FirebaseMessaging.instance.getInitialMessage();
    if (message != null) {
      handleNotification(message);
    }
  }

  Future<String?> getToken() async {
    String? token = await _firebaseMessaging.getToken();
    debugPrint('Token $token');
    return token;
  }
}
