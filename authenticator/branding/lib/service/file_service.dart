import 'dart:convert';
import 'dart:io';

class FileService {
  Future<bool> saveToFile(String path, String content) async {
    final outputFile = await File(path).create(recursive: true);
    var sink = outputFile.openWrite();
    sink.write(content);
    await sink.flush();
    await sink.close();
    return true;
  }

  Future<bool> copyFile(String source, String dest) async {
    final sourceFile = await File(source).create();
    await sourceFile.copy(dest);
    return true;
  }

  Future<Map<String, dynamic>?> loadJsonData(String path) async {
    try {
      final file = File(path);
      final jsonString = await file.readAsString();
      return jsonDecode(jsonString);
    } catch (e) {
      return null;
    }
  }

  Future<void> copyDirectory(
    Directory source,
    Directory destination,
  ) async {
    if (!await destination.exists()) {
      await destination.create(recursive: true);
    }

    await for (var entity in source.list(recursive: false)) {
      var newPath = destination.path +
          Platform.pathSeparator +
          entity.path.split(Platform.pathSeparator).last;
      if (entity is Directory) {
        await copyDirectory(entity, Directory(newPath));
      } else if (entity is File) {
        await entity.copy(newPath);
      }
    }
  }
}
