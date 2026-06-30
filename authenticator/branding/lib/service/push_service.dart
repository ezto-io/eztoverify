import 'package:branding/models/flavor_model.dart';
import 'package:branding/service/file_service.dart';

class PushService {
  final FileService fileService;
  final FlavorModel flavor;

  PushService({
    required this.fileService,
    required this.flavor,
  });

  Future<bool> moveAndroidFirebaseAndKeystoreConfig() async {
    print("");
    print("**********");
    print("Copying Android google-services.json");
    final keystoreName = flavor.flavorName.replaceAll('_', "-");
    try {
      await fileService.copyFile(
        "lib/branding/config/flavor/${flavor.flavorName}/push/google-services.json",
        "./../android/app/google-services.json",
      );
      await fileService.copyFile(
        "lib/branding/config/flavor/${flavor.flavorName}/keystore/android_keystore.keystore",
        "./../android/app/$keystoreName.keystore",
      );
      return true;
    } catch (e) {
      print(e);
      return false;
    }
  }

  Future<bool> moveIosFirebasePushConfig() async {
    print("*********");
    print("Copying iOS GoogleService-Info.plist");
    try {
      if (flavor.appInfo.isIosCiCd) {
        return true;
      }
      await fileService.copyFile(
        "lib/branding/config/flavor/${flavor.flavorName}/push/GoogleService-Info.plist",
        "./../ios/Runner/GoogleService-Info.plist",
      );
      return true;
    } catch (e) {
      print(e);
      return false;
    }
  }
}
