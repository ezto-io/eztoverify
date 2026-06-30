import 'package:ezto_biometric/ezto_biometric.dart';
import 'package:eztoverify/enums/status.dart';
import 'package:eztoverify/interface/js_handler.dart';
import 'package:eztoverify/model/register_biometric_model.dart';
import 'package:eztoverify/model/registered_biometric_model.dart';
import 'package:eztoverify/model/verify_biometric_model.dart';
import 'package:eztoverify/service/encryption_service.dart';
import 'package:eztoverify/service/service_locator.dart';
import 'package:eztoverify/service/storage_service.dart';

class BiometricHandler extends JsHandler {
  Future<Map<String, dynamic>> checkBiometric(args) async {
    var result = await sl<StorageService>().readBioPublicKey();

    if (result != null) {
      return RegisteredBiometricModel(
        registered: true,
        credentialIds: [result.credId],
        device: await getDeviceInfo(),
        errorCodes: [],
      ).toMap();
    } else {
      return RegisteredBiometricModel(
        registered: false,
        credentialIds: [],
        device: await getDeviceInfo(),
        errorCodes: [
          BiometricStatus.bioNoCredIds.name,
        ],
      ).toMap();
    }
  }

  Future<Map<String, dynamic>> registerBiometric(List<dynamic> args) async {
    final data = processArgs(args);

    final missingRequiredFields = validateRequiredFields(data, [
      "publicKey",
      "credentialId",
    ]);

    if (missingRequiredFields.isNotEmpty) {
      return RegisterBiometricModel(
        success: false,
        message: "Registration Failed. Missing required fields",
        device: await getDeviceInfo(),
        errorCodes: [
          Status.jsMissingFields.name,
        ],
      ).toMap();
    }

    final String publicKey = data["publicKey"];
    final String credentialId = data["credentialId"];
    final result = await sl<BiometricService>().register(
      publicKey,
      credentialId,
    );
    if (result == BiometricStatus.success) {
      await sl<StorageService>().writeBioPublicKey(publicKey, credentialId);
    }
    return RegisterBiometricModel(
      success: result == BiometricStatus.success,
      message: (result == BiometricStatus.success)
          ? "Registration Success"
          : result == BiometricStatus.bioNotAvailable
              ? "No biometrics found in the device"
              : "Registration Failed.",
      device: await getDeviceInfo(),
      errorCodes: [
        if (result != BiometricStatus.success) result.name,
      ],
    ).toMap();
  }

  Future<Map<String, dynamic>> verifyBiometric(List<dynamic> args) async {
    final data = processArgs(args);

    final missingFields = validateRequiredFields(data, ["challenge"]);
    if (missingFields.isNotEmpty) {
      return VerifyBiometricModel(
        success: false,
        message: "Verification failed",
        device: await getDeviceInfo(),
        errorCodes: [
          Status.jsMissingFields.name,
        ],
      ).toMap();
    }

    final String challenge = data["challenge"];
    final result = await sl<BiometricService>().verify();
    if (result != BiometricStatus.success) {
      return VerifyBiometricModel(
        success: false,
        message: result == BiometricStatus.bioNotAvailable
            ? "No biometrics found in the device"
            : "Verification Failed.",
        device: await getDeviceInfo(),
        errorCodes: [
          BiometricStatus.bioPublicKeyNotFound.name,
        ],
      ).toMap();
    }

    final publicKey = await sl<StorageService>().readBioPublicKey();

    final encryptedChallenge =
        sl<EncryptionService>().encryptWithRsa(publicKey!.key, challenge);

    return VerifyBiometricModel(
      success: true,
      message: "Verification success",
      device: await getDeviceInfo(),
      errorCodes: [],
      encryptedChallenge: encryptedChallenge,
      credentialId: publicKey.credId,
    ).toMap();
  }
}
