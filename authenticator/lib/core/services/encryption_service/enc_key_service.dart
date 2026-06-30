import 'dart:io';
import 'package:eztoverify_getting_started/core/services/storage_service/secure_storage_service.dart';
import 'package:eztoverify_getting_started/generated/flavor/generated_constants.g.dart';

class EncKeyService {
  static String encryptionKey = '';

  static Future<String> getEncKey() async {
    if (Platform.isAndroid || Platform.isIOS) {
      if (GeneratedConstants().isProd) {
        encryptionKey = GeneratedConstants().mobileEnc;
      } else {
        encryptionKey = await StorageService().getEncryptKey();
      }
    } else {
      if (GeneratedConstants().isProd) {
        encryptionKey = GeneratedConstants().desktopEnc;
      } else {
        encryptionKey = await StorageService().getEncryptKey();
      }
    }
    return encryptionKey;
  }

  static void setEncKey(String encKey) {
    StorageService().setEncKey(encKey);
  }
}
