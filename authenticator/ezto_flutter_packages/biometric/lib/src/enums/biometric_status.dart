enum BiometricStatus {
  //biometric
  bioNoCredIds, //User not registered any biometric flows
  bioNotAvailable, //User Biometric not available
  bioAuthFailed, //Biometric auth failed
  bioPublicKeyNotFound, success, //Biometric public key not available for verification
}
