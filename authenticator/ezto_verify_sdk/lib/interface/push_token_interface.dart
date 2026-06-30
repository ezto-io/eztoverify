import 'package:eztoverify/model/get_token_model.dart';

abstract class PushTokenInterface {
  Future<GetTokenModel> getToken();
}
