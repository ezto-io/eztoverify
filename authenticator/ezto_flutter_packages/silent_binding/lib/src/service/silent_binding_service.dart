import 'dart:convert';

import 'package:ezto_silent_binding/src/model/register_silent_binding_model.dart';
import 'package:ezto_silent_binding/src/model/verify_silent_binding_model.dart';
import 'package:ezto_silent_binding/src/service/binding_encryption_service.dart';

class SilentBindingService {
  final encryptionService = EncryptionService();

  Future<RegisterSilentBindingModel> registerSilentBinding({
    required String publicKey,
    required String? bindingId,
  }) async {
    if (bindingId == null) {
      final result = RegisterSilentBindingModel(
        success: false,
        message: "Unable to generate binding ID",
        encryptedDeviceId: "",
      );
      return result;
    }

    final hashedBindingId =
        encryptionService.toSha256Hash(utf8.encode(bindingId));
    final encryptedB64 = encryptionService.encryptWithRsa(
      publicKey,
      base64Encode(utf8.encode(hashedBindingId)),
    );

    final result = RegisterSilentBindingModel(
      success: true,
      message: "",
      encryptedDeviceId: encryptedB64,
    );
    return result;
  }

  Future<VerifySilentBindingModel> verifySilentBinding({
    required String challenge,
    required String? bindingId,
  }) async {
    if (bindingId == null) {
      final result = VerifySilentBindingModel(
        success: false,
        message: "Unable to generate binding ID",
        challengeHash: "",
        keyHashFirst6: "",
      );
      return result;
    }

    final hashedBindingId =
        encryptionService.toSha256Hash(utf8.encode(bindingId));

    final challengeHash = encryptionService.toSha256HashCombined(
      base64Decode(challenge),
      base64Decode(hashedBindingId),
    );

    final keyHashFirst6 =
        encryptionService.toSha256HashFirst6(utf8.encode(bindingId));

    final result = VerifySilentBindingModel(
      success: true,
      message: "",
      challengeHash: challengeHash,
      keyHashFirst6: keyHashFirst6,
    );
    return result;
  }
}
