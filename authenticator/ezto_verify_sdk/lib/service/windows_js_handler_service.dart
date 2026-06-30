import 'dart:convert';

import 'package:eztoverify/enums/events.dart';
import 'package:eztoverify/service/platform_webview_controller.dart';

import 'package:eztoverify/model/payload_data_model.dart';
import 'package:eztoverify/service/js_event_mapper_service.dart';
import 'package:eztoverify/service/result_handler_service.dart';
import 'package:flutter/material.dart';

class WindowsJavaScriptHandlerService {
  final ResultHandlerService resultHandlerService = ResultHandlerService();
  late final JsEventMapperService _jsEventMapperService;
  late final Socket socketInfo;

  WindowsJavaScriptHandlerService(
    Socket socketInfo,
    BuildContext context,
  ) {
    _jsEventMapperService = JsEventMapperService(
      socketInfo: socketInfo,
      context: context,
    );
  }

  Future<void> register(
    dynamic message,
    PlatformWebViewController controller, {
    required Socket socketInfo,
  }) async {
    Map<String, dynamic> jsonMap = json.decode(message!);
    String handlerName = jsonMap['name'];
    Map<String, dynamic> params = jsonMap['params'] ?? {};
    List<dynamic> args = [params];

    await handleDynamicHandler(
      handlerName,
      args,
      controller,
    );
  }

  Future<void> handleDynamicHandler(
    String handlerName,
    List<dynamic>? args,
    PlatformWebViewController controller,
  ) async {
    try {
      final event =
          Events.values.firstWhere((element) => element.name == handlerName);
      final result = await _jsEventMapperService
          .getHandlerInstance(event, controller)
          .call(args);
      controller.windowsWebviewController?.postWebMessage(jsonEncode(result));
    } catch (e) {
      controller.windowsWebviewController?.postWebMessage(jsonEncode({
        "success": false,
        "message": "Unsupported Operation",
      }));
    }
  }
}
