import 'dart:io';

import 'package:branding/models/flavor_model.dart';
import 'package:chalkdart/chalk.dart';

class FlavorDetails {
  Future<void> buildListFlavorMenu(
      FlavorModel flavor, String flavorName) async {
    _printFlavorDetails(
      question: "App Name",
      answer: flavor.appInfo.appName,
    );
    _printFlavorDetails(
      question: "Android App Bundle Id",
      answer: flavor.appInfo.androidBundleId,
    );
    _printFlavorDetails(
      question: "Prod App",
      answer: flavor.appInfo.isProd.toString(),
    );
    _printFlavorDetails(
      question: "Terms and Condition",
      answer: flavor.appInfo.urls.terms,
    );
    _printFlavorDetails(
      question: "Privacy Policy",
      answer: flavor.appInfo.urls.privacy,
    );
    _printColor(
      question: "Primary Color",
      answer: flavor.theme.primaryColor,
    );
    _printColor(
      question: "Primary Text Color",
      answer: flavor.theme.primaryTextColor,
    );
    _printFlavorDetails(
      question: "Font Name",
      answer: flavor.theme.fontName,
    );
    _printListData(
      question: "iOS Fido",
      answer: flavor.deeplinks.appleFidoDomains,
    );
    _printListData(
      question: "Deeplinks",
      answer: flavor.deeplinks.deepLinkDomains,
    );
    _printListData(
      question: "Android App Schemes",
      answer: flavor.deeplinks.androidAppSchemes,
    );
    _printListData(
      question: "iOS App Schemes",
      answer: flavor.deeplinks.iosAppSchemes,
    );
    await _printImage(
      question: "App Transparent Image",
      imagePath:
          'lib/branding/config/flavor/$flavorName/assets/launcher/transparent.png',
    );
  }

  void _printFlavorDetails({
    required String question,
    required String answer,
  }) {
    print("${chalk.green("$question - ")} ${chalk.bold.blue(answer)}");
    _divider();
  }

  void _printColor({
    required String question,
    required int answer,
  }) {
    String hexColor = _decimalToHex(answer);
    final customColor = chalk.rgb(
      int.parse(hexColor.substring(1, 3), radix: 16),
      int.parse(hexColor.substring(3, 5), radix: 16),
      int.parse(hexColor.substring(5, 7), radix: 16),
    );
    print(
        "${chalk.green("$question - ")} ${chalk.bold(customColor(hexColor))}");
    _divider();
  }

  void _printListData({
    required String question,
    required List<String> answer,
  }) {
    print(chalk.green(question));
    for (String link in answer) {
      print(chalk.bold.blue('• $link'));
    }
    _divider();
  }

  Future<void> _printImage({
    required String question,
    required String imagePath,
  }) async {
    print(chalk.green(question));
    if (File(imagePath).existsSync()) {
      List<String> arguments = [imagePath];
      arguments.insert(0, '--width=30');
      arguments.insert(0, '--height=15');
      await Process.run('viu', arguments).then((result) {
        stdout.write(result.stdout);
        stderr.write(result.stderr);
      });
    } else {
      print(chalk.red.blue('File not found: $imagePath'));
    }
    _divider();
  }

  void _divider({int length = 60, String char = '-'}) {
    print(chalk.white(char * length));
  }

  String _decimalToHex(int decimal) {
    String hex = decimal.toRadixString(16).padLeft(8, '0');

    return '#${hex.toUpperCase()}';
  }
}
