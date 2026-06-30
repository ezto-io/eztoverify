import 'dart:async';
import 'dart:io';

import 'package:ezto_platform_interface/ezto_permission_enum.dart';
import 'package:ezto_platform_interface/ezto_platform_interface.dart';
import 'package:flutter/material.dart';
import 'package:ezto_iproov/src/models/iproov_model.dart';
import 'package:ezto_iproov/src/services/iproov_error_service.dart';
import 'package:iproov_flutter/iproov_flutter.dart';

class IproovService implements EztoPlatformInterface {
  @override
  Map<String, dynamic> get availableHandlers => {
        "registerIproov": {
          "permissions": [
            EztoPermissionEnum.camera.name,
          ],
          "requiredFields": [
            "streamingUrl",
            "token",
          ]
        },
        "verifyIproov": {
          "permissions": [
            EztoPermissionEnum.camera.name,
          ],
          "requiredFields": [
            "streamingUrl",
            "token",
          ]
        }
      };

  @override
  Future<Map<String, dynamic>> executeHandler(
      String fnName, Map<String, dynamic> data) {
    final streamingUrl = data['streamingUrl'];
    final token = data['token'];

    if (fnName == "registerIproov") {
      if (Platform.isAndroid || Platform.isIOS) {
        final result = _init(
          streamingUrl: streamingUrl,
          token: token,
        );
        return result.then((model) => model.toJson());
      } else {
        throw UnsupportedError("Platform not supported");
      }
    } else if (fnName == "verifyIproov") {
      if (Platform.isAndroid || Platform.isIOS) {
        final result = _init(
          streamingUrl: streamingUrl,
          token: token,
        );
        return result.then((model) => model.toJson());
      } else {
        throw UnsupportedError("Platform not supported");
      }
    }
    throw ArgumentError("Invalid function name: $fnName");
  }

  // Future<IproovModel> iproovInit({
  //   required String streamingUrl,
  //   required String token,
  // }) async {
  //   if (Platform.isAndroid || Platform.isIOS) {
  //     return _init(
  //       streamingUrl: streamingUrl,
  //       token: token,
  //     );
  //   } else {
  //     throw UnsupportedError("Platform not supported");
  //   }
  // }

  Future<IproovModel> _init({
    required String streamingUrl,
    required String token,
  }) async {
    final completer = Completer<IproovModel>();

    try {
      final iproovStream = IProov.launch(
        streamingUrl: streamingUrl,
        token: token,
      );

      iproovStream.listen((event) {
        if (event is IProovEventConnecting) {
          _onIProovConnecting(event);
        } else if (event is IProovEventConnected) {
          _onIProovConnected(event);
        } else if (event is IProovEventProcessing) {
          _onIProovProcessing(event);
        } else if (event is IProovEventSuccess) {
          _onIProovSuccess(event, completer);
        } else if (event is IProovEventCanceled) {
          _onIproovCancelled(event, completer);
        } else if (event is IProovEventFailure) {
          _onIProovFailure(event, completer);
        } else if (event is IProovEventError) {
          _onIProovError(event, completer);
        }
      });
    } catch (e) {
      debugPrint("e : ${e.toString()}");
    }
    return completer.future;
  }

  void _onIProovConnecting(IProovEventConnecting event) {}

  void _onIProovConnected(IProovEventConnected event) {}

  void _onIProovProcessing(IProovEventProcessing event) {}

  void _onIProovSuccess(
      IProovEventSuccess event, Completer<IproovModel> completer) async {
    _handleIProovEvent(
      isSuccess: true,
      message: "iproov successfully",
      error: "",
      completer: completer,
    );
  }

  void _onIproovCancelled(
      IProovEventCanceled event, Completer<IproovModel> completer) async {
    _handleIProovEvent(
      isSuccess: false,
      message: "",
      error: "Unable to complete request. Please try again!",
      completer: completer,
    );
  }

  void _onIProovFailure(
      IProovEventFailure event, Completer<IproovModel> completer) async {
    final IproovErrorService iproovErrorService = IproovErrorService();
    _handleIProovEvent(
      isSuccess: false,
      message: "",
      error: iproovErrorService.feedbackCodeToReason(event.feedbackCode),
      completer: completer,
    );
  }

  void _onIProovError(
    IProovEventError event,
    Completer<IproovModel> completer,
  ) async {
    final IProovException error = event.error;
    String errorMessage = "Unable to complete request. Please try again!";
    if (error is CaptureAlreadyActiveException) {
      errorMessage = error.message ?? "CaptureAlreadyActiveException";
    } else if (error is NetworkException) {
      errorMessage = error.message ?? "NetworkException";
    } else if (error is CameraPermissionException) {
      errorMessage = error.message ?? "CameraPermissionException";
    } else if (error is ServerException) {
      errorMessage = error.message ?? "ServerException";
    } else if (error is ListenerNotRegisteredException) {
      errorMessage = error.message ?? "ListenerNotRegisteredException";
    } else if (error is MultiWindowUnsupportedException) {
      errorMessage = error.message ?? "MultiWindowUnsupportedException";
    } else if (error is CameraException) {
      errorMessage = error.message ?? "CameraException";
    } else if (error is FaceDetectorException) {
      errorMessage = error.message ?? "FaceDetectorException";
    } else if (error is UnsupportedDeviceException) {
      errorMessage = error.message ?? "UnsupportedDeviceException";
    } else if (error is InvalidOptionsException) {
      errorMessage = error.message ?? "InvalidOptionsException";
    } else {
      errorMessage = error.message ?? "unknown erro";
    }
    _handleIProovEvent(
      isSuccess: false,
      message: "",
      error: errorMessage,
      completer: completer,
    );
  }

  Future<void> _handleIProovEvent({
    required bool isSuccess,
    required String message,
    required String error,
    required Completer<IproovModel> completer,
  }) async {
    final res = IproovModel(
      success: isSuccess,
      message: message,
      error: error,
    );
    IProov.cancel();
    completer.complete(res);
  }
}
