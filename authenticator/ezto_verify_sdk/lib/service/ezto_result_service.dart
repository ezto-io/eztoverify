import 'package:eztoverify/enums/close_reason.dart';
import 'package:eztoverify/enums/error_code.dart';
import 'package:eztoverify/interface/result_interface.dart';

class EztoResultService implements ResultInterface {
  final ResultInterface handler;

  EztoResultService({
    required this.handler,
  });

  @override
  void onClose(CloseReason reason) {
    handler.onClose(reason);
  }

  @override
  void onError(ErrorCode error) {
    handler.onError(error);
  }

  @override
  void onResult(String requestId, String authReqId) {
    handler.onResult(requestId, authReqId);
  }

  @override
  void onTransactionRequest(Map<String, dynamic> data, bool isFromDeeplink) {
    handler.onTransactionRequest(data, isFromDeeplink);
  }

  @override
  Future<String> onSocketPushUrlRequest() {
    return handler.onSocketPushUrlRequest();
  }

  @override
  void onFileDownloadEnd(String fileName, String mimeType, String path) {
    handler.onFileDownloadEnd(fileName, mimeType, path);
  }

  @override
  void onFileDownloadStart(String fileName, String mimeType) {
    handler.onFileDownloadStart(fileName, mimeType);
  }
}
