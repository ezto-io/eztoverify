import 'package:branding/branding/config/base/base_app_info.dart';
import 'package:branding/models/encryption_model.dart';
import 'package:branding/models/url_model.dart';

class Appinfo extends BaseAppInfo {
  @override
  String get appName => "ezto verify dev";

  @override
  String get androidBundleId => "io.ezto.verify.dev";

  @override
  String get iosCiCdBundleId => "io.ezto.verify.dev";

  @override
  String get iosBundleId => "io.ezto.verify.dev";

  @override
  String get macosBundleId => "io.ezto.verify.dev";

  @override
  String get windowsBundleId => "io.ezto.verify.dev";

  @override
  bool get isCopyAuthCode => true;

  @override
  bool get isProd => false;

  @override
  bool get isAutoTest => true;

  @override
  bool get isIosCiCd => false;

  @override
  UrlModel get urls => UrlModel(
        terms: "https://ezto.io/verify/terms-and-conditions",
        privacy: "https://ezto.io/verify/privacy-policy",
        macUpdate: '{app_update_url_mac}',
        windowsUpdate: '{app_update_url_window}',
      );

  @override
  EncryptionModel get encryption => EncryptionModel(
        mobileKey: "{MOBILE_KEY}",
        desktopKey: "{DESKTOP_KEY}",
      );
}
