class VerifySilentBindingModel {
  bool? success;
  String? message;
  String? challengeHash;
  String? keyHashFirst6;

  VerifySilentBindingModel(
      {this.success, this.message, this.challengeHash, this.keyHashFirst6});

  VerifySilentBindingModel.fromJson(Map<String, dynamic> json) {
    success = json['success'];
    message = json['message'];
    challengeHash = json['challengeHash'];
    keyHashFirst6 = json['keyHashFirst6'];
  }

  Map<String, dynamic> toJson({required Map<String, dynamic> device}) {
    return {
      'success': success,
      'message': message,
      'challengeHash': challengeHash,
      'keyHashFirst6': keyHashFirst6,
      'device': device
    };
  }
}
