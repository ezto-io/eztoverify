import 'dart:io';
import 'package:eztoverify_getting_started/constants/app_constants.dart';
import 'package:eztoverify_getting_started/core/locator/locator.dart';
import 'package:eztoverify_getting_started/core/services/storage_service/shared_pref_service.dart';
import 'package:flutter/material.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

class StorageService {
  final _storage = const FlutterSecureStorage();

  Future<String> getEncryptKey() async {
    String encKey =
        await _storage.read(key: AppConstant.encryptionKey) ?? 'No data found!';
    debugPrint('Data read from secure storage: $encKey');
    return encKey;
  }

  Future<void> setEncKey(String value) async {
    if (Platform.isMacOS) {
      await _storage.delete(key: AppConstant.encryptionKey);
    }
    await _storage.write(
      key: AppConstant.encryptionKey,
      value: value,
    );
  }

  Future<String?> getSocketUrl() async {
    String? socketUrl =
        getIt<SharedPreferencesService>().getString(AppConstant.socketUrl);
    return socketUrl;
  }

  Future<void> setSocketUrl(String value) async {
    await getIt<SharedPreferencesService>()
        .setString(AppConstant.socketUrl, value);
  }
}
