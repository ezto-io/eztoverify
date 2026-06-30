import 'dart:io';
import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:path_provider/path_provider.dart';
import 'package:path/path.dart' as path;

class FileService {
  Future<String?> saveImage(Uint8List imageData) async {
    try {
      // Get the temporary directory of the device
      Directory tempDir = await getTemporaryDirectory();

      // Create a unique file name
      String fileName = '${DateTime.now().millisecondsSinceEpoch}.png';

      // Build the full file path
      String filePath = path.join(tempDir.path, fileName);

      // Write the data to the file
      File file = File(filePath);
      await file.writeAsBytes(imageData);

      return file.path;
    } catch (e) {
      debugPrint("Error saving image: $e");
      return null;
    }
  }
}
