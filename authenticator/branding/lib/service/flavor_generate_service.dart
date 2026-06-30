import 'package:branding/new_flavor_builder.dart';
import 'package:branding/service/file_service.dart';

class FlavorGenerateService {
  final newFlavorBuilder = NewFlavorBuilder();
  Future<bool> generateFlavorSwitchFile() async {
    try {
      final flavors = await newFlavorBuilder.getFlavours();
      // Parse JSON data
      final data = await newFlavorBuilder.getFlavorBuilder();

      final importStatements = flavors.map((flavor) {
        return "import '../branding/config/flavor/$flavor/index.dart' as $flavor;";
      }).join('\n');

      // Start building the Dart code for the chooseFlavor method
      final buffer = StringBuffer();
      buffer.writeln('// Generated file. Do not modify.');
      buffer.writeln();
      buffer.writeln("import 'package:branding/models/flavor_model.dart';");
      buffer.writeln(importStatements);
      buffer.writeln();
      buffer.writeln('class FlavorSwitcher {');
      buffer.writeln('  static FlavorModel chooseFlavor(String key) {');
      buffer.writeln('    switch (key) {');

      // Generate cases for each key in the JSON data
      data.forEach((flavorKey, flavorValues) {
        buffer.writeln('      case "$flavorKey":');
        buffer.writeln('        return FlavorModel(');
        flavorValues.forEach((k, v) {
          buffer.writeln('        $k: ${(k == "flavorName") ? '"$v"' : v} ,');
        });
        buffer.writeln('        );');
      });

      // Default case for unknown keys
      buffer.writeln('      default:');
      buffer
          .writeln('        throw ArgumentError("Unknown flavor key: \$key");');
      buffer.writeln('    }');
      buffer.writeln('  }');
      buffer.writeln('}');

      // Write the Dart code to the file
      final content = buffer.toString();
      await FileService().saveToFile(
        "./lib/generated/generate_choose_flavor.dart",
        content,
      );
    } catch (e) {
      print('Error generating file: $e');
    }
    return true;
  }
}
