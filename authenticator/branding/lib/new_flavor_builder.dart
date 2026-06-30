import 'dart:convert';
import 'dart:io';

class NewFlavorBuilder {
  Future<List<String>> getFlavours() async {
    try {
      final file = File("./lib/json/flavor.json");
      final jsonString = await file.readAsString();

      final jsonData = jsonDecode(jsonString);

      if (jsonData is Map<String, dynamic> && jsonData['flavors'] is List) {
        final List<dynamic> flavours = jsonData['flavors'];

        return flavours.map((flavour) => flavour.toString()).toList();
      } else {
        throw Exception('Invalid JSON format or no flavors found.');
      }
    } catch (e) {
      throw Exception('Error reading or parsing file: $e');
    }
  }

  Future<bool> updateFlavor({required String flavorName}) async {
    try {
      final file = File("./lib/json/flavor.json");
      final jsonString = await file.readAsString();
      final jsonData = jsonDecode(jsonString);

      if (jsonData is Map<String, dynamic> && jsonData['flavors'] is List) {
        jsonData['flavors'].add(flavorName);

        final updatedJsonString = jsonEncode(jsonData);
        await file.writeAsString(updatedJsonString);
        print('Flavor updated successfully.');
        return true;
      } else {
        print('Invalid JSON format.');
        return false;
      }
    } catch (e) {
      print('Error updating flavor: $e');
      return false;
    }
  }

  Future<Map<String, dynamic>> getFlavorBuilder() async {
    final file = File("./lib/json/flavor_builder.json");
    final jsonString = await file.readAsString();
    final jsonData = jsonDecode(jsonString);
    return jsonData;
  }

  Future<bool> updateFlavorBuilder({
    required String flavorName,
  }) async {
    try {
      final file = File("./lib/json/flavor_builder.json");

      final jsonString = await file.readAsString();
      final jsonData = jsonDecode(jsonString);

      // Update the JSON with new flavor data
      jsonData[flavorName] = {
        'permissions': '$flavorName.Permissions()',
        'deeplinks': '$flavorName.Deeplinks()',
        'appInfo': '$flavorName.Appinfo()',
        'flavorName': flavorName,
        'theme': '$flavorName.Theme()',
        'appSigning': '$flavorName.AppSigning()'
      };

      final updatedJsonString = jsonEncode(jsonData);
      await file.writeAsString(updatedJsonString);

      return true;
    } catch (e) {
      print('Error updating flavor: $e');
      return false;
    }
  }
}
