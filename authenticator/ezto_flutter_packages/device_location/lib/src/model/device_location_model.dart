import 'dart:convert';

DeviceLocationModel deviceLocationModelFromJson(String str) =>
    DeviceLocationModel.fromJson(json.decode(str));

class DeviceLocationModel {
  bool? success;
  String? message;
  LocationModel? location;
  TimeModel? time;

  DeviceLocationModel({
    this.success,
    this.message,
    this.location,
    this.time,
  });

  factory DeviceLocationModel.fromJson(Map<String, dynamic> json) =>
      DeviceLocationModel(
        success: json["success"],
        message: json["message"],
        location: LocationModel.fromJson(json["location"]),
        time: TimeModel.fromJson(json["time"]),
      );

  Map<String, dynamic> toJson({required Map<String, dynamic> device}) => {
        "success": success,
        "message": message,
        "location": location?.toJson(),
        "time": time?.toJson(),
        "device": device
      };
}

class LocationModel {
  double? accuracy;
  double? lat;
  double? long;

  LocationModel({
    this.accuracy,
    this.lat,
    this.long,
  });

  factory LocationModel.fromJson(Map<String, dynamic> json) => LocationModel(
        accuracy: json["accuracy"].toDouble(),
        lat: json["lat"].toDouble(),
        long: json["long"].toDouble(),
      );

  Map<String, dynamic> toJson() => {
        "accuracy": accuracy,
        "lat": lat,
        "long": long,
      };
}

class TimeModel {
  String? time;
  int? epoch;
  int? offset;

  TimeModel({
    this.time,
    this.epoch,
    this.offset,
  });

  factory TimeModel.fromJson(Map<String, dynamic> json) => TimeModel(
        time: json["time"],
        epoch: json["epoch"],
        offset: json["offset"],
      );

  Map<String, dynamic> toJson() => {
        "time": time,
        "epoch": epoch,
        "offset": offset,
      };
}
