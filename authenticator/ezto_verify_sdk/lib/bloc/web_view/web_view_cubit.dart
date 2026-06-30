import 'dart:io';

import 'package:eztoverify/enums/error_code.dart';
import 'package:eztoverify/service/platform_webview_controller.dart';
import 'package:eztoverify/service/windows_js_handler_service.dart';
import 'package:eztoverify/service/windows_permission_handler.dart';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:flutter_inappwebview/flutter_inappwebview.dart';
import 'package:eztoverify/enums/status.dart';
import 'package:eztoverify/model/payload_data_model.dart';
import 'package:eztoverify/service/api_service.dart';
import 'package:eztoverify/service/custom_permission_service.dart';
import 'package:eztoverify/service/service_locator.dart';
import 'package:eztoverify/service/websocket_service.dart';
import 'package:url_launcher/url_launcher.dart';
import 'package:webview_windows/webview_windows.dart';

part 'web_view_state.dart';

class WebViewCubit extends Cubit<WebViewStates> {
  WebViewCubit() : super(WebViewInitial());

  Future<PermissionResponse> onPermissionRequest({
    required PermissionRequest request,
  }) async {
    if (Platform.isMacOS) {
      return PermissionResponse(
        resources: request.resources,
        action: PermissionResponseAction.GRANT,
      );
    }
    final List<Status> failedResults = await sl<CustomPermissionService>()
        .requestPermissionFromWebRequest(request);
    debugPrint("on permission failedResults -> $failedResults");  
    debugPrint("on permission request resources -> ${request.resources}");   
    return PermissionResponse(
      resources: request.resources,
      action: failedResults.isEmpty
          ? PermissionResponseAction.GRANT
          : PermissionResponseAction.DENY,
    );
  }

  Future<WebviewPermissionDecision> onWindowsPermissionRequest(
      {required WebviewPermissionKind request}) async {
    final List<Status> failedResults = await sl<WindowsPermissionService>()
        .requestPermissionFromWebRequest(request);
    return failedResults.isEmpty
        ? WebviewPermissionDecision.allow
        : WebviewPermissionDecision.deny;
  }

  NavigationActionPolicy shouldOverrideUrlLoading(
      NavigationAction navigationAction) {
    var uri = navigationAction.request.url!;
    if (!["http", "https", "file", "chrome", "data", "javascript", "about"]
        .contains(uri.scheme)) {
      return NavigationActionPolicy.CANCEL;
    }
    List<String> splitUrl = uri.toString().split('/');
    if (splitUrl.contains('logged-in-successfully')) {
      onSuccess();
      return NavigationActionPolicy.CANCEL;
    }
    return NavigationActionPolicy.ALLOW;
  }

  void onLoading() {
    emit(WebViewLoading());
  }

  void onLoaded() {
    emit(WebViewLoaded());
  }

  void onSuccess() {
    emit(WebViewSuccess());
  }

  void onSslError() {
    emit(WebViewFailed(errorCode: ErrorCode.socketConnectionError));
  }

  void onInternetError() {
    emit(WebViewFailed(errorCode: ErrorCode.internetConnectionError));
  }

  void setupWebview(DataPayload dataPayload) async {
    emit(WebViewInitial());
    final isSocketSuccess =
        await sl<WebSocketService>().tryConnect(dataPayload);
    if (!isSocketSuccess) {
      emit(WebViewFailed(errorCode: ErrorCode.socketConnectionError));
    } else {
      await sl<ApiService>().sendConnectionStatus(
        status: ConnectionStatus.started,
        socketUrl: dataPayload.payload.data.socket.url,
        requestId: dataPayload.payload.data.socket.requestId,
      );
      emit(WebViewLoading());
    }
  }

  void setupWindowsWebview({
    required DataPayload dataPayload,
    required WebviewController controller,
    required WindowsJavaScriptHandlerService jsHandler,
  }) async {
    emit(WebViewInitial());
    final isSocketSuccess =
        await sl<WebSocketService>().tryConnect(dataPayload);
    if (!isSocketSuccess) {
      emit(WebViewFailed(errorCode: ErrorCode.socketConnectionError));
    } else {
      await sl<ApiService>().sendConnectionStatus(
        status: ConnectionStatus.started,
        socketUrl: dataPayload.payload.data.socket.url,
        requestId: dataPayload.payload.data.socket.requestId,
      );
      String? checkWebview = await WebviewController.getWebViewVersion();
      if (checkWebview == null) {
        installWebView2();
      }
      await controller.initialize();
      await controller.clearCache();
      await controller.clearCookies();
      await controller.setBackgroundColor(Colors.transparent);
      await controller.loadUrl(dataPayload.payload.data.action);
      emit(WebViewLoading());
      controller
        ..url.listen((event) async {
          if (event.contains(".pdf")) {
            controller.goBack();
            final url = Uri.parse(event);
            if (await canLaunchUrl(url)) {
              await launchUrl(url);
            }
            // List<String> parts = event.split("/");
            // String fileName = parts.last;
            // sl<EztoResultService>()
            //     .onFileDownloadStart(fileName, "application/pdf");
            // final path = await sl<FileDownloadService>().download(
            //   url: event,
            //   fileName: fileName,
            //   mimeType: "application/pdf",
            // );
            // sl<EztoResultService>().onFileDownloadEnd(
            //   fileName,
            //   "application/pdf",
            //   path,
            // );
          }
        })
        ..addScriptToExecuteOnDocumentCreated("script")
        ..onLoadError.listen((event) {
          onInternetError();
        })
        ..loadingState.listen((event) {
          onLoading();
          if (event == LoadingState.navigationCompleted) {
            onLoaded();
          }
        })
        ..webMessage.listen((event) {
          jsHandler.register(
            event,
            PlatformWebViewController(
              windowsWebviewController: controller,
            ),
            socketInfo: dataPayload.payload.data.socket,
          );
        })
        ..setPopupWindowPolicy(WebviewPopupWindowPolicy.allow);
    }
  }

  void windowsWebviewDispose(WebviewController controller) {
    controller.dispose();
  }

  void installWebView2() async {
    await Process.run(
        'webviewAsset/WebView2Runtime.exe', ['/silent', '/install']);
  }
}
