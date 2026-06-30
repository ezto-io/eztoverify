import 'package:eztoverify/handlers/device_info_handler.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:eztoverify/enums/status.dart';
import 'package:eztoverify/service/custom_permission_service.dart';
import 'package:eztoverify/service/service_locator.dart';

abstract class JsHandler {
  final List<Permission> requiredPermissions = [];

  Future<List<Status>> checkForPermissions() async {
    return sl<CustomPermissionService>()
        .requestPermissions(requiredPermissions);
  }

  List<String> validateRequiredFields(
    Map<String, dynamic> args,
    List<String> fieldKeys,
  ) {
    List<String> missingFields = [];
    for (var element in fieldKeys) {
      bool isAvailable = args.containsKey(element);
      if (!isAvailable) {
        missingFields.add(element);
      }
    }
    return missingFields;
  }

  Map<String, dynamic> processArgs(List<dynamic> args) {
    return args[0];
  }

  Future<Map<String, dynamic>> getDeviceInfo() async {
    return await sl<DeviceInfoHandler>().getDeviceInfo();
  }

  List<String> mapErrors(String? errorCode) {
    if (errorCode == null) return [];

    final code = Status.values.cast<Status?>().firstWhere(
          (element) => element?.name == errorCode.toLowerCase(),
          orElse: () => null,
        );

    if (code == null || code == Status.success) return [];
    return [code.name];
  }
}
