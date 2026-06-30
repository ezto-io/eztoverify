import 'dart:io';

import 'package:android_id/android_id.dart';
import 'package:ezto_device_info/src/model/device_info_model.dart';
import 'package:device_info_plus/device_info_plus.dart';

class DeviceInfoService {
  final androidIdPlugin = const AndroidId();
  final deviceInfo = DeviceInfoPlugin();

  Future<DeviceInfoModel> getDeviceInfo() async {
    if (Platform.isAndroid) {
      return _getAndroidDeviceInfo();
    } else if (Platform.isIOS) {
      return _getIosDeviceInfo();
    } else if (Platform.isMacOS) {
      return _getmacOSInfo();
    } else if (Platform.isWindows) {
      return _getWindowsInfo();
    } else if (Platform.isLinux) {
      return _getLinuxInfo();
    } else {
      throw UnsupportedError("Platform not supported");
    }
  }

  Future<DeviceInfoModel> _getAndroidDeviceInfo() async {
    AndroidDeviceInfo info = await deviceInfo.androidInfo;
    return DeviceInfoModel(
      model: "${info.brand} ${info.model}",
      isPhysicalDevice: info.isPhysicalDevice,
      osVersion: info.version.release,
      platform: "Android",
    );
  }

  Future<DeviceInfoModel> _getIosDeviceInfo() async {
    IosDeviceInfo info = await deviceInfo.iosInfo;
    return DeviceInfoModel(
      model: info.model,
      isPhysicalDevice: info.isPhysicalDevice,
      osVersion: "${info.systemName} ${info.systemVersion}",
      platform: "iOS",
    );
  }

  Future<DeviceInfoModel> _getmacOSInfo() async {
    MacOsDeviceInfo info = await deviceInfo.macOsInfo;
    return DeviceInfoModel(
      model: info.model,
      isPhysicalDevice: true,
      osVersion: "${info.majorVersion}.${info.minorVersion}",
      platform: "macOS",
    );
  }

  Future<DeviceInfoModel> _getWindowsInfo() async {
    WindowsDeviceInfo info = await deviceInfo.windowsInfo;
    return DeviceInfoModel(
      model: info.computerName,
      isPhysicalDevice: true,
      osVersion:
          "${info.productName} ${info.majorVersion}.${info.minorVersion} ${info.buildNumber}",
      platform: "Windows",
    );
  }

  Future<DeviceInfoModel> _getLinuxInfo() async {
    LinuxDeviceInfo info = await deviceInfo.linuxInfo;
    return DeviceInfoModel(
      model: info.prettyName,
      isPhysicalDevice: true,
      osVersion: "${info.name} ${info.version}",
      platform: "Linux",
    );
  }

  Future<String?> getBindingId() async {
    if (Platform.isAndroid) {
      return androidIdPlugin.getId();
    } else if (Platform.isIOS) {
      final IosDeviceInfo info = await deviceInfo.iosInfo;
      return info.identifierForVendor;
    } else if (Platform.isMacOS) {
      final MacOsDeviceInfo info = await deviceInfo.macOsInfo;
      return info.systemGUID;
    } else if (Platform.isWindows) {
      final WindowsDeviceInfo info = await deviceInfo.windowsInfo;
      return info.deviceId;
    } else if (Platform.isLinux) {
      final LinuxDeviceInfo info = await deviceInfo.linuxInfo;
      return info.machineId;
    } else {
      throw UnimplementedError('Platform not implemented');
    }
  }
}
