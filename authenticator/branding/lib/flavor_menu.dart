import 'dart:io';

import 'package:args/args.dart';
import 'package:branding/app_release.dart';
import 'package:branding/flavor_builder.dart';
import 'package:branding/flavor_details.dart';
import 'package:branding/generated/generate_choose_flavor.dart';
import 'package:branding/models/flavor_model.dart';
import 'package:branding/new_flavor_builder.dart';
import 'package:branding/new_flavor_create_builder.dart';
import 'package:chalkdart/chalk.dart';

class FlavorMenu {
  Future<void> chooserMenu(ArgResults argResults) async {
    final list = argResults['list'];
    final flavor = argResults['flavor'];
    final generate = argResults['generate'];
    final flavorDetails = argResults['details'];
    final build = argResults['build'];
    final platform = argResults['platform'];
    final create = argResults['create'];
    final path = argResults['path'];
    final isCiCd = argResults['cicd'];

    if (!list && !create && flavor == null && !build) {
      _showMenu();
      final String? input = stdin.readLineSync();
      await _handleOption(input, null, null, null, false);
    } else {
      String options = "0";
      if (list) {
        options = "1";
      }
      if (create && path != null) {
        options = "4";
      }
      if (flavor != null) {
        if (generate) {
          options = "2";
        }
        if (flavorDetails) {
          options = "3";
        }
        if (build && platform != null) {
          options = "5";
        }
      }
      await _handleOption(options, flavor, platform, path, isCiCd);
    }
  }

  void _showMenu() {
    print("*****Please choose an option******");
    print("1. List Flavors");
    print("2. Build Flavor");
    print("3. Show Flavor details");
    print("4. Create a new Flavor");
    print("5. Release App");
    print("Enter option number:");
  }

  Future<void> _handleOption(
    String? option,
    String? flavorName,
    String? platform,
    String? path,
    bool isCiCd,
  ) async {
    int currentOption = int.tryParse(option ?? '') ?? 0;

    switch (currentOption) {
      case 1:
        await buildListFlavorMenu();
        break;
      case 2:
        await buildChangeFlavorMenu(flavorName);
        break;
      case 3:
        await buildFlavorDetails(flavorName);
        break;
      case 4:
        await buildCreateNewFlavor(path, isCiCd);
        break;
      case 5:
        await buildReleaseFlavorMenu(flavorName, platform);
        break;
      default:
        print(chalk.red.bold("Invalid input"));
        _showMenu();
    }
  }

  Future<void> buildListFlavorMenu() async {
    final result = await NewFlavorBuilder().getFlavours();
    print(chalk.bold.blue(result.join(", ")));
  }

  Future<FlavorModel?> buildChangeFlavorMenu(String? flavorNameArgs) async {
    final flavorName = await getFlavorName(flavorNameArgs);
    final flavor = await checkForFlavor(flavorName);

    if (!flavor) {
      print(chalk.red.bold("Invalid Flavor"));
      return null;
    } else {
      final flavorModel = FlavorSwitcher.chooseFlavor(flavorName);
      await FlavorBuilder().buildFlavor(flavorModel);
      return flavorModel;
    }
  }

  Future<void> buildFlavorDetails(String? flavorNameArgs) async {
    final flavorName = await getFlavorName(flavorNameArgs);
    final flavor = await checkForFlavor(flavorName);

    if (!flavor) {
      print(chalk.red.bold("Invalid Flavor"));
    } else {
      final flavorModel = FlavorSwitcher.chooseFlavor(flavorName);
      await FlavorDetails().buildListFlavorMenu(flavorModel, flavorName);
    }
  }

  Future<void> buildCreateNewFlavor(String? path, bool isCiCd) async {
    String flavorPath = "";
    final newFlavorCreateBuilder = NewFlavorCreateBuilder();
    if (path != null) {
      flavorPath = path;
    } else {
      print("Requirement Json Path");
      flavorPath = stdin.readLineSync() ?? "";
    }
    await newFlavorCreateBuilder.createFlavor(flavorPath, isCiCd);
  }

  Future<void> buildReleaseFlavorMenu(
    String? flavorNameArgs,
    String? platformName,
  ) async {
    int selectedPlatform = 0;
    final flavor = await buildChangeFlavorMenu(flavorNameArgs);
    if (flavor == null) {
      print(chalk.red.bold("Invalid Input"));
      return;
    }
    final appRelease = AppRelease(flavor: flavor);

    if (platformName != null) {
      selectedPlatform = checkPlatform(platformName);
    } else {
      _showPlatform();
      selectedPlatform = int.tryParse(stdin.readLineSync() ?? "") ?? 0;
    }

    final result = await appRelease.startRelease(selectedPlatform);
    print(chalk.blue.bold("Build Status - $result"));
  }

  Future<List<String>> listFlavors() async {
    final list = await NewFlavorBuilder().getFlavours();
    list.sort();
    return list;
  }

  Future<bool> checkForFlavor(String name) async {
    try {
      List<String> flavor = await NewFlavorBuilder().getFlavours();
      return flavor.contains(name);
    } catch (e) {
      return false;
    }
  }

  Future<String> getFlavorName(String? flavorNameArgs) async {
    String flavorName;
    if (flavorNameArgs == null) {
      final listFlavor = await listFlavors();
      print(chalk.bold.blue(listFlavor.join(", ")));
      print("Enter Flavor Name:");
      flavorName = stdin.readLineSync() ?? "";
    } else {
      flavorName = flavorNameArgs;
    }
    return flavorName;
  }

  void _showPlatform() {
    print("Choose a platform to release");
    print("1. Android");
    print("2. iOS");
    print("3. Mac OS");
    print("4. Windows");
  }

  int checkPlatform(String platformName) {
    int platform = 0;
    switch (platformName) {
      case "android":
        platform = 1;
        break;
      case "ios":
        platform = 2;
        break;
      case "macos":
        platform = 3;
        break;
      case "windows":
        platform = 4;
        break;
      default:
        platform = 0;
        break;
    }
    return platform;
  }
}
