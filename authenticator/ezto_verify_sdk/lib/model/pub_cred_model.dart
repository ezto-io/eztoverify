import 'dart:convert';

List<PubCredModel> pubCredModelFromJson(String str) => List<PubCredModel>.from(
    json.decode(str).map((x) => PubCredModel.fromJson(x)));

String pubCredModelToJson(List<PubCredModel> data) =>
    json.encode(List<dynamic>.from(data.map((x) => x.toJson())));

class PubCredModel {
  String type;
  int alg;

  PubCredModel({
    required this.type,
    required this.alg,
  });

  factory PubCredModel.fromJson(Map<String, dynamic> json) => PubCredModel(
        type: json["type"],
        alg: int.parse(json["alg"].toString()),
      );

  Map<String, dynamic> toJson() => {
        "type": type,
        "alg": alg,
      };
}
