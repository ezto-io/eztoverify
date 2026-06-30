import 'dart:convert';

class VerifyBiometricModel {
  final bool success;
  final String message;
  final Map<String, dynamic> device;
  final String? encryptedChallenge;
  final String? credentialId;
  final List<String> errorCodes;

  const VerifyBiometricModel({
    required this.success,
    required this.message,
    required this.device,
    this.encryptedChallenge,
    this.credentialId,
    required this.errorCodes,
  });

  Map<String, dynamic> toMap() {
    return {
      'success': success,
      'message': message,
      'device': device,
      'encryptedChallenge': encryptedChallenge,
      'credentialId': credentialId,
      'errorCodes': errorCodes,
    };
  }

  factory VerifyBiometricModel.fromMap(Map<String, dynamic> map) {
    return VerifyBiometricModel(
      success: map['success'] ?? false,
      message: map['message'] ?? '',
      device: Map<String, dynamic>.from(map['device']),
      encryptedChallenge: map['encryptedChallenge'],
      credentialId: map['credentialId'],
      errorCodes: List<String>.from(map['errorCodes']),
    );
  }

  String toJson() => json.encode(toMap());

  factory VerifyBiometricModel.fromJson(String source) =>
      VerifyBiometricModel.fromMap(json.decode(source));
}
