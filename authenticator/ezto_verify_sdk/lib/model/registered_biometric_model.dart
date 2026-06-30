import 'dart:convert';

class RegisteredBiometricModel {
  final bool registered;
  final List<String> credentialIds;
  final Map<String, dynamic> device;
  final List<String> errorCodes;

  RegisteredBiometricModel({
    required this.registered,
    required this.credentialIds,
    required this.device,
    required this.errorCodes,
  });

  Map<String, dynamic> toMap() {
    return {
      'registered': registered,
      'credentialIds': credentialIds,
      'device': device,
      'errorCodes': errorCodes,
    };
  }

  factory RegisteredBiometricModel.fromMap(Map<String, dynamic> map) {
    return RegisteredBiometricModel(
      registered: map['registered'] ?? false,
      credentialIds: List<String>.from(map['credentialIds']),
      device: Map<String, dynamic>.from(map['device']),
      errorCodes: List<String>.from(map['errorCodes']),
    );
  }

  String toJson() => json.encode(toMap());

  factory RegisteredBiometricModel.fromJson(String source) =>
      RegisteredBiometricModel.fromMap(json.decode(source));
}
