import 'dart:convert';
import 'dart:io';

import 'package:android_play_install_referrer/android_play_install_referrer.dart';
import 'package:app_links/app_links.dart';
import 'package:eztoverify/enums/error_code.dart';
import 'package:eztoverify/service/api_service.dart';
import 'package:eztoverify/service/ezto_result_service.dart';
import 'package:eztoverify/service/service_locator.dart';
import 'package:eztoverify/service/storage_service.dart';
import 'package:eztoverify/service/window_manager_service.dart';
import 'package:eztoverify/handlers/registry_platform_handler.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

class AppLinksService {
  AppLinks appLinks = AppLinks();

  Future<void> init() async {
    if (Platform.isWindows && kDebugMode) {
      Registry().registerForWindows("eztovrfy");
    }
    // Check initial link if app was in cold state (terminated)
    _handleInitialLink();

    // Handle link when app is in warm state (front or background)
    _handleWarmStateLink();
  }

  void _handleInitialLink() async {
    final appLink = await appLinks.getInitialLink();
    if (appLink == null) {
      if (Platform.isMacOS) {
        final clipboardData = await Clipboard.getData("text/plain");
        if (clipboardData == null || clipboardData.text == null) return;
        final url = clipboardData.text!;
        if (url.startsWith('eztovrfy')) {
          _handleAppLink(Uri.parse(url));
          await Clipboard.setData(const ClipboardData(text: ""));
        }
      } else {
        final isFirstRun = await sl<StorageService>().isFirstRun();
        debugPrint("isFirstRun $isFirstRun");
        if (isFirstRun) {
          try {
            if (Platform.isAndroid) {
              //Use play referrer api
              ReferrerDetails referrerDetails =
                  await AndroidPlayInstallReferrer.installReferrer;
              if (referrerDetails.installReferrer == null) return;
              // https://play.google.com/store/apps/details?id=com.example.app
              // &referrer=utm_source%3Dgoogle
              // %26utm_medium%3Dcpc
              // %26utm_term%3Drunning%252Bshoes
              // %26utm_content%3DdisplayAd1
              // %26utm_campaign%3Dshoe%252Bcampaign
              debugPrint(
                "Referrer details -> ${referrerDetails.installReferrer}",
              );
              final uri = Uri.parse(
                  "https://ezto.io?${referrerDetails.installReferrer}");
              final source = uri.queryParameters["utm_source"];
              if (source == null) return;
              final decodedUrl = Uri.decodeComponent(source);
              _handleAppLink(Uri.parse(decodedUrl));
            } else {
              //Get Data from clipboard
              final clipboardData = await Clipboard.getData("text/plain");
              if (clipboardData == null || clipboardData.text == null) return;
              final url = clipboardData.text!;
              _handleAppLink(Uri.parse(url));
            }
          } catch (e) {
            debugPrint("Deeplink failed");
          }
        }
      }
    } else {
      _handleAppLink(appLink);
    }
  }

  void _handleWarmStateLink() {
    appLinks.uriLinkStream.listen((uri) {
      _handleAppLink(uri);
    });
  }

  void _handleAppLink(Uri? uri) async {
    debugPrint("Deeplink -> $uri");

    if (uri == null) return;
    if (Platform.isMacOS || Platform.isWindows || Platform.isLinux) {
      await sl<WindowManagerService>().bringToFront();
      if (uri.scheme == 'eztovrfy') {
        final checkUrl = _checkDeeplinkUrl(uri: uri);
        if (checkUrl) {
          _getPayload(uri: uri);
          return;
        }
        final data = uri.queryParameters['data'];
        if (data != null) {
          try {
            final decodedString = utf8.decode(base64.decode(data));
            final jsonData = json.decode(decodedString);
            sl<EztoResultService>().onTransactionRequest(jsonData, true);
          } catch (e) {
            debugPrint("invalid deeplink");
          }
        }
      }
    } else {
      if (uri.scheme == 'https') {
        final checkUrl = _checkDeeplinkUrl(uri: uri);
        debugPrint("check url -> $checkUrl");
        if (checkUrl) {
          _getPayload(uri: uri);
        }
      }
    }
  }

  bool _checkDeeplinkUrl({required Uri uri}) {
    if (uri.queryParameters.containsKey("id") &&
        uri.queryParameters.containsKey("base_url") &&
        uri.queryParameters.containsKey("action_token") &&
        uri.queryParameters.containsKey("realm_id")) {
      return true;
    }
    return false;
  }

  Future<void> _getPayload({
    required Uri uri,
  }) async {
    String id = uri.queryParameters["id"] ?? "";
    String baseUrl = uri.queryParameters["base_url"] ?? "";
    String actionToken = uri.queryParameters["action_token"] ?? "";
    String realmId = uri.queryParameters["realm_id"] ?? "";
    final payload = await ApiService().getPayloadFromId(
      id: id,
      baseUrl: baseUrl,
      actionToken: actionToken,
      realmId: realmId,
    );
    if (payload != null) {
      sl<EztoResultService>().onTransactionRequest(payload, true);
    } else {
      sl<EztoResultService>().onError(ErrorCode.invalidPayload);
    }
  }
}
