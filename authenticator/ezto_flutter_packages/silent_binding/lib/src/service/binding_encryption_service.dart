import 'dart:convert';
import 'dart:typed_data';

import 'package:pointycastle/export.dart';
import 'package:basic_utils/basic_utils.dart';

class EncryptionService {
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

  String toSha256HashFirst6(Uint8List dataToDigest) {
    final d = SHA256Digest();
    return base64Encode(d.process(dataToDigest).take(6).toList());
  }

  String toSha256HashCombined(Uint8List dataToDigest, Uint8List salt) {
    final d = SHA256Digest();
    final combined = Uint8List.fromList([...dataToDigest, ...salt]);
    return base64Encode(d.process(combined));
  }
}
