import 'dart:convert';
import 'dart:io';

import 'package:eztoverify/enums/close_reason.dart';
import 'package:eztoverify/enums/error_code.dart';
import 'package:eztoverify/screen/windows_webview_screen.dart';
import 'package:eztoverify/service/ezto_result_service.dart';
import 'package:eztoverify/service/file_service.dart';
import 'package:eztoverify/service/navigation_service.dart';
import 'package:eztoverify/service/websocket_service.dart';
import 'package:eztoverify/service/window_manager_service.dart';
import 'package:flutter/foundation.dart';
import 'package:eztoverify/exceptions/encryption_key_exception.dart';
import 'package:eztoverify/model/payload_data_model.dart';
import 'package:eztoverify/model/qr_scanner_result.dart';
import 'package:eztoverify/screen/qr_scanner_screen.dart';
import 'package:eztoverify/screen/web_view_screen.dart';
import 'package:eztoverify/service/service_locator.dart';
import 'package:eztoverify/widget/qv_bottom_sheet.dart';
import 'package:mobile_scanner/mobile_scanner.dart';

class EztoVerify {
  final String? encryptionKey;
  EztoVerify({
    this.encryptionKey,
  });

  void onPushReceived({
    required Map<String, dynamic> data,
  }) async {
    debugPrint("-------Debug pring data -> $data");
    await sl<WindowManagerService>().bringToFront();
    sl<WebSocketService>().dispose();
    try {
      bool isPopped = sl<NavigationService>().popAll();
      if (isPopped) {
        sl<EztoResultService>().onClose(CloseReason.newTransaction);
      }
      final dataPayload = DataPayload.fromJson(
        json: data,
        encryptionKey: encryptionKey,
      );
      if (dataPayload.payload.id == "qv_auth") {
        if (Platform.isWindows) {
          await showQvBottomSheet(
              eztoNavigatorKey.currentContext!,
              WindowsWebViewScreen(
                pushDataModel: dataPayload,
              ));
        } else if (Platform.isLinux) {
          await showLinuxBrowser(dataPayload: dataPayload);
        } else {
          await showQvBottomSheet(
              eztoNavigatorKey.currentContext!,
              WebViewScreen(
                pushDataModel: dataPayload,
              ));
        }
      } else {
        sl<EztoResultService>().onError(ErrorCode.invalidPayload);
      }
    } catch (e) {
      debugPrint(e.toString());
      if (e is EncryptionKeyException) {
        sl<EztoResultService>().onError(ErrorCode.missingEncryptionKey);
      } else {
        sl<EztoResultService>().onError(ErrorCode.invalidPayload);
      }
    }
  }

  void qrFromBytes({
    required Uint8List bytes,
  }) async {
    if (Platform.isAndroid || Platform.isIOS) {
      final path = await sl<FileService>().saveImage(bytes);
      if (path == null) {
        sl<EztoResultService>().onError(ErrorCode.invalidPayload);
        return;
      } else {
        final scanner = MobileScannerController();
        final capture = await scanner.analyzeImage(path);
        final barcode = capture?.barcodes.firstOrNull?.rawValue;
        if (barcode == null) {
          sl<EztoResultService>().onError(ErrorCode.invalidPayload);
          return;
        } else {
          String json = jsonEncode({"payload": barcode});
          Map<String, dynamic> jsonResult = jsonDecode(json);
          onPushReceived(data: jsonResult);
        }
      }
    } else {
      throw UnsupportedError("Platform not supported");
    }
  }

  void scanQr() async {
    try {
      QrScannerResult? result = await showQvBottomSheet<QrScannerResult?>(
        eztoNavigatorKey.currentContext!,
        QrScannerScreen(
          encryptionKey: encryptionKey,
        ),
      );
      if (result == null) {
        return;
      }
      if (result.status == QrStatus.success && result.data != null) {
        onPushReceived(data: result.data!);
      }
    } catch (e) {
      debugPrint(e.toString());
      sl<EztoResultService>().onError(ErrorCode.invalidPayload);
    }
  }
}
