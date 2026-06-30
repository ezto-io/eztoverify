import 'package:flutter/material.dart';
import 'package:ezto_fido/fido.dart';
import 'package:flutter/services.dart';

class EztoFidoPage extends StatelessWidget {
  const EztoFidoPage({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text("Ezto fido"),
      ),
      body: ListView(
        children: [
          ListTile(
            title: const Text("Register fido"),
            onTap: () async {
              try {
                final result = await EztoVerifyFido().registrationFido(
                  FidoRegRequest(
                      challenge: Uint8List(10),
                      rpEntityId: "example.com",
                      rpEntityName: "example.com",
                      userId: Uint8List(10),
                      userName: "userName",
                      userDisplayName: "userDisplayName",
                      pubKeyCredParams: [],
                      pubKeyIds: [-7, -257, -35, -258, -36, -259, -65535]),
                );
                debugPrint(result.encode().toString());
              } on PlatformException {
                debugPrint("paltform exception");
              } catch (e) {
                debugPrint("Error : $e");
              }
            },
          ),
          ListTile(
            title: const Text("Verify fido"),
            onTap: () async {
              final result = await EztoVerifyFido().verifyFido(
                FidoVerifyRequest(
                  challenge: Uint8List(10),
                  rpID: "example.com",
                  allowCredentials: [
                    AllowedCredentials(
                      id: "m5yy7neKY5CAlQhse2ting",
                      type: "public-key",
                      rawId: Uint8List(16),
                    )
                  ],
                ),
              );
              debugPrint(result.encode().toString());
            },
          ),
        ],
      ),
    );
  }
}
