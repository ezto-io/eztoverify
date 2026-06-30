import 'dart:io';

import 'package:ezto_device_location/src/handler/location_permission_handler.dart';
import 'package:ezto_device_location/src/model/device_location_model.dart';
import 'package:geolocator/geolocator.dart';
import 'package:location/location.dart';

class DeviceLocationService {
  final Location location = Location();
  final LocationPermissionHandler locationPermissionHandler;

  DeviceLocationService({required this.locationPermissionHandler});

  Future<TimeModel> _readDeviceTime() async {
    final DateTime time = DateTime.now();
    final utc = time.toUtc().toIso8601String();
    final epoch = time.toUtc().millisecondsSinceEpoch;
    return TimeModel(
      time: utc,
      epoch: epoch,
      offset: time.timeZoneOffset.inMinutes,
    );
  }

  Future<LocationModel> _checkDeviceLocation() async {
    return _readDeviceLocation();
  }

  Future<LocationModel> _readDeviceLocation() async {
    final isServiceEnabled = await location.serviceEnabled();
    if (!isServiceEnabled) {
      final gpsEnabled = await location.requestService();
      if (!gpsEnabled) {
        throw Exception("GPS turned off");
      }
    }

    final isPermissionGranted =
        (await location.hasPermission()) == PermissionStatus.granted;

    if (!isPermissionGranted) {
      final result = await location.requestPermission();
      if (result == PermissionStatus.deniedForever ||
          result == PermissionStatus.grantedLimited) {
        final res = await locationPermissionHandler.onPermissionDenied();
        if (res) {
          await _readDeviceLocation();
        }
      }
    }
    return _geoLocationPermissionSuccess();
  }

  Future<LocationModel> _readWindowsDeviceLocation() async {
    LocationPermission permission;
    bool serviceEnabled = await Geolocator.isLocationServiceEnabled();
    if (!serviceEnabled) {
      return Future.error('Location services are disabled.');
    }

    permission = await Geolocator.checkPermission();
    if (permission == LocationPermission.denied) {
      permission = await Geolocator.requestPermission();
      if (permission == LocationPermission.denied) {
        final res = await locationPermissionHandler.onPermissionDenied();
        if (res) {
          await _readWindowsDeviceLocation();
        }
      }
    }

    if (permission == LocationPermission.deniedForever) {
      final res = await locationPermissionHandler.onPermissionDenied();
      if (res) {
        await _readWindowsDeviceLocation();
      }
    }
    return _geoLocationPermissionSuccess();
  }

  Future<LocationModel> _geoLocationPermissionSuccess() async {
    Position position = await Geolocator.getCurrentPosition();
    return LocationModel(
      accuracy: position.accuracy,
      lat: position.latitude,
      long: position.longitude,
    );
  }

  Future<DeviceLocationModel> readDeviceDetails(List<dynamic> args) async {
    try {
      if (Platform.isWindows) {
        final location = await _readWindowsDeviceLocation();
        final result = DeviceLocationModel(
            success: true,
            message: "",
            location: location,
            time: await _readDeviceTime());
        return result;
      } else {
        final location = await _checkDeviceLocation();
        final result = DeviceLocationModel(
            success: true,
            message: "",
            location: location,
            time: await _readDeviceTime());
        return result;
      }
    } catch (e) {
      final result = DeviceLocationModel(
          success: false,
          message: e.toString(),
          location: null,
          time: await _readDeviceTime());
      return result;
    }
  }
}
