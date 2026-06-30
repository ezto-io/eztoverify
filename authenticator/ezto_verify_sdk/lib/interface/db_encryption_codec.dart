import 'dart:convert';

import 'package:eztoverify/service/encryption_service.dart';
import 'package:eztoverify/service/service_locator.dart';

class DbCodec extends Codec<Map<String, dynamic>, String> {
  final String password;
  const DbCodec({required this.password});

  @override
  Converter<String, Map<String, dynamic>> get decoder =>
      DbEncryptDecoder(password: password);

  @override
  Converter<Map<String, dynamic>, String> get encoder =>
      DbEncryptEncoder(password: password);
}

class DbEncryptEncoder extends Converter<Map<String, dynamic>, String> {
  final String password;
  const DbEncryptEncoder({required this.password});
  @override
  String convert(Map<String, dynamic> input) {
    final json = jsonEncode(input);
    final encrypted = sl<EncryptionService>()
        .aesCbcEncrypt(utf8.encode(password), utf8.encode(json));
    return base64Encode(encrypted);
  }
}

class DbEncryptDecoder extends Converter<String, Map<String, dynamic>> {
  final String password;
  const DbEncryptDecoder({required this.password});

  @override
  Map<String, dynamic> convert(String input) {
    final decoded = base64Decode(input);
    final result =
        sl<EncryptionService>().aesCbcDecrypt(utf8.encode(password), decoded);

    return jsonDecode(utf8.decode(result));
  }
}
