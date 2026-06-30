import 'dart:convert';
import 'dart:io';

import 'package:eztoverify/interface/db_encryption_codec.dart';
import 'package:eztoverify/model/public_key_model.dart';
import 'package:eztoverify/service/encryption_service.dart';
import 'package:eztoverify/service/service_locator.dart';
import 'package:flutter/foundation.dart';
import 'package:path_provider/path_provider.dart';
import 'package:sembast/sembast_io.dart';
import 'package:uuid/uuid.dart';
import 'package:path/path.dart' as p;

class StorageService {
  DatabaseFactory dbFactory = databaseFactoryIo;
  late Database db;

  Future<void> init() async {
    const dbName = "ezto_verify.db";
    final oldPath = await getApplicationDocumentsDirectory();
    final oldDbFile = File(p.join(oldPath.path, dbName));
    final isMigrationNeeded = await oldDbFile.exists();

    final newDbPath = kIsWeb
        ? null
        : (Platform.isIOS | Platform.isMacOS)
            ? await getLibraryDirectory()
            : await getApplicationSupportDirectory();

    if (newDbPath != null) {
      if (isMigrationNeeded) {
        await oldDbFile.rename(File(p.join(newDbPath.path, dbName)).path);
      }

      final dpPath = p.join(newDbPath.path, dbName);

      try {
        var codec = SembastCodec(
          signature: 'encrypt',
          codec: DbCodec(
            password:
                utf8.decode(await sl<EncryptionService>().generateDbKey()),
          ),
        );
        db = await dbFactory.openDatabase(dpPath, codec: codec);
      } catch (e) {
        debugPrint(e.toString());
        await dbFactory.deleteDatabase(dpPath);
        await init();
      }
    }
  }

  Future<void> writeBioPublicKey(String publicKey, String credentialId) async {
    var store = StoreRef.main();
    await store.record("biometric").put(db, {
      "publicKey": publicKey,
      "credentialId": credentialId,
    });
  }

  Future<PublicKeyModel?> readBioPublicKey() async {
    var store = StoreRef.main();
    final result = await store.record("biometric").get(db);
    if (result == null) return null;
    final keyJson = jsonEncode(result);
    final keyData = jsonDecode(keyJson);
    final publicKey = keyData["publicKey"];
    final credId = keyData["credentialId"];

    if (publicKey == null || credId == null) return null;
    return PublicKeyModel(key: publicKey, credId: credId);
  }

  Future<String> createOrGetDeviceId() async {
    var store = StoreRef.main();
    String? deviceId = await store.record("device_id").get(db) as String?;

    if (deviceId == null) {
      var uuid = const Uuid();
      final id = uuid.v4().toString();
      await store.record("device_id").put(db, id);
      deviceId = id;
    }

    return deviceId;
  }

  Future<bool> isFirstRun() async {
    var store = StoreRef.main();
    final isFirstRun = await store.record("is_first_run").get(db) as bool?;
    if (isFirstRun == null || isFirstRun) {
      await store.record("is_first_run").put(db, false);
      return true;
    }
    return false;
  }
}
