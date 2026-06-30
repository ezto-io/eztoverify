class DeviceInfoModel {
  String? model;
  bool? isPhysicalDevice;
  String? osVersion;
  String? platform;

  DeviceInfoModel(
      {this.model, this.isPhysicalDevice, this.osVersion, this.platform});

  DeviceInfoModel.fromJson(Map<String, dynamic> json) {
    model = json['model'];
    isPhysicalDevice = json['isPhysicalDevice'];
    osVersion = json['osVersion'];
    platform = json['platform'];
  }

  Map<String, dynamic> toJson({required String id}) {
    return {
      'model': model,
      'isPhysicalDevice': isPhysicalDevice,
      'osVersion': osVersion,
      'platform': platform,
      'id': id
    };
  }
}
