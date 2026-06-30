import 'package:eztoverify/enums/events.dart';
import 'package:eztoverify/enums/status.dart';
import 'package:eztoverify/handlers/biometric_handler.dart';
// import 'package:eztoverify/handlers/ezto_pass_handler.dart';
import 'package:eztoverify/handlers/fido_handler.dart';
// import 'package:eztoverify/handlers/iproov_handler.dart';
import 'package:eztoverify/handlers/push_token_handler.dart';
import 'package:eztoverify/handlers/silent_binding_handler.dart';
import 'package:eztoverify/handlers/system_service_handler.dart';
import 'package:eztoverify/handlers/url_service_handler.dart';
import 'package:eztoverify/model/payload_data_model.dart';
import 'package:eztoverify/screen/mac_web_view_browser.dart';
import 'package:eztoverify/service/platform_webview_controller.dart';
import 'package:eztoverify/service/result_handler_service.dart';
import 'package:eztoverify/service/service_locator.dart';
import 'package:eztoverify/handlers/device_location_platform_handler.dart';
import 'package:flutter/material.dart';

class JsEventMapperService {
  final MacWebViewBrowser? inAppBrowser;
  final Socket socketInfo;
  final BuildContext context;
  final ResultHandlerService resultHandlerService = ResultHandlerService();
  final commonError = {
    "success": false,
    "message": "Required fields missing",
    "errorCodes": [Status.platformNotSupported.name],
  };

  JsEventMapperService({
    this.inAppBrowser,
    required this.socketInfo,
    required this.context,
  });

  Function getHandlerInstance(
    Events eventName,
    PlatformWebViewController controller,
  ) {
    switch (eventName) {
      case Events.checkBiometric:
        return sl<BiometricHandler>().checkBiometric;
      case Events.registerBiometric:
        return sl<BiometricHandler>().registerBiometric;
      case Events.verifyBiometric:
        return sl<BiometricHandler>().verifyBiometric;
      case Events.getFcmToken:
        return sl<PushTokenHandler>().getToken;
      case Events.isJsFidoSupported:
        return sl<FidoHandler>().isJsFidoSupported;
      case Events.registerFido:
        return sl<FidoHandler>().register;
      case Events.signFido:
        return sl<FidoHandler>().auth;
      case Events.registerSilentBinding:
        return sl<SilentBindingHandler>().registerSilentBinding;
      case Events.verifySilentBinding:
        return sl<SilentBindingHandler>().verifySilentBinding;
      case Events.readDeviceDetails:
        var readDeviceDetails = getReadDeviceDetailsFunction();
        if (readDeviceDetails != null) {
          return readDeviceDetails;
        } else {
          return (args) {
            return commonError;
          };
        }
      case Events.copyToClipboard:
        return sl<SystemServiceHandler>().copyToClipboard;
      case Events.getCurrentNetwork:
        return sl<SystemServiceHandler>().getCurrentNetwork;
      case Events.isEventSupported:
        return () {
          return {
            "success": true,
            "message": "",
            "supportedEvents": Events.values.map((e) => e.name).toList()
          };
        };
      case Events.success:
        resultHandlerService.handleSuccess(
          macWebview: inAppBrowser,
        );
        return (args) {
          return {"success": true};
        };
      case Events.failure:
        resultHandlerService.handleFailure(
          socketInfo: socketInfo,
          macWebview: inAppBrowser,
        );
        return (args) {
          return {"success": true};
        };
      case Events.registerIproov:
        // return sl<IproovHandler>().register;
        return (args) {
          return commonError;
          // return sl<EztoPassHandler>().getSdmNdef(context, args);
        };
      case Events.verifyIproov:
        // return sl<IproovHandler>().verify;
        return (args) {
          return commonError;
          // return sl<EztoPassHandler>().getSdmNdef(context, args);
        };
      case Events.loadSekuraSessionUrl:
        return sl<SystemServiceHandler>().loadSekuraSessionUrl;
      case Events.isCustomSchemeRegisteredOnApp:
        return sl<SystemServiceHandler>().isCustomSchemeRegisteredOnApp;
      case Events.loadUrl:
        return (args) {
          return sl<UrlServiceHandler>().loadUrl(args, controller);
        };
      case Events.registerEztoPass:
        return (args) {
          return commonError;

          // return sl<EztoPassHandler>().register(context, args);
        };
      case Events.createSdm:
        return (args) {
          return commonError;

          // return sl<EztoPassHandler>().createSdm(context, args);
        };
      case Events.getSdmNdef:
        return (args) {
          return commonError;
          // return sl<EztoPassHandler>().getSdmNdef(context, args);
        };
    }
  }
}
