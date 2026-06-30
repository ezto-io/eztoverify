import 'package:branding/service/firebase_service.dart';
import 'package:branding/service/shell_service.dart';

class AppleStoreService {
  Future<bool> createIos({required String appName}) async {
    final name = appName.replaceAll("_", " ");
    final command = '''
      pwd
      cd ios
      fastlane create_app name:$name bundle_id:"io.ezto.verify.dev.$appName" sku:"io.ezto.verify.dev.$appName"
    ''';
    final result = await runShellCommand('sh', ['-c', command]);
    return result;
  }

  Future<bool> createFirebaseIos({
    required String appName,
    required String firebaseDisplayName,
  }) async {
    final result = await FirebaseService().createFirebaseApp(
        bundleId: "io.ezto.verify.dev.$appName", appName: firebaseDisplayName);
    return result;
  }
}
