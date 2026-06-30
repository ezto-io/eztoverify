import 'package:eztoverify/enums/events.dart';
import 'package:eztoverify/screen/mac_web_view_browser.dart';
import 'package:eztoverify/service/js_event_mapper_service.dart';
import 'package:eztoverify/service/platform_webview_controller.dart';
import 'package:eztoverify/model/payload_data_model.dart';
import 'package:flutter/material.dart';

class JavaScriptHandlerService {
  final Socket socketInfo;
  final MacWebViewBrowser? inAppBrowser;
  late final JsEventMapperService _jsEventMapperService;
  final BuildContext context;

  JavaScriptHandlerService({
    required this.socketInfo,
    this.inAppBrowser,
    required this.context,
  }) {
    _jsEventMapperService = JsEventMapperService(
      socketInfo: socketInfo,
      inAppBrowser: inAppBrowser,
      context: context,
    );
  }

  Future<void> register(
    PlatformWebViewController controller, {
    required Socket socketInfo,
  }) async {
    for (var element in Events.values) {
      controller.mobileWebviewController?.addJavaScriptHandler(
        handlerName: element.name,
        callback: (args) => _jsEventMapperService
            .getHandlerInstance(
              element,
              controller,
            )
            .call(args),
      );
    }
  }

  void dispose() {}
}
