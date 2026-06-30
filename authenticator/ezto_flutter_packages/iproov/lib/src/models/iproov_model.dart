class IproovModel {
  bool? success;
  String? message;
  String? error;

  IproovModel({this.success, this.message, this.error});

  IproovModel.fromJson(Map<String, dynamic> json) {
    success = json['success'];
    message = json['message'];
    error = json['error'];
  }

  Map<String, dynamic> toJson() {
    return {
      'success': success,
      'message': message,
      'error': error,
    };
  }
}
