import 'package:eztoverify/interface/js_handler.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:connectivity_plus/connectivity_plus.dart';
import 'package:ezto_http_client/ezto_http_client.dart';
import 'package:url_launcher/url_launcher.dart';

class SystemServiceHandler extends JsHandler {
  Future<Map<String, dynamic>> copyToClipboard(List<dynamic> args) async {
    final data = processArgs(args);
    await Clipboard.setData(ClipboardData(text: data["text"] ?? ""));
    return {
      "success": true,
      "message": "",
    };
  }

  Future<Map<String, dynamic>> getCurrentNetwork(List<dynamic> args) async {
    final List<ConnectivityResult> connectivityResult =
        await (Connectivity().checkConnectivity());
    debugPrint(connectivityResult.toString());
    var connection = "other";
    if (connectivityResult.contains(ConnectivityResult.wifi)) {
      connection = ConnectivityResult.wifi.name;
    } else if (connectivityResult.contains(ConnectivityResult.vpn)) {
      connection = ConnectivityResult.vpn.name;
    } else if (connectivityResult.contains(ConnectivityResult.mobile)) {
      connection = ConnectivityResult.mobile.name;
    }
    return {
      "success": true,
      "connectivity": connection,
      "device": (await getDeviceInfo()),
    };
  }

  Future<Map<String, dynamic>> isCustomSchemeRegisteredOnApp(
      List<dynamic> args) async {
    final data = processArgs(args);
    final scheme = data["scheme"];
    final host = data["host"];

    final isRegistered = await canLaunchUrl(Uri(scheme: scheme, host: host));

    return {
      "isRegistered": isRegistered,
      "success": true,
      "message": "Process completed successfully"
    };
  }

  Future<Map<String, dynamic>> loadSekuraSessionUrl(List<dynamic> args) async {
    final data = processArgs(args);
    final url = data["url"];
    try {
      final client = HttpNativeClient();
      final response = await client.getUrl(url);
      return {
        "success": true,
        "message": "",
        "result": {
          "status_code": response.statusCode,
          "body": response.body,
        },
        "device": await getDeviceInfo(),
      };
    } catch (e) {
      return {
        "success": false,
        "message": "Request Failed",
        "device": await getDeviceInfo(),
      };
    }
  }
}
