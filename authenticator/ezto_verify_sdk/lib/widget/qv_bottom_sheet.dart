import 'package:desktop_webview_window/desktop_webview_window.dart';
import 'package:eztoverify/bloc/web_view/web_view_cubit.dart';
import 'package:eztoverify/model/payload_data_model.dart';
import 'package:eztoverify/service/linux_js_handler_service.dart';
import 'package:eztoverify/service/service_locator.dart';
import 'package:eztoverify/service/window_manager_service.dart';
import 'package:flutter/material.dart';
import 'package:modal_bottom_sheet/modal_bottom_sheet.dart';
import 'package:package_info_plus/package_info_plus.dart';

Future<T> showQvBottomSheet<T>(BuildContext context, Widget child) async {
  return await showCupertinoModalBottomSheet(
    context: context,
    enableDrag: false,
    elevation: 9,
    isDismissible: false,
    topRadius: const Radius.circular(30),
    barrierColor: Colors.grey,
    builder: (context) {
      return child;
    },
  );
}

Future showLinuxBrowser({required DataPayload dataPayload}) async {
  final LinuxJavaScriptHandlerService browser = LinuxJavaScriptHandlerService();
  final WebViewCubit cubit = WebViewCubit();
  cubit.setupWebview(dataPayload);
  PackageInfo packageInfo = await PackageInfo.fromPlatform();
  var getSize = await sl<WindowManagerService>().getSize();
  var getPosition = await sl<WindowManagerService>().getPosition();
  final webview = await WebviewWindow.create(
    configuration: CreateConfiguration(
      windowHeight: getSize.height.toInt(),
      windowWidth: getSize.width.toInt(),
      windowPosX: getPosition.dx.toInt(),
      windowPosY: getPosition.dy.toInt(),
      title: packageInfo.appName,
    ),
  );
  await browser.register(webview, socketInfo: dataPayload.payload.data.socket);
  webview.launch(dataPayload.payload.data.action);
}
