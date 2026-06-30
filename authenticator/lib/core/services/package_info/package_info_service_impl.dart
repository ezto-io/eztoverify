import 'package:eztoverify_getting_started/core/services/package_info/package_info_service.dart';
import 'package:package_info_plus/package_info_plus.dart';

class PackageInfoServiceImpl implements PackageInfoServices {
  @override
  Future<PackageInfo> getAppInfo() async {
    PackageInfo packageInfo = await PackageInfo.fromPlatform();
    return packageInfo;
  }
}
