import 'package:flutter/material.dart';
import 'package:implementation_tester/pages/ezto_biometric_page.dart';
import 'package:implementation_tester/pages/ezto_fido_page.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  // This widget is the root of your application.
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Flutter Demo',
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.deepPurple),
        useMaterial3: true,
      ),
      home: const HomePage(),
    );
  }
}

class HomePage extends StatelessWidget {
  const HomePage({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text("Test"),
      ),
      body: ListView(
        children: [
          ListTile(
            title: const Text("Fido"),
            onTap: () {
              Navigator.of(context).push(
                MaterialPageRoute<void>(
                  builder: (BuildContext context) => const EztoFidoPage(),
                ),
              );
            },
          ),
          ListTile(
            title: const Text("Biometric"),
            onTap: () {
              Navigator.of(context).push(
                MaterialPageRoute<void>(
                  builder: (BuildContext context) => const EztoBiometricPage(),
                ),
              );
            },
          ),
        ],
      ),
    );
  }
}
