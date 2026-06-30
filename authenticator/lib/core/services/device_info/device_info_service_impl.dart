import 'dart:io';

import 'package:apple_product_name/apple_product_name.dart';
import 'package:device_info_plus/device_info_plus.dart';
import 'package:eztoverify_getting_started/core/services/device_info/device_info_service.dart';

class DeviceInfoServiceImpl implements DeviceInfoServices {
  String deviceModel = "";
  String deviceVersion = "";
  final DeviceInfoPlugin deviceInfoPlugin;

  DeviceInfoServiceImpl({required this.deviceInfoPlugin});

  @override
  Future<void> setDeviceModel() async {
    if (Platform.isIOS) {
      IosDeviceInfo iosInfo = await deviceInfoPlugin.iosInfo;
      deviceModel = iosInfo.utsname.productName;
    } else if (Platform.isMacOS) {
      MacOsDeviceInfo macosInfo = await deviceInfoPlugin.macOsInfo;
      deviceModel = macosInfo.model;
    } else if (Platform.isWindows) {
      WindowsDeviceInfo windowsInfo = await deviceInfoPlugin.windowsInfo;
      deviceModel = windowsInfo.computerName;
    } else {
      AndroidDeviceInfo androidInfo = await deviceInfoPlugin.androidInfo;

      deviceModel = androidInfo.model;
    }
  }

  @override
  Future<void> setDeviceVersion() async {
    if (Platform.isIOS) {
      IosDeviceInfo iosInfo = await deviceInfoPlugin.iosInfo;
      deviceVersion = iosInfo.systemVersion;
    } else if (Platform.isMacOS) {
      MacOsDeviceInfo macosInfo = await deviceInfoPlugin.macOsInfo;
      deviceVersion = "${macosInfo.majorVersion}.${macosInfo.minorVersion}";
    } else if (Platform.isWindows) {
      WindowsDeviceInfo windowsInfo = await deviceInfoPlugin.windowsInfo;
      deviceVersion =
          "${windowsInfo.productName} ${windowsInfo.majorVersion}.${windowsInfo.minorVersion} ${windowsInfo.buildNumber}";
    } else {
      AndroidDeviceInfo androidInfo = await deviceInfoPlugin.androidInfo;
      deviceVersion = androidInfo.version.release;
    }
  }

  @override
  String getDeviceModel() => deviceModel;

  @override
  String getDeviceVersion() => deviceVersion;
}
