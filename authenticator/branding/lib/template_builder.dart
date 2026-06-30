import "dart:async";
import "dart:io";

import "package:branding/models/flavor_model.dart";
import "package:branding/service/file_service.dart";
import "package:branding/service/shell_service.dart";
import "package:xml/xml.dart";

class TemplateBuilder {
  final FileService fileService;
  final FlavorModel flavor;

  TemplateBuilder({
    required this.flavor,
    required this.fileService,
  });

  Future<bool> editMacOsBundleId() async {
    print("**********");
    print("Editing pbxproj");

    try {
      final teamId = flavor.appSigning.appTeamId;
      final profileName = flavor.appSigning.macOsProvisioningProfileName;
      final file = File('./../macos/Runner.xcodeproj/project.pbxproj');
      final lines = file.readAsLinesSync();
      final fileSink = file.openWrite();

      for (var line in lines) {
        if (line.contains("PRODUCT_BUNDLE_IDENTIFIER")) {
          line = 'PRODUCT_BUNDLE_IDENTIFIER = ${flavor.appInfo.macosBundleId};';
        }

        if (line.contains("DEVELOPMENT_TEAM[sdk=macosx*]")) {
          line = '"DEVELOPMENT_TEAM[sdk=macosx*]" = $teamId;';
        }

        if (line.contains("PROVISIONING_PROFILE_SPECIFIER[sdk=macosx*]")) {
          line =
              '"PROVISIONING_PROFILE_SPECIFIER[sdk=macosx*]" = "$profileName";';
        }

        fileSink.writeln(line);
      }
      fileSink.close();
      return true;
    } catch (e) {
      print(e);
      return false;
    }
  }

  Future<bool> copyMacOsInfoPlist() async {
    print("**********");
    print("Copying macos Info.plist");
    final file = File('lib/branding/template/macos_info.plist');
    try {
      String contents = await file.readAsString();
      final appSchemes = flavor.deeplinks.iosAppSchemes;

      //1. Read and update Permission
      final requiredPermissions = flavor.permissions.applePermissions;
      final permissionXmlBuilder = XmlBuilder();
      String permissionStringBuilder = "";
      for (var e in requiredPermissions.entries) {
        permissionXmlBuilder.element("key", nest: () {
          permissionXmlBuilder.text(e.key);
        });
        permissionXmlBuilder.element("string", nest: () {
          permissionXmlBuilder.text(e.value);
        });
        permissionStringBuilder +=
            "${permissionXmlBuilder.buildDocument().toXmlString(pretty: true)}\n";
      }
      contents = contents.replaceFirst(
        "<!--{ezto_permission}-->",
        permissionStringBuilder,
      );

      //2. Deeplink
      contents = contents.replaceAll(
        "<!--{ezto_bundle_id}-->",
        flavor.appInfo.macosBundleId,
      );

      //3. Change app name
      contents = contents.replaceAll(
        "<!--{ezto_appname}-->",
        flavor.appInfo.appName,
      );

      //4. Add app schemes
      final schemeXmlBuilder = XmlBuilder();
      String schemeStringBuilder = "";
      for (var e in appSchemes) {
        schemeXmlBuilder.element("string", nest: () {
          schemeXmlBuilder.text(e);
        });
        schemeStringBuilder +=
            "${schemeXmlBuilder.buildDocument().toXmlString(pretty: true)}\n";
      }
      contents = contents.replaceFirst(
        "<!--{ezto_app_shemes}-->",
        schemeStringBuilder,
      );

      await fileService.saveToFile("./../macos/Runner/Info.plist", contents);

      return true;
    } catch (e) {
      print(e);
      return false;
    }
  }

  Future<bool> copyMacosRunnerEntitlements() async {
    print("**********");
    print("Copying MacOs Release.Entitlements");
    try {
      final file = File('lib/branding/template/macos_release.entitlements');
      String contents = await file.readAsString();

      // 1. Update fido
      // <!--{ezto_fido}-->
      final requiredFidoLinks = flavor.deeplinks.appleFidoDomains;
      final fidoXmlBuilder = XmlBuilder();
      String fidolinksStringBuilder = "";
      for (var e in requiredFidoLinks) {
        fidoXmlBuilder.element("string", nest: () {
          fidoXmlBuilder.text("webcredentials:$e");
        });
        fidolinksStringBuilder +=
            fidoXmlBuilder.buildDocument().toXmlString(pretty: true);
      }
      contents = contents.replaceFirst(
        "<!--{ezto_fido}-->",
        fidolinksStringBuilder,
      );

      await fileService.saveToFile(
        "./../macos/Runner/Release.Entitlements",
        contents,
      );

      print("**********");
      print("Copying MacOs DebugProfile.Entitlements");

      final debugEntitlement = """
              <key>com.apple.security.cs.allow-jit</key>
      <true/>
      <key>com.apple.security.network.client</key>
      <true/>
""";

      contents = contents.replaceFirst(
        "<!--{ezto_flutter_debug}-->",
        debugEntitlement,
      );

      await fileService.saveToFile(
        "./../macos/Runner/DebugProfile.Entitlements",
        contents,
      );
      return true;
    } catch (e) {
      print(e);
      return false;
    }
  }

  Future<bool> editIosBundleId() async {
    print("**********");
    print("Editing pbxproj");

    try {
      final file = File('./../ios/Runner.xcodeproj/project.pbxproj');
      final lines = file.readAsLinesSync();
      final fileSink = file.openWrite();
      for (var line in lines) {
        if (line.contains("PRODUCT_BUNDLE_IDENTIFIER = ") &&
            !line.contains('RunnerTests')) {
          line =
              "PRODUCT_BUNDLE_IDENTIFIER = ${flavor.appInfo.isIosCiCd ? flavor.appInfo.iosCiCdBundleId : flavor.appInfo.iosBundleId};";
        }

        // Write the (potentially modified) line to the file
        fileSink.writeln(line);
      }
      fileSink.close();
      return true;
    } catch (e) {
      print("e : $e");
      return false;
    }
  }

  Future<bool> copyIosInfoPlist() async {
    print("**********");
    print("Copying iOS Info.plist");
    final file = File('lib/branding/template/ios_info.plist');
    try {
      String contents = await file.readAsString();
      final appSchemes = flavor.deeplinks.iosAppSchemes;

      //1. Read and update Permission
      final requiredPermissions = flavor.permissions.applePermissions;
      final permissionXmlBuilder = XmlBuilder();
      String permissionStringBuilder = "";
      for (var e in requiredPermissions.entries) {
        permissionXmlBuilder.element("key", nest: () {
          permissionXmlBuilder.text(e.key);
        });
        permissionXmlBuilder.element("string", nest: () {
          permissionXmlBuilder.text(e.value);
        });
        permissionStringBuilder +=
            "${permissionXmlBuilder.buildDocument().toXmlString(pretty: true)}\n";
      }
      contents = contents.replaceFirst(
        "<!--{ezto_permission}-->",
        permissionStringBuilder,
      );

      //2. Deeplink
      contents = contents.replaceAll(
        "<!--{ezto_ios_bundle_id}-->",
        flavor.appInfo.isIosCiCd
            ? flavor.appInfo.iosCiCdBundleId
            : flavor.appInfo.iosBundleId,
      );

      //3. Change app name
      contents = contents.replaceAll(
        "<!--{ezto_appname}-->",
        flavor.appInfo.appName,
      );

      //4. Add app schemes
      final schemeXmlBuilder = XmlBuilder();
      String schemeStringBuilder = "";
      for (var e in appSchemes) {
        schemeXmlBuilder.element("string", nest: () {
          schemeXmlBuilder.text(e);
        });
        schemeStringBuilder +=
            "${schemeXmlBuilder.buildDocument().toXmlString(pretty: true)}\n";
      }
      contents = contents.replaceFirst(
        "<!--{ezto_app_shemes}-->",
        schemeStringBuilder,
      );

      await fileService.saveToFile("./../ios/Runner/Info.plist", contents);

      return true;
    } catch (e) {
      print(e);
      return false;
    }
  }

  Future<bool> copyIosExportOptionsPlist() async {
    print("**********");
    print("Copying iOS ExportOptions.plist");
    final file = File('lib/branding/template/ios_export_options.plist');
    try {
      String contents = await file.readAsString();
      final teamId = flavor.appSigning.appTeamId;
      final provisioningProfileId = flavor.appSigning.iosProvisioningProfileId;

      //1. Update bundle ID
      contents = contents.replaceFirst(
        "<!--{ezto_ios_bundle_id}-->",
        flavor.appInfo.isIosCiCd
            ? flavor.appInfo.iosCiCdBundleId
            : flavor.appInfo.iosBundleId,
      );

      //2. Update provisioning profile ID
      contents = contents.replaceAll(
        "<!--{ezto_provision_profile_id}-->",
        provisioningProfileId,
      );

      //3. Change Appstore team Id
      contents = contents.replaceAll(
        "<!--{ezto_team_id}-->",
        teamId,
      );

      await fileService.saveToFile("./../ios/ExportOptions.plist", contents);
      return true;
    } catch (e) {
      print(e);
      return false;
    }
  }

  Future<bool> copyIosRunnerEntitlements() async {
    print("**********");
    print("Copying iOS Runner.Entitlements");
    try {
      final file = File('lib/branding/template/ios_runner.entitlements');
      String contents = await file.readAsString();

      //1. Add fido applinks
      //  <!--{ezto_fido}-->
      final requiredFidoLinks = flavor.deeplinks.appleFidoDomains;
      final fidoXmlBuilder = XmlBuilder();
      String fidoStringBuilder = "";
      for (var e in requiredFidoLinks) {
        fidoXmlBuilder.element("string", nest: () {
          fidoXmlBuilder.text("webcredentials:$e");
        });
        fidoStringBuilder +=
            fidoXmlBuilder.buildDocument().toXmlString(pretty: true);
      }
      contents = contents.replaceFirst(
        "<!--{ezto_fido}-->",
        fidoStringBuilder,
      );

      // 2. Update deeplinks
      // <!--{ezto_deeplinks_intent}-->
      final requiredDeepLinks = flavor.deeplinks.deepLinkDomains;
      final deeplinksXmlBuilder = XmlBuilder();
      String deeplinksStringBuilder = "";
      for (var e in requiredDeepLinks) {
        deeplinksXmlBuilder.element("string", nest: () {
          deeplinksXmlBuilder.text("applinks:$e");
        });
        deeplinksStringBuilder +=
            deeplinksXmlBuilder.buildDocument().toXmlString(pretty: true);
      }
      contents = contents.replaceFirst(
        "<!--{ezto_deeplinks_intent}-->",
        deeplinksStringBuilder,
      );

      await fileService.saveToFile(
        "./../ios/Runner/Runner.Entitlements",
        contents,
      );

      return true;
    } catch (e) {
      print(e);
      return false;
    }
  }

  Future<bool> copyAndroidManifest() async {
    print("**********");
    print("Copying Android Manifest");
    final file = File('lib/branding/template/android_manifest.xml');
    try {
      String contents = await file.readAsString();

      //Update comments
      contents = contents.replaceFirst(
        "<!--{ezto_template}-->",
        "<!--This is a generated template. Do not edit manually-->",
      );

      //1. Read and update Permission
      final requiredPermissions = flavor.permissions.androidPermissions;
      final permissionXmlBuilder = XmlBuilder();
      String permissionStringBuilder = "";
      for (var e in requiredPermissions) {
        permissionXmlBuilder.element("uses-permission",
            attributes: {"android:name": "android.permission.$e"});
        permissionStringBuilder +=
            "${permissionXmlBuilder.buildDocument().toXmlString()}\n";
      }
      contents = contents.replaceFirst(
          "<!--{ezto_permission}-->", permissionStringBuilder);

      //2. Update Query packages
      //For App to App scheme to work
      final requiredAppToAppSchemes = flavor.deeplinks.androidAppSchemes;
      final appToAppSchemeBuilder = XmlBuilder();
      appToAppSchemeBuilder.element("queries", nest: () {
        for (var e in requiredAppToAppSchemes) {
          appToAppSchemeBuilder
              .element("package", attributes: {"android:name": e});
        }
      });
      contents = contents.replaceFirst(
        "<!--{ezto_query_packages}-->",
        appToAppSchemeBuilder.buildDocument().toXmlString(pretty: true),
      );

      //3. Update deeplinks
      final requiredDeeplinkDomains = flavor.deeplinks.deepLinkDomains;
      final deeplinksXmlBuilder = XmlBuilder();
      String deeplinksStringBuilder = "";
      for (var e in requiredDeeplinkDomains) {
        deeplinksXmlBuilder.element("data", attributes: {"android:host": e});
        deeplinksStringBuilder +=
            "${deeplinksXmlBuilder.buildDocument().toXmlString()}\n";
      }
      contents = contents.replaceFirst(
        "<!--{ezto_deeplinks_intent}-->",
        deeplinksStringBuilder,
      );
      contents = contents.replaceFirst(
        "{ezto_app_scheme}",
        flavor.appInfo.androidBundleId,
      );

      //4. Update app name
      contents = contents.replaceFirst(
        "{ezto_appname}",
        flavor.appInfo.appName,
      );
      final xmlBuilder = XmlDocument.parse(contents);

      await fileService.saveToFile(
        './../android/app/src/main/AndroidManifest.xml',
        xmlBuilder.toXmlString(pretty: true),
      );

      return true;
    } catch (e) {
      print(e);
      return false;
    }
  }

  Future<bool> copyAndroidAppBuildGradle() async {
    print("");
    print("**********");
    print("Copying Android app/build.gradle");
    try {
      final file = File('lib/branding/template/android_app_build.gradle');
      String contents = await file.readAsString();
      final keystoreName = flavor.flavorName.replaceAll('_', "-");
      contents = contents.replaceFirst(
        "{ezto_android_bundle_id}",
        flavor.appInfo.androidBundleId,
      );
      contents = contents.replaceAll(
        "{flavor_name}",
        flavor.flavorName.toUpperCase(),
      );
      contents = contents.replaceFirst(
        "{keystore_file_name}",
        keystoreName,
      );
      await fileService.saveToFile(
        './../android/app/build.gradle',
        contents,
      );
      print("TODO: Fix signing logic");
      return true;
    } catch (e) {
      print(e);
      return false;
    }
  }

  Future<bool> copyLauncherIcons() async {
    print("");
    print("**********");
    print("Copying Launcher icons");
    try {
      final file = File('lib/branding/template/flutter_launcher_icons.yaml');
      String contents = await file.readAsString();
      contents = contents.replaceAll(
        "{ezto_flavor}",
        flavor.flavorName,
      );
      contents = contents.replaceFirst(
        "{ezto_background_color}",
        "#FFFFFF",
      );
      await fileService.saveToFile(
        './../flutter_launcher_icons.yaml',
        contents,
      );

      await runShellCommand("flutter", [
        "pub",
        "run",
        "flutter_launcher_icons",
        "-f",
        "flutter_launcher_icons.yaml",
      ]);
      return true;
    } catch (e) {
      print(e);
      return false;
    }
  }
}
