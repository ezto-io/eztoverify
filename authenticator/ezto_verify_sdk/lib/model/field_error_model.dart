import 'dart:convert';

import 'package:eztoverify/enums/status.dart';

class FieldErrorModel {
  final bool success;
  final String message;
  final List<Status> errorCodes;
  final Map<String, dynamic> device;

  FieldErrorModel({
    this.success = false,
    this.message = "Required fields missing",
    this.errorCodes = const [
      Status.jsMissingFields,
    ],
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

  factory FieldErrorModel.fromMap(Map<String, dynamic> map) {
    return FieldErrorModel(
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

  factory FieldErrorModel.fromJson(String source) =>
      FieldErrorModel.fromMap(json.decode(source));
}
