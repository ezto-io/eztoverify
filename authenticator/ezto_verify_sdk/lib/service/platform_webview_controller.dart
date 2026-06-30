import 'package:eztoverify/interface/generic_webview_controller.dart';
import 'package:flutter_inappwebview/flutter_inappwebview.dart';
import 'package:webview_windows/webview_windows.dart';

class PlatformWebViewController implements GenericWebviewController {
  @override
  InAppWebViewController? mobileWebviewController;

  @override
  WebviewController? windowsWebviewController;

  PlatformWebViewController({
    this.mobileWebviewController,
    this.windowsWebviewController,
  });

  @override
  Future<void> loadUrl(String url) async {
    await mobileWebviewController?.loadUrl(
        urlRequest: URLRequest(url: WebUri(url)));
    await windowsWebviewController?.loadUrl(url);
  }
}
