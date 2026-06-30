import 'package:branding/models/encryption_model.dart';
import 'package:branding/models/url_model.dart';

abstract class BaseAppInfo {
  abstract final String appName;
  abstract final String androidBundleId;
  abstract final String iosCiCdBundleId;
  abstract final String iosBundleId;
  abstract final String macosBundleId;
  abstract final String windowsBundleId;
  abstract final bool isProd;
  abstract final bool isIosCiCd;
  abstract final bool isCopyAuthCode;
  abstract final UrlModel urls;
  abstract final EncryptionModel encryption;
  bool isAutoTest = false;

  @override
  String toString() {
    return 'BaseAppInfo(appName: $appName, androidBundleId: $androidBundleId, iosDevBundleId: $iosCiCdBundleId, iosProdBundleId: $iosBundleId, macosBundleId: $macosBundleId, windowsBundleId: $windowsBundleId, isProd: $isProd, isIosCiCd: $isIosCiCd, isCopyAuthCode: $isCopyAuthCode, urls: $urls, encryption: $encryption, isAutoTest : $isAutoTest)';
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;

    return other is BaseAppInfo &&
        other.appName == appName &&
        other.androidBundleId == androidBundleId &&
        other.isProd == isProd &&
        other.isCopyAuthCode == isCopyAuthCode &&
        other.urls == urls &&
        other.encryption == encryption;
  }

  @override
  int get hashCode {
    return appName.hashCode ^
        androidBundleId.hashCode ^
        isProd.hashCode ^
        isCopyAuthCode.hashCode ^
        urls.hashCode ^
        encryption.hashCode;
  }
}
