import 'package:flutter_inappwebview/flutter_inappwebview.dart';
import 'package:webview_windows/webview_windows.dart';

abstract class GenericWebviewController {
  WebviewController? windowsWebviewController;
  InAppWebViewController? mobileWebviewController;

  Future<void> loadUrl(String url);
}
