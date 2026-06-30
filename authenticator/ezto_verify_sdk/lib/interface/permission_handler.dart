import 'package:eztoverify/enums/system_permission.dart';

abstract class PermissionHandler {
  Future<bool> onPermissionDenied(List<SystemPermission> deniedPermissions);
}
