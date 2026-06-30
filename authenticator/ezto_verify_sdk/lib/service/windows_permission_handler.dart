import 'package:flutter/material.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:eztoverify/enums/status.dart';
import 'package:eztoverify/enums/system_permission.dart';
import 'package:eztoverify/interface/permission_handler.dart';
import 'package:webview_windows/webview_windows.dart';

class WindowsPermissionService {
  final PermissionHandler handler;

  WindowsPermissionService({
    required this.handler,
  });
  //If all requested permissions are allowed, the response will be empty list
  //else will contain all denied permissions list
  Future<List<Status>> requestPermissionFromWebRequest(
    WebviewPermissionKind request,
  ) async {
    final List<Permission> requiredPermissions = _requestToPermission(request);
    final failedPermissions = await _requestPermissions(requiredPermissions);

    if (failedPermissions.isNotEmpty) {
      //send status back to app
      final updatedFailedPermissions = await onPermissionDenied(
        failedPermissions,
      );
      return _permissionToErrorStatusMapper(updatedFailedPermissions);
    }

    return _permissionToErrorStatusMapper(failedPermissions);
  }

  Future<List<Permission>> onPermissionDenied(
    List<Permission> failedPermissions,
  ) async {
    final failed = _permissionToErrorStatusMapper(failedPermissions);
    final List<SystemPermission> toRecheck = [];
    for (var element in failed) {
      try {
        final mapEnum = SystemPermission.values.byName(element.name);
        toRecheck.add(mapEnum);
      } catch (e) {
        //ignore
      }
    }
    await handler.onPermissionDenied(toRecheck);
    //Verify again
    return _verifyPermissions(failedPermissions);
  }

  List<Status> _permissionToErrorStatusMapper(List<Permission> permissions) {
    return permissions.map((permission) {
      switch (permission) {
        case Permission.camera:
          return Status.osCameraPermission;
        case Permission.microphone:
          return Status.osMicPermission;
        case Permission.phone:
          return Status.osPhonePermission;
        case Permission.sms:
          return Status.osSmsPermission;
        default:
          return Status.osOtherPermission;
      }
    }).toList();
  }

  List<Permission> _requestToPermission(WebviewPermissionKind request) {
    List<Permission> permissions = [];
    if (request == WebviewPermissionKind.camera) {
      permissions.add(Permission.camera);
    }
    if (request == WebviewPermissionKind.microphone) {
      permissions.add(Permission.microphone);
    }
    return permissions;
  }

  Future<List<Permission>> _requestPermissions(
    List<Permission> requiredPermissions,
  ) async {
    List<Permission> failedPermissions = [];

    for (var permission in requiredPermissions) {
      if (await permission.isGranted) {
        continue;
      }

      final status = await permission.request();
      final isGranted = status == PermissionStatus.granted;
      if (!isGranted) {
        failedPermissions.add(permission);
      }
    }
    debugPrint("failedPermissions = $failedPermissions");

    return failedPermissions;
  }

  Future<List<Permission>> _verifyPermissions(
    List<Permission> requiredPermissions,
  ) async {
    final List<Permission> failedPermissions = [];

    for (var permission in requiredPermissions) {
      if (!(await permission.isGranted)) {
        failedPermissions.add(permission);
      }
    }
    debugPrint("_verifyPermissions failedPermissions = $failedPermissions");

    return failedPermissions;
  }
}
