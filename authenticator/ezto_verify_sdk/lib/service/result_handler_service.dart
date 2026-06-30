import 'package:ezto_biometric/ezto_biometric.dart';
import 'package:desktop_webview_window/desktop_webview_window.dart';
import 'package:eztoverify/enums/close_reason.dart';
import 'package:eztoverify/enums/error_code.dart';
import 'package:eztoverify/model/payload_data_model.dart';
import 'package:eztoverify/screen/mac_web_view_browser.dart';
import 'package:eztoverify/service/api_service.dart';
import 'package:eztoverify/service/ezto_result_service.dart';
import 'package:eztoverify/service/navigation_service.dart';
import 'package:eztoverify/service/service_locator.dart';
import 'package:eztoverify/service/websocket_service.dart';
import 'package:eztoverify/handlers/device_location_platform_handler.dart';
import 'package:flutter/foundation.dart';
import 'package:eztoverify/handlers/fido_handler.dart';
import 'package:eztoverify/handlers/silent_binding_handler.dart';
import 'package:eztoverify/handlers/ashield_handler.dart';
import 'package:eztoverify/handlers/biometric_handler.dart';
import 'package:eztoverify/handlers/push_token_handler.dart';

class ResultHandlerService {
  void handleSuccess({
    MacWebViewBrowser? macWebview,
    Webview? linuxWebview,
  }) {
    debugPrint("success called");
    sl<NavigationService>().popAll();
    macWebview?.close();
    linuxWebview?.close();
    sl<EztoResultService>().onClose(CloseReason.transactionCompleted);
  }

  void handleFailure({
    required Socket socketInfo,
    MacWebViewBrowser? macWebview,
    Webview? linuxWebview,
  }) {
    debugPrint("failure called");
    sl<WebSocketService>().dispose();
    sl<ApiService>().sendConnectionStatus(
      status: ConnectionStatus.userClosed,
      socketUrl: socketInfo.url,
      requestId: socketInfo.requestId,
    );
    sl<NavigationService>().popAll();
    macWebview?.close();
    linuxWebview?.close();
    sl<EztoResultService>().onError(ErrorCode.serverError);
    sl<EztoResultService>().onClose(CloseReason.transactionCompleted);
  }

  Function getHandlerInstance(String handlerName) {
    switch (handlerName) {
      case 'checkBiometric':
        return sl<BiometricHandler>().checkBiometric;
      case 'registerBiometric':
        return sl<BiometricHandler>().registerBiometric;
      case 'verifyBiometric':
        return sl<BiometricHandler>().verifyBiometric;
      case 'registerAshield':
        return sl<AshieldHandler>().register;
      case 'authenticateAshield':
        return sl<AshieldHandler>().auth;
      case 'getFcmToken':
        return sl<PushTokenHandler>().getToken;
      case 'isJsFidoSupported':
        return sl<FidoHandler>().isJsFidoSupported;
      case 'registerFido':
        return sl<FidoHandler>().register;
      case 'signFido':
        return sl<FidoHandler>().auth;
      case 'registerSilentBinding':
        return sl<SilentBindingHandler>().registerSilentBinding;
      case 'verifySilentBinding':
        return sl<SilentBindingHandler>().verifySilentBinding;
      case 'readDeviceDetails':
        var readDeviceDetails = getReadDeviceDetailsFunction();
        if (readDeviceDetails != null) {
          return readDeviceDetails;
        } else {
          throw Exception('Handler not found: $handlerName');
        }
      default:
        throw Exception('Handler not found: $handlerName');
    }
  }

  Map<String, dynamic> getResult({
    required String handlerName,
    required Map<String, dynamic> params,
  }) {
    return {"name": handlerName, "params": params};
  }

  void dispose() {
    sl<BiometricService>().closeAuth();
    // "TODO Dispose fido and ashield"
  }
}
