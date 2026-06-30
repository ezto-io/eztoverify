import 'package:eztoverify/enums/push_support.dart';
import 'package:eztoverify/model/get_token_model.dart';
import 'package:eztoverify_internal/service/push_service_interface.dart';

class WebService implements PushServiceInterface {
  @override
  void checkNotificationDataOnLaunch() {}

  @override
  Future<GetTokenModel> getToken() async {
    return GetTokenModel(pushToken: "", pushType: PushSupport.socket);
  }

  @override
  Future<void> init() async {}

  @override
  void showNotification(
      {required int id,
      required Map<String, dynamic> payload,
      required String? title,
      required String? body}) {}

  @override
  void showTransactionFromDeeplink(Map<String, dynamic> data) {}
}
