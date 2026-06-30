class RegisterSilentBindingModel {
  bool? success;
  String? message;
  String? encryptedDeviceId;

  RegisterSilentBindingModel(
      {this.success, this.message, this.encryptedDeviceId});

  RegisterSilentBindingModel.fromJson(Map<String, dynamic> json) {
    success = json['success'];
    message = json['message'];
    encryptedDeviceId = json['encryptedDeviceId'];
  }

  Map<String, dynamic> toJson({required Map<String, dynamic> device}) {
    return {
      'success': success,
      'message': message,
      'encryptedDeviceId': encryptedDeviceId,
      'device': device
    };
  }
}
