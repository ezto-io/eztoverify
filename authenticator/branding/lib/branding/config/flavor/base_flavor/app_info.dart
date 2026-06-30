import 'package:branding/branding/config/base/base_app_info.dart';
import 'package:branding/models/encryption_model.dart';
import 'package:branding/models/url_model.dart';

class Appinfo extends BaseAppInfo {
  @override
  String get appName => "{ezto_flavor_app_name}";

  @override
  String get androidBundleId => "{ezto_flavor_android_bundle_id}";

  @override
  String get iosCiCdBundleId => "{ezto_flavor_ios_cicd_bundle_id}";

  @override
  String get iosBundleId => "{ezto_flavor_ios_bundle_id}";

  @override
  String get macosBundleId => "{ezto_flavor_macos_bundle_id}";

  @override
  String get windowsBundleId => "{ezto_flavor_windows_bundle_id}";

  @override
  bool get isCopyAuthCode => "{ezto_flavor_isCopyAuthCode}" as bool;

  @override
  bool get isProd => "{ezto_flavor_isProd}" as bool;

  @override
  bool get isIosCiCd => "{ezto_flavor_isIosCiCd}" as bool;

  @override
  UrlModel get urls => UrlModel(
        terms: "{ezto_flavor_terms}",
        privacy: "{ezto_flavor_privacy}",
        macUpdate: 'https://your-update-server/application/your-app-id',
        windowsUpdate: 'https://your-update-server/application/your-app-id',
      );

  @override
  EncryptionModel get encryption => EncryptionModel(
        mobileKey: "{MOBILE_KEY}",
        desktopKey: "{DESKTOP_KEY}",
      );
}
