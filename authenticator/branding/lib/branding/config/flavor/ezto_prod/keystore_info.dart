import 'package:branding/branding/config/base/base_keystore_info.dart';

class KeystoreInfo extends BaseKeystoreInfo {
  @override
  String get keyAlias => "{keystore_key_alias}";

  @override
  String get keyPassword => "{keystore_key_password}";

  @override
  String get storePassword => "{keystore_store_password}";
}
