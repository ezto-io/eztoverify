import 'package:flutter/material.dart';
import 'package:ezto_biometric/ezto_biometric.dart';

class EztoBiometricPage extends StatelessWidget {
  const EztoBiometricPage({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: ListView(
        children: [
          ListTile(
            title: const Text("Request Biometric"),
            onTap: () async {
              final result = await BiometricService().verify();
              debugPrint(result.toString());
            },
          ),
        ],
      ),
    );
  }
}
