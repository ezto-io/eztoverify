import 'package:eztoverify/enums/system_permission.dart';
import 'package:permission_handler/permission_handler.dart';

class PermissionService {
  List<Permission> statusMapperErrorPermissionTo(
      List<SystemPermission> permissions) {
    permissions.first.name;
    return permissions.map((permission) {
      switch (permission) {
        case SystemPermission.osCameraPermission:
          return Permission.camera;
        case SystemPermission.osMicPermission:
          return Permission.microphone;
        case SystemPermission.osPhonePermission:
          return Permission.phone;
        case SystemPermission.osSmsPermission:
          return Permission.sms;
        default:
          return Permission.unknown;
      }
    }).toList();
  }

  Future<bool> requestPermission(
      {required List<Permission> permissionToRequest}) async {
    permissionToRequest.remove(Permission.unknown);
    bool allrequestedPermissionApproved = true;

    final statuses = await permissionToRequest.request();
    statuses.forEach((key, value) {
      if (!value.isGranted) {
        allrequestedPermissionApproved = false;
      }
    });

    return allrequestedPermissionApproved;
  }
}
