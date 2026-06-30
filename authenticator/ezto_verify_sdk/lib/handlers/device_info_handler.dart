import 'package:eztoverify/service/service_locator.dart';
import 'package:eztoverify/service/storage_service.dart';
import 'package:ezto_device_info/ezto_device_info.dart';

class DeviceInfoHandler {
  final deviceInfo = DeviceInfoService();

  Future<Map<String, dynamic>> getDeviceInfo() async {
    final id = await sl<StorageService>().createOrGetDeviceId();
    final result = (await deviceInfo.getDeviceInfo()).toJson(id: id);
    return result;
  }

  Future<String?> getBindingId() async {
    return deviceInfo.getBindingId();
  }
}
