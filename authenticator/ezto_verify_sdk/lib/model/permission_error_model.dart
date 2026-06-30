import 'dart:convert';

import 'package:eztoverify/enums/status.dart';

class PermissionErrorModel {
  final bool success;
  final String message;
  final List<Status> errorCodes;
  final Map<String, dynamic> device;

  PermissionErrorModel({
    this.success = false,
    this.message = "Permission denied",
    required this.errorCodes,
    required this.device,
  });

  Map<String, dynamic> toMap() {
    return {
      'success': success,
      'message': message,
      'errorCodes': errorCodes.map((x) => x.name).toList(),
      'device': device,
    };
  }

  factory PermissionErrorModel.fromMap(Map<String, dynamic> map) {
    return PermissionErrorModel(
      success: map['success'] ?? false,
      message: map['message'] ?? '',
      errorCodes: List<Status>.from(
        map['errorCodes']?.map(
          (x) => Status.values.where((element) => element.name == x),
        ),
      ),
      device: Map<String, dynamic>.from(map['device']),
    );
  }

  String toJson() => json.encode(toMap());

  factory PermissionErrorModel.fromJson(String source) =>
      PermissionErrorModel.fromMap(json.decode(source));
}
