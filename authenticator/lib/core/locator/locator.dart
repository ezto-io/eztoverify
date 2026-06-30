import 'package:device_info_plus/device_info_plus.dart';
import 'package:eztoverify_getting_started/core/services/device_info/device_info_service.dart';
import 'package:eztoverify_getting_started/core/services/device_info/device_info_service_impl.dart';
import 'package:eztoverify_getting_started/core/services/package_info/package_info_service.dart';
import 'package:eztoverify_getting_started/core/services/package_info/package_info_service_Impl.dart';
import 'package:eztoverify_getting_started/core/services/storage_service/shared_pref_service.dart';
import 'package:eztoverify_getting_started/core/services/storage_service/shared_pref_service_impl.dart';
import 'package:get_it/get_it.dart';

GetIt getIt = GetIt.instance;

Future<void> setupServiceLocator() async {
  await _setupDeviceInfoService();
  await _setupPackageInfoService();
  await _setupSharedPreferencesService();
}

Future<void> _setupDeviceInfoService() async {
  getIt.registerSingleton<DeviceInfoServices>(
      DeviceInfoServiceImpl(deviceInfoPlugin: DeviceInfoPlugin())
        ..setDeviceModel()
        ..setDeviceVersion());
}

Future<void> _setupPackageInfoService() async {
  getIt.registerSingleton<PackageInfoServices>(PackageInfoServiceImpl());
}

Future<void> _setupSharedPreferencesService() async {
  getIt.registerSingleton<SharedPreferencesService>(
      SharedPreferencesServiceImpl()..initialize());
}
