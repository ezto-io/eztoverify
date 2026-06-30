abstract interface class DeviceInfoServices {
  Future<void> setDeviceModel();
  Future<void> setDeviceVersion();

  String getDeviceModel();
  String getDeviceVersion();
}
