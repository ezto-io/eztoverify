import 'package:branding/branding/config/base/base_app_info.dart';
import 'package:branding/branding/config/base/base_app_signing.dart';
import 'package:branding/branding/config/base/base_deeplink.dart';
import 'package:branding/branding/config/base/base_permission.dart';
import 'package:branding/branding/config/base/base_theme.dart';

class FlavorModel {
  final BasePermission permissions;
  final BaseDeeplink deeplinks;
  final BaseAppInfo appInfo;
  final String flavorName;
  final BaseTheme theme;
  final BaseAppSigning appSigning;

  FlavorModel({
    required this.permissions,
    required this.deeplinks,
    required this.appInfo,
    required this.flavorName,
    required this.theme,
    required this.appSigning,
  });
}
