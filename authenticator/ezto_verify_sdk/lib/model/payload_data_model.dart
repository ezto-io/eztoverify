import 'dart:convert';

import 'package:eztoverify/exceptions/encryption_key_exception.dart';
import 'package:eztoverify/service/encryption_service.dart';

class DataPayload {
  final Payload payload;

  DataPayload({
    required this.payload,
  });

  factory DataPayload.fromJson({
    required Map<String, dynamic> json,
    required String? encryptionKey,
  }) {
    return DataPayload(
      payload: Payload.fromMap(
        map: jsonDecode(json["payload"]),
        encryptionKey: encryptionKey,
      ),
    );
  }

  Map<String, dynamic> toJson() => {
        "payload": payload.toJson(),
      };
}

class Payload {
  final int version;
  final String id;
  final bool isEncrypted;
  final Data data;

  Payload({
    required this.version,
    required this.id,
    required this.isEncrypted,
    required this.data,
  });

  Map<String, dynamic> toMap() {
    return {
      'version': version,
      'id': id,
      'isEncrypted': isEncrypted,
      'data': data.toMap(),
    };
  }

  factory Payload.fromMap({
    required Map<String, dynamic> map,
    String? encryptionKey,
  }) {
    final isEncrypted = map['isEncrypted'] ?? false;
    return Payload(
      version: map['version']?.toInt() ?? 0,
      id: map['id'] ?? '',
      isEncrypted: map['isEncrypted'] ?? false,
      data: isEncrypted
          ? Data.fromEncrypted(
              encrypted: map['data'],
              encryptionKey: encryptionKey,
            )
          : Data.fromMap(map['data']),
    );
  }

  String toJson() => json.encode(toMap());
}

class Data {
  final String action;
  final String type;
  final Socket socket;

  Data({
    required this.action,
    required this.type,
    required this.socket,
  });

  factory Data.fromEncrypted({
    required String encrypted,
    String? encryptionKey,
  }) {
    if (encryptionKey == null) throw EncryptionKeyException();
    EncryptionService encryptionService = EncryptionService();

    try {
      final json =
          encryptionService.decryptPushWithAes(encryptionKey, encrypted);
      return Data.fromJson(json);
    } catch (e) {
      throw EncryptionKeyException();
    }
  }

  Map<String, dynamic> toMap() {
    return {
      'action': action,
      'type': type,
      'socket': socket.toMap(),
    };
  }

  factory Data.fromMap(Map<String, dynamic> map) {
    return Data(
      action: map['action'] ?? '',
      type: map['type'] ?? '',
      socket: Socket.fromMap(map['socket']),
    );
  }

  String toJson() => json.encode(toMap());

  factory Data.fromJson(String source) => Data.fromMap(json.decode(source));
}

class Socket {
  final String url;
  final String requestId;
  Socket({
    required this.url,
    required this.requestId,
  });

  Map<String, dynamic> toMap() {
    return {
      'url': url,
      'requestId': requestId,
    };
  }

  factory Socket.fromMap(Map<String, dynamic> map) {
    return Socket(
      url: map['url'] ?? '',
      requestId: map['requestId'] ?? '',
    );
  }

  String toJson() => json.encode(toMap());

  factory Socket.fromJson(String source) => Socket.fromMap(json.decode(source));
}
