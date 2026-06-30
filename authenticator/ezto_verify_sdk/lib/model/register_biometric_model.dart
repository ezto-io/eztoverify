import 'dart:convert';

class RegisterBiometricModel {
  final bool success;
  final String message;
  final Map<String, dynamic> device;
  final List<String> errorCodes;

  RegisterBiometricModel({
    required this.success,
    required this.message,
    required this.device,
    required this.errorCodes,
  });

  Map<String, dynamic> toMap() {
    return {
      'success': success,
      'message': message,
      'device': device,
      'errorCodes': errorCodes,
    };
  }

  factory RegisterBiometricModel.fromMap(Map<String, dynamic> map) {
    return RegisterBiometricModel(
      success: map['success'] ?? false,
      message: map['message'] ?? '',
      device: Map<String, dynamic>.from(map['device']),
      errorCodes: List<String>.from(map['errorCodes']),
    );
  }

  String toJson() => json.encode(toMap());

  factory RegisterBiometricModel.fromJson(String source) =>
      RegisterBiometricModel.fromMap(json.decode(source));
}
