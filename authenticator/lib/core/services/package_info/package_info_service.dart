import 'package:package_info_plus/package_info_plus.dart';

abstract interface class PackageInfoServices {
  Future<PackageInfo> getAppInfo();
}
