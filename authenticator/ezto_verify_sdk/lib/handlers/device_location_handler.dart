import 'package:ezto_device_location/ezto_device_location.dart';
import 'package:eztoverify/interface/js_handler.dart';
import 'package:eztoverify/service/navigation_service.dart';
import 'package:eztoverify/service/service_locator.dart';

class DeviceLocationHandler extends JsHandler
    implements LocationPermissionHandler {
  // final Location location = Location();
  // Future<Map<String, dynamic>> readDeviceTime() async {
  //   final DateTime time = DateTime.now();
  //   final utc = time.toUtc().toIso8601String();
  //   final epoch = time.toUtc().millisecondsSinceEpoch;
  //   return {
  //     "time": utc,
  //     "epoch": epoch,
  //     "offset": time.timeZoneOffset.inMinutes,
  //   };
  // }

  // Future<Map<String, dynamic>> readDeviceLocation() async {
  //   final isServiceEnabled = await location.serviceEnabled();
  //   if (!isServiceEnabled) {
  //     final gpsEnabled = await location.requestService();
  //     if (!gpsEnabled) {
  //       throw Exception("GPS turned off");
  //     }
  //   }

  //   final isPermissionGranted =
  //       (await location.hasPermission()) == PermissionStatus.granted;

  //   if (!isPermissionGranted) {
  //     final result = await location.requestPermission();
  //     if (result == PermissionStatus.deniedForever ||
  //         result == PermissionStatus.grantedLimited) {
  //       final res = await _permissionDeniedAlert();
  //       if (res) {
  //         await readDeviceLocation();
  //       }
  //     }
  //   }
  //   return _geoLocationPermissionSuccess();
  // }

  // Future<Map<String, dynamic>> readWindowsDeviceLocation() async {
  //   LocationPermission permission;
  //   bool serviceEnabled = await Geolocator.isLocationServiceEnabled();
  //   if (!serviceEnabled) {
  //     return Future.error('Location services are disabled.');
  //   }

  //   permission = await Geolocator.checkPermission();
  //   if (permission == LocationPermission.denied) {
  //     permission = await Geolocator.requestPermission();
  //     if (permission == LocationPermission.denied) {
  //       final res = await _permissionDeniedAlert();
  //       if (res) {
  //         await readWindowsDeviceLocation();
  //       }
  //     }
  //   }

  //   if (permission == LocationPermission.deniedForever) {
  //     final res = await _permissionDeniedAlert();
  //     if (res) {
  //       await readWindowsDeviceLocation();
  //     }
  //   }
  //   return _geoLocationPermissionSuccess();
  // }

  Future<bool> _permissionDeniedAlert() async {
    final result = await sl<NavigationService>().showPermissionDeniedAlert(
      title: "Location Access Denied",
      content:
          "This app requires access to the location to function properly. Please enable location access in Settings.",
    );
    return result;
  }

  // Future<Map<String, dynamic>> _geoLocationPermissionSuccess() async {
  //   Position position = await Geolocator.getCurrentPosition();
  //   return {
  //     "accuracy": position.accuracy,
  //     "lat": position.latitude,
  //     "long": position.longitude,
  //   };
  // }

  Future<Map<String, dynamic>> readDeviceDetails(List<dynamic> args) async {
    final result = await DeviceLocationService(locationPermissionHandler: this)
        .readDeviceDetails(args);
    final json = result.toJson(
      device: await getDeviceInfo(),
    );
    return json;
  }

  @override
  Future<bool> onPermissionDenied() {
    return _permissionDeniedAlert();
  }
}
