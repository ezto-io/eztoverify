import 'package:branding/code_builder.dart';
import 'package:branding/models/flavor_model.dart';
import 'package:branding/service/file_service.dart';
import 'package:branding/service/push_service.dart';
import 'package:branding/template_builder.dart';

class FlavorBuilder {
  Future<void> buildFlavor(FlavorModel flavor) async {
    final fileService = FileService();

    final templateBuilder = TemplateBuilder(
      flavor: flavor,
      fileService: fileService,
    );

    print("*****BUILDING ANDROID*******\n");

    var result = await templateBuilder.copyAndroidManifest();
    print("success - $result");

    result = await templateBuilder.copyAndroidAppBuildGradle();
    print("success - $result");

    final pushService = PushService(
      fileService: fileService,
      flavor: flavor,
    );
    result = await pushService.moveAndroidFirebaseAndKeystoreConfig();
    print("success - $result");

    result = await templateBuilder.copyLauncherIcons();
    final codeBuilder = CodeBuilder(
      flavor: flavor,
      fileService: fileService,
    );

    print("*****BUILDING IOS*******\n");
    result = await templateBuilder.editIosBundleId();
    print("success - $result");

    result = await templateBuilder.copyIosExportOptionsPlist();
    print("success - $result");

    result = await templateBuilder.copyIosInfoPlist();
    print("success - $result");

    result = await templateBuilder.copyIosRunnerEntitlements();
    print("success - $result");

    result = await pushService.moveIosFirebasePushConfig();
    print("success - $result");

    print("*****BUILDING MACOS*******\n");

    result = await templateBuilder.editMacOsBundleId();
    print("success - $result");

    result = await templateBuilder.copyMacOsInfoPlist();
    print("success - $result");

    result = await templateBuilder.copyMacosRunnerEntitlements();
    print("success - $result");

    print("");
    print("*****BUILDING COMMON*******\n");

    result = await codeBuilder.buildAppConstants();
    print("success - $result");

    result = await codeBuilder.buildThemeConstants();
    print("success - $result");

    result = await codeBuilder.buildUrlConstants();
    print("success - $result");
  }
}
