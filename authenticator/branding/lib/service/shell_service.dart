import 'dart:convert';
import 'dart:io';

import 'package:chalkdart/chalk.dart';

Future<bool> runShellCommand(String command, List<String> arguments) async {
  try {
    // Run the command
    Process process = await Process.start(
      command,
      arguments,
      workingDirectory: './../',
      runInShell: true,
    );

    // Listen to stdout
    process.stdout.transform(utf8.decoder).listen((data) {
      print(chalk.blue.bold(data));
    });

    process.stderr.transform(utf8.decoder).listen((data) {
      print(chalk.red.bold(data));
    });

    int exitCode = await process.exitCode;
    if (exitCode != 0) {
      print('Command failed with exit code $exitCode');
      return false;
    }

    return true;
  } catch (e) {
    print('Error: $e');
    return false;
  }
}
