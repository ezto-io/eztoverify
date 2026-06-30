import 'package:eztoverify/service/navigation_service.dart';
import 'package:eztoverify/service/service_locator.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:permission_handler/permission_handler.dart';

part 'qr_scanner_state.dart';

class QrScannerCubit extends Cubit<QrScannerState> {
  QrScannerCubit() : super(QrScannerInitial());

  void checkCameraPermission() async {
    var status = await Permission.camera.status;
    if (!status.isGranted) {
      status = await Permission.camera.request();
      if (status.isGranted) {
        onQrSuccess();
      } else if (status.isPermanentlyDenied || status.isDenied) {
        final res = await sl<NavigationService>().showPermissionDeniedAlert(
          title: "Camera Access Denied",
          content:
              "This app requires access to the camera to function properly. Please enable camera access in Settings.",
        );
        if (res) {
          checkCameraPermission();
        } else {
          onQrFailed();
        }
      } else {
        onQrFailed();
      }
    } else {
      onQrSuccess();
    }
  }

  void onQrSuccess() {
    emit(QrScannerSuccess());
  }

  void onQrFailed() {
    emit(QrScannerFailure());
  }
}
