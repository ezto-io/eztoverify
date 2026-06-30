import 'dart:async';
import 'package:eztoverify/enums/close_reason.dart';
import 'package:eztoverify/model/payload_data_model.dart';
import 'package:eztoverify/service/ezto_result_service.dart';
import 'package:eztoverify/service/java_script_handler_service.dart';
import 'package:eztoverify/service/platform_webview_controller.dart';
import 'package:eztoverify/service/service_locator.dart';
import 'package:flutter/widgets.dart';
import 'package:flutter_inappwebview/flutter_inappwebview.dart';
import 'package:url_launcher/url_launcher.dart';

class MacWebViewBrowser extends InAppBrowser {
  late JavaScriptHandlerService _handlerService;
  final DataPayload dataPayload;
  final BuildContext context;

  MacWebViewBrowser({
    super.windowId,
    super.initialUserScripts,
    required this.dataPayload,
    required this.context,
  }) {
    _handlerService = JavaScriptHandlerService(
      socketInfo: dataPayload.payload.data.socket,
      inAppBrowser: this,
      context: context,
    );
  }

  @override
  Future onBrowserCreated() async {
    debugPrint("Browser Created!");
  }

  @override
  Future<bool?>? onCreateWindow(CreateWindowAction createWindowAction) async {
    if (createWindowAction.request.url != null) {
      await launchUrl(createWindowAction.request.url!);
      return true;
    }
    return super.onCreateWindow(createWindowAction);
  }

  @override
  Future onLoadStart(url) async {
    debugPrint("On load start -> $url");
    _handlerService.register(
      PlatformWebViewController(mobileWebviewController: webViewController),
      socketInfo: dataPayload.payload.data.socket,
    );
  }

  @override
  Future onLoadStop(url) async {
    pullToRefreshController?.endRefreshing();
  }

  @override
  Future<PermissionResponse> onPermissionRequest(permissionRequest) async {
    return PermissionResponse(
      resources: permissionRequest.resources,
      action: PermissionResponseAction.GRANT,
    );
  }

  @override
  void onLoadError(url, code, message) {
    pullToRefreshController?.endRefreshing();
  }

  @override
  void onProgressChanged(progress) {
    if (progress == 100) {
      pullToRefreshController?.endRefreshing();
    }
  }

  @override
  void onConsoleMessage(ConsoleMessage consoleMessage) {
    debugPrint("Console -> $consoleMessage");
    super.onConsoleMessage(consoleMessage);
  }

  @override
  void onExit() {
    sl<EztoResultService>().onClose(CloseReason.byUser);
  }

  @override
  Future<NavigationActionPolicy> shouldOverrideUrlLoading(
      navigationAction) async {
    return NavigationActionPolicy.ALLOW;
  }

  @override
  void onDownloadStartRequest(DownloadStartRequest downloadStartRequest) async {
    // if (downloadStartRequest.mimeType != "application/pdf") {
    //   return;
    // }
    // final mimeType = downloadStartRequest.mimeType!;
    // final fileName = downloadStartRequest.suggestedFilename ?? "terms";
    // sl<EztoResultService>().onFileDownloadStart(fileName, mimeType);
    // final path = await sl<FileDownloadService>().download(
    //   url: downloadStartRequest.url.toString(),
    //   fileName: fileName,
    //   mimeType: mimeType,
    // );
    // sl<EztoResultService>().onFileDownloadEnd(
    //   fileName,
    //   mimeType,
    //   path,
    // );
  }
}
