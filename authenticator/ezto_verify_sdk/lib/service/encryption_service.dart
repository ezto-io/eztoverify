import 'dart:convert';
import 'dart:typed_data';

import 'package:eztoverify/handlers/device_info_handler.dart';
import 'package:eztoverify/service/service_locator.dart';
import 'package:pointycastle/export.dart';
import 'package:basic_utils/basic_utils.dart';
import 'package:uuid/uuid.dart';

class EncryptionService {
  Uint8List aesCbcEncrypt(Uint8List key, Uint8List paddedPlaintext) {
    assert([128, 192, 256].contains(key.length * 8));

    final cbc = CBCBlockCipher(AESEngine()); // true=encrypt
    final ParametersWithIV<KeyParameter> ivParams =
        ParametersWithIV(KeyParameter(key), Uint8List(16));
    final paddingParams =
        PaddedBlockCipherParameters<ParametersWithIV<KeyParameter>, Null>(
            ivParams, null);
    final PaddedBlockCipherImpl paddedCipher =
        PaddedBlockCipherImpl(PKCS7Padding(), cbc);
    paddedCipher.init(true, paddingParams);
    final result = paddedCipher.process(paddedPlaintext);
    return result;
  }

  Uint8List aesCbcDecrypt(Uint8List key, Uint8List cipherText) {
    assert([128, 192, 256].contains(key.length * 8));

    // final cbc = CBCBlockCipher(AESEngine())
    //   ..init(false,
    //       ParametersWithIV(KeyParameter(key), Uint8List(16))); // false=decrypt

    // return cbc.process(cipherText);

    final CBCBlockCipher cbcCipher = CBCBlockCipher(AESEngine());
    final ParametersWithIV<KeyParameter> ivParams =
        ParametersWithIV<KeyParameter>(KeyParameter(key), Uint8List(16));
    final PaddedBlockCipherParameters<ParametersWithIV<KeyParameter>, Null>
        paddingParams =
        PaddedBlockCipherParameters<ParametersWithIV<KeyParameter>, Null>(
            ivParams, null);
    final PaddedBlockCipherImpl paddedCipher =
        PaddedBlockCipherImpl(PKCS7Padding(), cbcCipher);
    paddedCipher.init(false, paddingParams);
    final decrypted = paddedCipher.process(cipherText);
    return decrypted;
  }

  String encryptWithRsa(String publicKey, String challenge) {
    try {
      final key = Uint8List.fromList(base64Decode(publicKey));
      final dataToEncrypt = Uint8List.fromList(base64Decode(challenge));
      final rsaPublicKey = CryptoUtils.rsaPublicKeyFromDERBytes(key);
      final encryptor = OAEPEncoding(RSAEngine())
        ..init(
          true,
          PublicKeyParameter<RSAPublicKey>(rsaPublicKey),
        );

      final result = base64Encode(encryptor.process(dataToEncrypt));
      return result;
    } catch (e) {
      return "Error encrypting data: $e";
    }
  }

  String decryptPushWithAes(String key, String encrytedData) {
    int paddingCount = 4 - (key.length % 4);

    if (paddingCount == 4) {
      paddingCount = 0;
    }

    final paddedKey = key.padRight(key.length + paddingCount, "=");

    final decodedKey = Uint8List.fromList(base64Decode(paddedKey));
    final decodedData = Uint8List.fromList(base64Decode(encrytedData));
    final ebcCipher = ECBBlockCipher(AESEngine());
    final PaddedBlockCipherImpl paddedCipher =
        PaddedBlockCipherImpl(PKCS7Padding(), ebcCipher);

    final cipher = paddedCipher
      ..init(
        false,
        PaddedBlockCipherParameters(KeyParameter(decodedKey), null),
      );

    final decrypted = cipher.process(decodedData);
    return String.fromCharCodes(decrypted);
  }

  String toSha256Hash(Uint8List dataToDigest) {
    final d = SHA256Digest();
    return base64Encode(d.process(dataToDigest));
  }

  String toSha512(Uint8List dataToDigest) {
    final d = SHA512Digest();
    return base64UrlEncode(d.process(dataToDigest));
  }

  Future<Uint8List> generateDbKey() async {
    String? key = await sl<DeviceInfoHandler>().getBindingId();
    if (key == null) {
      const uuid = Uuid();
      key = uuid.v5(Namespace.url.value, "ezto.io");
    }
    final sha = toSha256Hash(utf8.encode(key));
    return utf8.encode(sha).sublist(0, 16);
  }
}
