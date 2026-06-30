import 'dart:convert';
import 'dart:io';
import 'package:googleapis_auth/auth_io.dart';
import 'package:http/http.dart' as http;

class FirebaseService {
  Future<bool> createFirebaseApp({
    required String bundleId,
    required String appName,
  }) async {
    String accessToken = await _getAccessToken();
    print("accessToken : $accessToken");

    final url =
        'https://firebase.googleapis.com/v1beta1/projects/eztoverify/iosApps';
    final response = await http.post(
      Uri.parse(url),
      headers: {
        'Authorization': 'Bearer $accessToken',
        'Content-Type': 'application/json',
      },
      body: json.encode({
        'displayName': appName,
        'bundleId': bundleId,
      }),
    );

    if (response.statusCode == 200) {
      final appId = await _findAppIdFromBundleId(bundleId, accessToken);
      if (appId != null) {
        await _downloadGoogleServiceInfoPlist(appId, accessToken);
      }
      print('Firebase App created successfully: ${response.body}');
      return true;
    } else if (response.statusCode == 409) {
      final appId = await _findAppIdFromBundleId(bundleId, accessToken);
      if (appId != null) {
        await _downloadGoogleServiceInfoPlist(appId, accessToken);
      }
      print(
          'Failed to create Firebase App: ${response.statusCode} ${response.body}');
      return false;
    } else {
      print(
          'Failed to create Firebase App: ${response.statusCode} ${response.body}');
      return false;
    }
  }

  Future<String> _getAccessToken() async {
    final filePath = './lib/service/firebase-admin.json';

    final file = File(filePath);
    final jsonString = await file.readAsString();

    // Parse the JSON
    final serviceAccountJson = json.decode(jsonString);

    var accountCredentials =
        ServiceAccountCredentials.fromJson(serviceAccountJson);

    var scopes = [
      'https://www.googleapis.com/auth/firebase',
      'https://www.googleapis.com/auth/cloud-platform',
    ];

    var authClient = await clientViaServiceAccount(accountCredentials, scopes);
    var accessToken = (authClient.credentials).accessToken.data;
    authClient.close();
    return accessToken;
  }

  Future<String?> _findAppIdFromBundleId(
    String bundleId,
    String accessToken,
  ) async {
    final url =
        'https://firebase.googleapis.com/v1beta1/projects/eztoverify/iosApps';
    final response = await http.get(
      Uri.parse(url),
      headers: {
        'Authorization': 'Bearer $accessToken',
        'Content-Type': 'application/json',
      },
    );

    if (response.statusCode == 200) {
      var data = jsonDecode(response.body);
      print("data : $data");
      var iosApps = data['apps'] as List;

      for (var app in iosApps) {
        if (app['bundleId'] == bundleId) {
          print('App ID: ${app['appId']}');
          final appId = app['appId'];
          return appId;
        }
      }
      print('No app found with Bundle ID: $bundleId');
      return null;
    } else {
      print('Failed to list iOS apps: ${response.statusCode} ${response.body}');
      return null;
    }
  }

  Future<void> _downloadGoogleServiceInfoPlist(
    String appId,
    String accessToken,
  ) async {
    final url =
        'https://firebase.googleapis.com/v1beta1/projects/eztoverify/iosApps/$appId/config';
    final response = await http.get(
      Uri.parse(url),
      headers: {
        'Authorization': 'Bearer $accessToken',
        'Content-Type': 'application/json',
      },
    );

    if (response.statusCode == 200) {
      final decodedBytes =
          base64.decode(jsonDecode(response.body)['configFileContents']);

      final file = File("../ios/Runner/GoogleService-Info.plist");
      await file.writeAsBytes(decodedBytes);

      print('GoogleService-Info.plist downloaded and saved successfully.');
    } else {
      print(
          'Failed to download GoogleService-Info.plist: ${response.statusCode} ${response.body}');
    }
  }
}
