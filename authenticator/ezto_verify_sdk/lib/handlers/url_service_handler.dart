import 'package:eztoverify/interface/js_handler.dart';
import 'package:eztoverify/service/platform_webview_controller.dart';

class UrlServiceHandler extends JsHandler {
  Future<Map<String, dynamic>> loadUrl(
    List<dynamic> args,
    PlatformWebViewController controller,
  ) async {
    final params = processArgs(args);
    final url = params['url'];
    await controller.loadUrl(url);
    return {
      "success": true,
      "message": "",
    };
  }
}
