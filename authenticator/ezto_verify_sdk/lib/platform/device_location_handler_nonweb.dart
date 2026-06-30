import 'package:eztoverify/handlers/device_location_handler.dart';

DeviceLocationHandler? getDeviceLocationHandler() {
  return DeviceLocationHandler();
}

Function? getReadDeviceDetailsFunction() {
  return getDeviceLocationHandler()?.readDeviceDetails;
}
