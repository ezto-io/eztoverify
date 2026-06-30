import 'package:eztoverify/enums/close_reason.dart';
import 'package:eztoverify/enums/error_code.dart';

abstract class ResultInterface {
  void onClose(CloseReason reason);
  void onError(ErrorCode error);
  void onResult(String requestId, String authReqId);
  void onTransactionRequest(Map<String, dynamic> data, bool isFromDeeplink);
  void onFileDownloadStart(
    String fileName,
    String mimeType,
  );
  void onFileDownloadEnd(
    String fileName,
    String mimeType,
    String path,
  );

  Future<String> onSocketPushUrlRequest();
}
