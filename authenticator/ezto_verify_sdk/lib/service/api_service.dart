import 'dart:convert';
import 'dart:io';

import 'package:flutter/foundation.dart';
import 'package:http/http.dart' as http;
import 'package:http/retry.dart';

enum ConnectionStatus {
  started(201),
  completed(200),
  connectionReset(205),
  userClosed(403);

  final int value;
  const ConnectionStatus(this.value);
}

class ApiService {
  Future<Map<String, dynamic>> loadSekuraSessionUrl(String url) async {
    final request = await http.get(Uri.parse(url));
    return {
      "status_code": request.statusCode,
      "data": request.body,
    };
  }

  Future<bool> sendConnectionStatus({
    required ConnectionStatus status,
    required String? socketUrl,
    required String? requestId,
    int tryCount = 0,
  }) async {
    if (socketUrl == null) return false;
    if (requestId == null) return false;
    try {
      final url = '$socketUrl/status?key=$requestId';
      final retryClient = RetryClient(http.Client());
      final response = await retryClient.post(
        Uri.parse(url),
        headers: <String, String>{
          'Content-Type': 'application/json',
        },
        body: jsonEncode({'status': '${status.value}'}),
      );
      debugPrint("status code: ${response.statusCode}");
      return (response.statusCode == 200);
    } catch (e) {
      return false;
    }
  }

  Future<Map<String, dynamic>?> getPayloadFromId({
    required String id,
    required String baseUrl,
    required String actionToken,
    required String realmId,
  }) async {
    final url = "$baseUrl/$id/$realmId";
    debugPrint("url : $url , action token : $actionToken");
    final response = await http.get(
      Uri.parse(url),
      headers: <String, String>{
        'Content-Type': 'application/json',
        'Authorization': 'Bearer $actionToken',
      },
    );
    debugPrint("status -> ${response.statusCode}");
    if (response.statusCode == 200) {
      debugPrint("response -> ${response.body}");
      if (Platform.isAndroid || Platform.isIOS) {
        final json = jsonEncode({"payload": response.body});
        final result = jsonDecode(json);
        return result;
      } else {
        final result = jsonDecode(response.body);
        return result;
      }
    } else {
      return null;
    }
  }
}
