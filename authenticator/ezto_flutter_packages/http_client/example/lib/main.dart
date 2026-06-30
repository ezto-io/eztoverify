import 'package:flutter/material.dart';
import 'package:ezto_http_client/ezto_http_client.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  final _httpClientPlugin = HttpNativeClient();

  @override
  void initState() {
    super.initState();
    _httpClientPlugin
        .getUrl("https://api.ipify.org")
        .then(
          (value) => debugPrint(
            value.encode().toString(),
          ),
        )
        .catchError((exception) {
      debugPrint(exception.toString());
    });
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
        ),
        body: const Center(
          child: Text('Running on:\n'),
        ),
      ),
    );
  }
}
