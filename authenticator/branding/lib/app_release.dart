import 'package:branding/models/flavor_model.dart';
import 'package:branding/service/shell_service.dart';

class AppRelease {
  final FlavorModel flavor;

  const AppRelease({
    required this.flavor,
  });
  Future<bool> startRelease(int? platform) async {
    switch (platform) {
      case 1: //Android
        return releaseAndroid();
      case 2: //ios
        return releaseiOS();
      case 3: //mac os
        return releaseMacOs();
      case 4: //windows
        return releaseWindows();
      default:
        print("Invalid Option");
        return false;
    }
  }

  Future<bool> releaseAndroid() async {
    bool lockRemoved = await runShellCommand("rm", ["-rf", "pubspec.lock"]);
    if (!lockRemoved) return false;

    // Run flutter clean
    bool cleaned = await runShellCommand("flutter", ["clean"]);
    if (!cleaned) return false;

    // Run flutter pub get
    bool pubGet = await runShellCommand("flutter", ["pub", "get"]);
    if (!pubGet) return false;

    return runShellCommand("flutter", ["build", "appbundle"]);
  }

  Future<bool> releaseiOS() async {
    bool cleaned = await runShellCommand("flutter", ["clean"]);
    if (!cleaned) return false;

    // Run flutter pub get
    bool pubGet = await runShellCommand("flutter", ["pub", "get"]);
    if (!pubGet) return false;

    final command = '''
      pwd
      cd ios
      rm -rf Pods                       
      rm -rf Podfile.lock
      rm -rf .symlinks
      pod install --repo-update
    ''';

    bool repoUpdate = await runShellCommand('sh', ['-c', command]);
    if (!repoUpdate) return false;

    return runShellCommand("flutter", [
      "build",
      "ipa",
    ]);
  }

  Future<bool> releaseMacOs() async {
    return runShellCommand("flutter", ["build", "macos"]);
  }

  Future<bool> releaseWindows() async {
    return runShellCommand("dart", [
      "run",
      "msix:create",
      "--display-name",
      flavor.appInfo.appName,
      "--publisher-display-name",
      "ezto",
      "--identity-name",
      flavor.appInfo.androidBundleId,
      "--version",
      "1.0.0.0",
      "--logo-path",
      "branding/lib/branding/config/flavor/${flavor.flavorName}/assets/windows.png",
      "--capabilities",
      "internetClient, location, microphone, webcam",
      "--enable-at-startup",
      "true",
      "--store",
      "false",
      "--protocol-activation",
      "eztovrfy",
    ]);
  }
}
