import 'package:ezto_biometric/ezto_biometric.dart';
import 'package:ezto_device_info/ezto_device_info.dart';
import 'package:eztoverify/handlers/device_info_handler.dart';
// import 'package:eztoverify/handlers/ezto_pass_handler.dart';
// import 'package:eztoverify/handlers/iproov_handler.dart';
import 'package:eztoverify/handlers/silent_binding_handler.dart';
import 'package:eztoverify/handlers/system_service_handler.dart';
import 'package:eztoverify/handlers/url_service_handler.dart';
// import 'package:eztoverify/handlers/miracl_handler.dart';
import 'package:eztoverify/interface/result_interface.dart';
import 'package:eztoverify/service/android_app_update_service.dart';
import 'package:eztoverify/service/app_links_service.dart';
import 'package:eztoverify/service/ezto_result_service.dart';
import 'package:eztoverify/service/file_service.dart';
import 'package:eztoverify/service/navigation_service.dart';
import 'package:eztoverify/service/socket_push_service.dart';
import 'package:eztoverify/service/window_manager_service.dart';
import 'package:eztoverify/service/windows_permission_handler.dart';
import 'package:eztoverify/handlers/device_location_platform_handler.dart';
import 'package:get_it/get_it.dart';
import 'package:eztoverify/handlers/ashield_handler.dart';
import 'package:eztoverify/handlers/biometric_handler.dart';
import 'package:eztoverify/handlers/fido_handler.dart';
import 'package:eztoverify/handlers/push_token_handler.dart';
import 'package:eztoverify/interface/permission_handler.dart';
import 'package:eztoverify/interface/push_token_interface.dart';
import 'package:eztoverify/service/api_service.dart';
import 'package:eztoverify/service/custom_permission_service.dart';
import 'package:eztoverify/service/encryption_service.dart';
import 'package:eztoverify/service/storage_service.dart';
import 'package:eztoverify/service/websocket_service.dart';

final sl = GetIt.asNewInstance();

Future<void> initSl({
  required PermissionHandler permissionHandler,
  required PushTokenInterface pushTokenInterface,
  required ResultInterface resultInterface,
  required bool closeImmediatelyOnResult,
}) async {
  await sl.resetScope();
  sl.registerSingletonAsync<StorageService>(
    () async {
      final storage = StorageService();
      await storage.init();
      return storage;
    },
  );
  sl.registerSingletonAsync<AppLinksService>(
    () async {
      final applinkService = AppLinksService();
      await applinkService.init();
      return applinkService;
    },
    dependsOn: [StorageService],
  );

  sl.registerSingletonAsync<WindowManagerService>(
    () async {
      final windowManagerService = WindowManagerService();
      await windowManagerService.init();
      return windowManagerService;
    },
  );
  sl.registerSingleton(WebSocketService(
    closeImmediatelyOnResult: closeImmediatelyOnResult,
  ));
  sl.registerSingleton(NavigationService());
  sl.registerSingleton(EncryptionService());
  sl.registerSingleton(DeviceInfoHandler());

  sl.registerSingleton(EztoResultService(handler: resultInterface));
  sl.registerSingleton(ApiService());
  sl.registerSingleton(BiometricService());
  sl.registerSingleton(CustomPermissionService(handler: permissionHandler));
  sl.registerSingleton(AshieldHandler());
  sl.registerSingleton(FidoHandler());
  sl.registerSingleton(BiometricHandler());
  // sl.registerSingleton(EztoPassHandler());
  sl.registerSingleton(SilentBindingHandler());
  var handler = getDeviceLocationHandler();
  if (handler != null) {
    sl.registerSingleton(handler);
  }
  // sl.registerSingleton(DeviceLocationHandler());
  sl.registerSingleton(SocketPushService());
  sl.registerSingleton(SystemServiceHandler());
  sl.registerSingleton(UrlServiceHandler());
  // sl.registerSingleton(MiraclHandler());
  sl.registerSingleton(
    PushTokenHandler(pushTokenInterface: pushTokenInterface),
  );
  sl.registerSingleton(DeviceInfoService());
  // sl.registerSingleton(IproovHandler());
  sl.registerSingleton(AndroidAppUpdateService());
  sl.registerSingleton(FileService());
  sl.registerSingleton(WindowsPermissionService(handler: permissionHandler));
}
