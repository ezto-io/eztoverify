import 'dart:convert';

import 'package:eztoverify/model/get_token_model.dart';
import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import 'package:eztoverify_internal/firebase_options.dart';
import 'package:firebase_core/firebase_core.dart';
import 'package:flutter/material.dart';
import 'package:eztoverify/controller/ezto_verify.dart';
import 'package:shared_preferences/shared_preferences.dart';

@pragma('vm:entry-point')
void desktopNotificationResponse(NotificationResponse response) async {
  final SharedPreferences prefs = await SharedPreferences.getInstance();
  final key = prefs.getString("push_encryption_key");
  debugPrint("response payload");
  debugPrint(response.payload);
  EztoVerify(
    encryptionKey: key,
  ).onPushReceived(
    data: jsonDecode(response.payload ?? "{}"),
  );
}

@pragma('vm:entry-point')
void mobileNotificationResponse(NotificationResponse response) async {
  await Firebase.initializeApp(
    options: DefaultFirebaseOptions.currentPlatform,
  );
}

abstract class PushServiceInterface {
  Future<void> init();

  void checkNotificationDataOnLaunch();

  void showNotification({
    required int id,
    required Map<String, dynamic> payload,
    required String? title,
    required String? body,
  });

  void showTransactionFromDeeplink(Map<String, dynamic> data);

  Future<GetTokenModel> getToken();
}
