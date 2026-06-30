import 'dart:convert';
import 'dart:io';

import 'package:branding/models/ezto_requirement_model.dart';
import 'package:branding/new_flavor_builder.dart';
import 'package:branding/service/apple_store_service.dart';
import 'package:branding/service/file_service.dart';
import 'package:branding/service/flavor_generate_service.dart';
import 'package:chalkdart/chalk.dart';

class NewFlavorCreateBuilder {
  final baseDirectory = Directory('./lib/branding/config/flavor/base_flavor');
  final fileService = FileService();
  final newFlavorBuilder = NewFlavorBuilder();
  final appleStoreService = AppleStoreService();

  Future<void> createFlavor(String path, bool isCiCd) async {
    final flavorJson = await fileService.loadJsonData(path);

    if (flavorJson != null) {
      // final devRequirementModel = EztoRequirementModel.fromJson(flavorJson);
      final prodRequirementModel = EztoRequirementModel.fromJson(flavorJson);
      // await _dev(devRequirementModel);
      await _prod(prodRequirementModel, isCiCd);
    }
  }

  // ignore: unused_element
  Future<void> _dev(EztoRequirementModel requirement) async {
    final appName = requirement.appName.replaceAll(" ", "_");
    final folderName = "${appName}_dev";
    Directory devDirectory =
        Directory("./lib/branding/config/flavor/$folderName");
    await fileService.copyDirectory(baseDirectory, devDirectory);
    requirement.appName = "${requirement.appName} dev";
    requirement.androidAppBundleId = "${requirement.androidAppBundleId}.dev";
    var result =
        await _updateAppInfoClass(devDirectory, requirement, false, false);
    print("${chalk.green.bold("Dev app info")} - $result");

    result = await _updateThemeClass(devDirectory, requirement);
    print("${chalk.green.bold("Dev theme info")} - $result");

    result = await _updatePushFolder(devDirectory, requirement);
    print("${chalk.green.bold("Dev push folder")} - $result");

    result = await _updateAssetsLauncherFolder(devDirectory, requirement);
    print("${chalk.green.bold("Dev Assets Launcher folder")} - $result");

    result = await _updateAssetsLogoFolder(devDirectory, requirement);
    print("${chalk.green.bold("Dev Assets Logo folder")} - $result");

    result = await _updateKeystoreFolder(devDirectory, requirement);
    print("${chalk.green.bold("Dev Keystore folder")} - $result");
  }

  Future<void> _prod(EztoRequirementModel requirement, bool isCiCd) async {
    // final appName = requirement.appName.replaceAll(" ", "_");
    // final folderName = "${appName}_prod";
    final flavorName = requirement.flavorName;
    final iosDevAppName = requirement.flavorName.replaceAll("_", ".");
    final firebaseDevAppName = requirement.flavorName.replaceAll("_", " ");
    Directory prodDirectory =
        Directory("./lib/branding/config/flavor/$flavorName");
    await fileService.copyDirectory(baseDirectory, prodDirectory);

    var result = await newFlavorBuilder.updateFlavor(flavorName: flavorName);
    print("${chalk.green.bold("Flavor create")} - $result");

    result = await newFlavorBuilder.updateFlavorBuilder(flavorName: flavorName);
    print("${chalk.green.bold("Flavor Builder create")} - $result");

    result = await FlavorGenerateService().generateFlavorSwitchFile();
    print("${chalk.green.bold("Flavor Generate create")} - $result");

    result =
        await _updateAppInfoClass(prodDirectory, requirement, true, isCiCd);
    print("${chalk.green.bold("Prod App info")} - $result");

    result = await _updateThemeClass(prodDirectory, requirement);
    print("${chalk.green.bold("Prod Theme info")} - $result");

    result = await _updatePushFolder(prodDirectory, requirement);
    print("${chalk.green.bold("Prod Push folder")} - $result");

    result = await _updateAssetsLauncherFolder(prodDirectory, requirement);
    print("${chalk.green.bold("Prod Assets Launcher folder")} - $result");

    result = await _updateAssetsLogoFolder(prodDirectory, requirement);
    print("${chalk.green.bold("Prod Assets Logo folder")} - $result");

    result = await _updateKeystoreFolder(prodDirectory, requirement);
    print("${chalk.green.bold("Prod Keystore folder")} - $result");

    if (requirement.supportPlatforms.contains("iOS") && isCiCd) {
      result = await appleStoreService.createIos(appName: iosDevAppName);
      print("${chalk.green.bold("create ios app in app store")} - $result");

      result = await appleStoreService.createFirebaseIos(
          appName: iosDevAppName, firebaseDisplayName: firebaseDevAppName);
      print("${chalk.green.bold("create firebase")} - $result");
    }
  }

  Future<bool> _updateAppInfoClass(
    Directory directory,
    EztoRequirementModel requirement,
    bool isProd,
    bool isCiCd,
  ) async {
    final appInfoFile = File('${directory.path}/app_info.dart');
    final iosDevBundleId = requirement.flavorName.replaceAll("_", ".");
    if (await appInfoFile.exists()) {
      final content = await appInfoFile.readAsString();
      final updatedContent = content
          .replaceAll('{ezto_flavor_app_name}', requirement.appName)
          .replaceAll('{ezto_flavor_android_bundle_id}',
              requirement.androidAppBundleId ?? "")
          .replaceAll('{ezto_flavor_ios_cicd_bundle_id}',
              "io.ezto.verify.dev.$iosDevBundleId")
          .replaceAll(
              '{ezto_flavor_ios_bundle_id}', requirement.iosAppBundleId ?? "")
          .replaceAll('{ezto_flavor_macos_bundle_id}',
              requirement.macosAppBundleId ?? "")
          .replaceAll('{ezto_flavor_windows_bundle_id}',
              requirement.windowsAppBundleId ?? "")
          .replaceAll('"{ezto_flavor_isCopyAuthCode}" as bool',
              isProd ? false.toString() : true.toString())
          .replaceAll('"{ezto_flavor_isProd}" as bool', isProd.toString())
          .replaceAll('"{ezto_flavor_isIosCiCd}" as bool', isCiCd.toString())
          .replaceAll(
              '{ezto_flavor_terms}', requirement.termsAndCondition ?? "")
          .replaceAll('{ezto_flavor_privacy}', requirement.privacyPolicy ?? "");
      await appInfoFile.writeAsString(updatedContent);
      return true;
    } else {
      return false;
    }
  }

  Future<bool> _updateThemeClass(
    Directory directory,
    EztoRequirementModel requirement,
  ) async {
    final themeFile = File('${directory.path}/theme.dart');
    if (await themeFile.exists()) {
      final content = await themeFile.readAsString();
      final updatedContent = content
          .replaceAll('{ezto_flavor_primary_color}', requirement.primaryColor)
          .replaceAll('{ezto_flavor_font_name}', requirement.fontName);
      await themeFile.writeAsString(updatedContent);
      return true;
    } else {
      return false;
    }
  }

  Future<bool> _updatePushFolder(
    Directory directory,
    EztoRequirementModel requirement,
  ) async {
    final androidFirebaseJson = requirement.androidFirebaseJson;
    final iosFirebasePlist = requirement.androidFirebaseJson;

    final firebaseJson = _convertBase64ToBytes(androidFirebaseJson!);
    final firebasePlist = _convertBase64ToBytes(iosFirebasePlist!);

    final pushFolder = Directory('${directory.path}/push');
    if (!await pushFolder.exists()) {
      await pushFolder.create(recursive: true);
    }

    final firebaseJsonFile =
        File('${directory.path}/push/google-services.json');
    final firebasePlistFile =
        File('${directory.path}/push/GoogleService-Info.plist');

    await firebaseJsonFile.writeAsBytes(firebaseJson);
    await firebasePlistFile.writeAsBytes(firebasePlist);
    return true;
  }

  Future<bool> _updateKeystoreFolder(
    Directory directory,
    EztoRequirementModel requirement,
  ) async {
    final androidKeystore = requirement.androidKeystoreFile;

    if (androidKeystore != null) {
      final keystoreBytes = _convertBase64ToBytes(androidKeystore);
      final keystoreFolder = Directory('${directory.path}/keystore');
      if (!await keystoreFolder.exists()) {
        await keystoreFolder.create(recursive: true);
      }
      final keystoreJsonFile =
          File('${keystoreFolder.path}/android-keystore.keystore');
      await keystoreJsonFile.writeAsBytes(keystoreBytes);
      return true;
    }
    return false;
  }

  Future<bool> _updateAssetsLauncherFolder(
    Directory directory,
    EztoRequirementModel requirement,
  ) async {
    final appLogo = requirement.appTransparentLogo;

    final appLogoBytes = _convertBase64ToBytes(appLogo);
    final appLogoFile =
        File('${directory.path}/assets/launcher/transparent.png');
    await appLogoFile.writeAsBytes(appLogoBytes);
    return true;
  }

  Future<bool> _updateAssetsLogoFolder(
    Directory directory,
    EztoRequirementModel requirement,
  ) async {
    var isBool = true;
    final appFullLogo = requirement.appFullLogo;
    final appLogoSimple = requirement.appLogoSimple;
    final appLogoWhite = requirement.appLogoWhite;

    try {
      final appLogoBytes = _convertBase64ToBytes(appFullLogo);
      final appLogoFile = File('${directory.path}/assets/logo/logo_full.svg');
      await appLogoFile.writeAsBytes(appLogoBytes);
    } catch (e) {
      isBool = false;
      print('Error decoding or writing SVG file: $e');
    }

    try {
      final appLogoSimpleBytes = _convertBase64ToBytes(appLogoSimple);
      final appLogoSimpleFile =
          File('${directory.path}/assets/logo/logo_simple.svg');
      await appLogoSimpleFile.writeAsBytes(appLogoSimpleBytes);
    } catch (e) {
      isBool = false;
      print('Error decoding or writing SVG file: $e');
    }

    try {
      final appLogoWhiteBytes = _convertBase64ToBytes(appLogoWhite);
      final appLogoWhiteFile =
          File('${directory.path}/assets/logo/logo_simple_white.svg');
      await appLogoWhiteFile.writeAsBytes(appLogoWhiteBytes);
    } catch (e) {
      isBool = false;
      print('Error decoding or writing SVG file: $e');
    }

    return isBool;
  }

  List<int> _convertBase64ToBytes(
    String base64String,
  ) {
    final base64Data = base64String.split(',').last;
    final bytes = base64Decode(base64Data);
    return bytes;
  }
}
