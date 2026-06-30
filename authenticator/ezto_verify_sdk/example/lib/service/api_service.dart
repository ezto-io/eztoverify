import 'dart:convert';

import 'package:http/http.dart' as http;

class ApiService {
  static Future<String> sendToken(
      {required String token, required String device}) async {
    const url = '<<YOUR_WEBHOOK_URL>>'; // Replace with your actual webhook URL

    try {
      final response = await http.post(
        Uri.parse(url),
        headers: <String, String>{
          'Content-Type': 'application/json',
        },
        body: jsonEncode({'text': 'device : $device \n token : $token'}),
      );
      if (response.statusCode == 200) {
        return "Success!";
      } else {
        return "Failed!";
      }
    } catch (e) {
      return "Something went wrong!";
    }
  }
}
