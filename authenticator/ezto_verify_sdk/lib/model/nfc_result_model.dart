class NfcResultModel {
  final String tagId;
  final String privateKey;

  NfcResultModel(this.tagId, this.privateKey);

  // Add a method to convert the model object to a Map
  Map<String, dynamic> toJson() {
    return {
      "tag_id": tagId,
      "private_key": privateKey,
    };
  }

  // Add a factory constructor to create a model object from a Map
  factory NfcResultModel.fromJson(Map<String, dynamic> json) {
    return NfcResultModel(
      json['tag_id'] as String,
      json['private_key'] as String,
    );
  }
}
