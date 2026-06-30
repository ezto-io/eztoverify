import 'dart:convert';

class DeviceInfoModel {
  final String model;
  final bool isPhysicalDevice;
  final String osVersion;
  final String id;
  final String platform;
  DeviceInfoModel({
    required this.model,
    required this.isPhysicalDevice,
    required this.osVersion,
    required this.id,
    required this.platform,
  });

  Map<String, dynamic> toMap() {
    return {
      'model': model,
      'isPhysicalDevice': isPhysicalDevice,
      'osVersion': osVersion,
      'id': id,
      'platform': platform,
    };
  }

  String toJson() => json.encode(toMap());
}
