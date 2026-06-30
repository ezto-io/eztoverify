import 'package:args/args.dart';
import 'package:branding/flavor_menu.dart';

void main(List<String> arguments) async {
  final flavorMenu = FlavorMenu();
  final ArgParser argParser = ArgParser()
    ..addOption('flavor', abbr: 'f', help: 'Specify a flavor')
    ..addFlag('list', abbr: 'l', negatable: false, help: 'List flavors')
    ..addFlag('generate',
        abbr: 'g', negatable: false, help: 'Generate specify flavor')
    ..addFlag('details',
        abbr: 'd', negatable: false, help: 'Get flavor Details')
    ..addFlag('create', abbr: 'c', negatable: false, help: 'Create new flavor')
    ..addFlag('build', abbr: 'b', negatable: false, help: 'Build flavor app')
    ..addOption('platform', abbr: 'p', help: 'Specify a platform for release')
    ..addOption('path',
        abbr: 'j', help: 'Specify a ezto verify requirement path')
    ..addFlag('cicd', abbr: 'a', negatable: false, help: 'Specify a flavor dev')
    ..addFlag('help', abbr: 'h', negatable: false, help: 'Show help');

  final ArgResults argResults = argParser.parse(arguments);

  if (argResults['help'] as bool) {
    print(argParser.usage);
    return;
  }

  await flavorMenu.chooserMenu(argResults);
}
