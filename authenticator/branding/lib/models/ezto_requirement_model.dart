class EztoRequirementModel {
  String appName;
  String flavorName;
  String? androidAppBundleId;
  String? iosAppBundleId;
  String? macosAppBundleId;
  String? windowsAppBundleId;
  List<String> supportPlatforms;
  String? termsAndCondition;
  String? privacyPolicy;

  String? androidFirebaseJson;
  String? androidKeystoreFile;
  String? androidKeyAlias;
  String? androidKeyPassword;
  String? androidStorePassword;

  String primaryColor;
  String fontName;
  String appTransparentLogo;
  String appFullLogo;
  String appLogoSimple;
  String appLogoWhite;

  EztoRequirementModel({
    required this.appName,
    required this.flavorName,
    required this.supportPlatforms,
    this.androidAppBundleId,
    this.iosAppBundleId,
    this.macosAppBundleId,
    this.windowsAppBundleId,
    this.termsAndCondition,
    this.privacyPolicy,
    this.androidFirebaseJson,
    this.androidKeystoreFile,
    this.androidKeyAlias,
    this.androidKeyPassword,
    this.androidStorePassword,
    required this.primaryColor,
    required this.fontName,
    required this.appTransparentLogo,
    required this.appFullLogo,
    required this.appLogoSimple,
    required this.appLogoWhite,
  });

  factory EztoRequirementModel.fromJson(Map<String, dynamic> json) {
    return EztoRequirementModel(
      appName: json['app_name'],
      flavorName: json['flavor_name'],
      androidAppBundleId: json['android_app_bundle_id'],
      iosAppBundleId: json['ios_app_bundle_id'],
      macosAppBundleId: json['macos_app_bundle_id'],
      windowsAppBundleId: json['windows_app_bundle_id'],
      supportPlatforms: json['support_platforms'].cast<String>(),
      primaryColor: json['primary_color'],
      fontName: json['font_theme'],
      androidFirebaseJson: json['android_firebase_json'],
      androidKeystoreFile: json['android_keystore_file'],
      androidKeyAlias: json['android_key_alias'],
      androidKeyPassword: json['android_key_password'],
      androidStorePassword: json['android_store_password'],
      termsAndCondition: json['terms_and_condition'],
      privacyPolicy: json['privacy_policy'],
      appTransparentLogo: json['app_transparent_logo'],
      appFullLogo: json['app_full_logo'],
      appLogoSimple: json['app_logo_simple'],
      appLogoWhite: json['app_logo_white'],
    );
  }

  Map<String, dynamic> toJson() {
    final Map<String, dynamic> data = <String, dynamic>{};
    data['app_name'] = appName;
    data['flavor_name'] = flavorName;
    data['android_app_bundle_id'] = androidAppBundleId;
    data['ios_app_bundle_id'] = iosAppBundleId;
    data['macos_app_bundle_id'] = macosAppBundleId;
    data['windows_app_bundle_id'] = windowsAppBundleId;
    data['support_platforms'] = supportPlatforms;
    data['primary_color'] = primaryColor;
    data['font_theme'] = fontName;

    data['android_firebase_json'] = androidFirebaseJson;
    data['android_keystore_file'] = androidKeystoreFile;
    data['android_key_alias'] = androidKeyAlias;
    data['android_key_password'] = androidKeyPassword;
    data['android_store_password'] = androidStorePassword;

    data['terms_and_condition'] = termsAndCondition;
    data['privacy_policy'] = privacyPolicy;
    data['app_transparent_logo'] = appTransparentLogo;
    data['app_full_logo'] = appFullLogo;
    data['app_logo_simple'] = appLogoSimple;
    data['app_logo_white'] = appLogoWhite;

    return data;
  }
}
