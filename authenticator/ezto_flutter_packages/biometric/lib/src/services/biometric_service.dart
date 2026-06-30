import 'dart:io';

import 'package:ezto_biometric/src/enums/biometric_status.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter_local_authentication/flutter_local_authentication.dart';
import 'package:local_auth/local_auth.dart';

class BiometricService {
  final LocalAuthentication auth = LocalAuthentication();

  Future<BiometricStatus> register(
      String publicKey, String credentialId) async {
    if (Platform.isLinux) {
      final FlutterLocalAuthentication macAuth = FlutterLocalAuthentication();
      try {
        bool canAuthenticate = await macAuth.canAuthenticate();

        if (!canAuthenticate) return BiometricStatus.bioNotAvailable;

        bool isBioSuccess = await macAuth.authenticate();
        if (!isBioSuccess) return BiometricStatus.bioAuthFailed;
        //  ## await sl<StorageService>().writeBioPublicKey(publicKey, credentialId);
        return BiometricStatus.success;
      } catch (e) {
        debugPrint("Exception checking support. $e");
        return BiometricStatus.bioAuthFailed;
      }
    } else {
      final bool canAuthenticateWithBiometrics = await auth.canCheckBiometrics;
      final bool canAuthenticate =
          canAuthenticateWithBiometrics || await auth.isDeviceSupported();

      if (!canAuthenticate) return BiometricStatus.bioNotAvailable;

      // final List<BiometricType> availableBiometrics =
      //     await auth.getAvailableBiometrics();
      // if (availableBiometrics.isEmpty) return Status.bioNotAvailable;

      try {
        final isBioSuccess = await auth.authenticate(
          localizedReason: 'Please authenticate',
        );
        if (!isBioSuccess) return BiometricStatus.bioAuthFailed;

        // ## await sl<StorageService>().writeBioPublicKey(publicKey, credentialId);
        return BiometricStatus.success;
      } catch (e) {
        return BiometricStatus.bioAuthFailed;
      }
    }
  }

  Future<BiometricStatus?> verify() async {
    if (Platform.isLinux) {
      final FlutterLocalAuthentication macAuth = FlutterLocalAuthentication();
      try {
        final bool canAuthenticate = await macAuth.canAuthenticate();
        macAuth.setTouchIDAuthenticationAllowableReuseDuration(0);
        if (!canAuthenticate) return BiometricStatus.bioNotAvailable;

        final bool isBioSuccess = await macAuth.authenticate();
        if (!isBioSuccess) return BiometricStatus.bioAuthFailed;

        // final publicKey = await sl<StorageService>().readBioPublicKey();
        return BiometricStatus.success;
      } catch (e) {
        debugPrint("Exception checking support. $e");
        return BiometricStatus.bioAuthFailed;
      }
    } else {
      final bool canAuthenticateWithBiometrics = await auth.canCheckBiometrics;
      final bool canAuthenticate =
          canAuthenticateWithBiometrics || await auth.isDeviceSupported();

      if (!canAuthenticate) return BiometricStatus.bioNotAvailable;

      // final List<BiometricType> availableBiometrics =
      //     await auth.getAvailableBiometrics();

      // if (availableBiometrics.isEmpty) return null;

      try {
        final isBioSuccess =
            await auth.authenticate(localizedReason: 'Please authenticate');
        if (!isBioSuccess) return BiometricStatus.bioAuthFailed;

        // final publicKey = await sl<StorageService>().readBioPublicKey();
        return BiometricStatus.success;
      } catch (e) {
        return BiometricStatus.bioAuthFailed;
      }
    }
  }

  Future closeAuth() async {
    await auth.stopAuthentication();
  }
}
