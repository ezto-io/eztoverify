import 'dart:convert';
import 'dart:io';

import 'package:eztoverify/model/pub_cred_model.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:eztoverify/interface/js_handler.dart';
import 'package:eztoverify/model/field_error_model.dart';
import 'package:ezto_fido/fido.dart';

class FidoHandler extends JsHandler {
  final EztoVerifyFido _eztoVerifyFido = EztoVerifyFido();

  Future<Map<String, dynamic>> register(List<dynamic> args) async {
    final params = processArgs(args);
    final data = params['params'];
    final missingFields = validateRequiredFields(
      data['publicKey'],
      ["challenge", "rp", "user", "pubKeyCredParams"],
    );
    if (missingFields.isNotEmpty) {
      return FieldErrorModel(
        device: await getDeviceInfo(),
      ).toMap();
    }
    debugPrint("args -> $data");
    final request = FidoRegRequest(
      challenge: convertAlgToUInt(data['publicKey']['challenge']),
      rpEntityId: data['publicKey']['rp']['id'],
      rpEntityName: data['publicKey']['rp']['name'],
      userId: convertAlgToUInt(data['publicKey']['user']['id']),
      userName: data['publicKey']['user']['name'],
      userDisplayName: data['publicKey']['user']['displayName'],
      pubKeyCredParams: data['publicKey']['pubKeyCredParams'],
      pubKeyIds: pubCredModelFromJson(
              jsonEncode(data['publicKey']['pubKeyCredParams']))
          .map((e) => e.alg)
          .toList(),
    );
    final result = await _eztoVerifyFido.registrationFido(request);
    debugPrint('result -> ${result.errorCode}');
    var json = result.toJson();
    json["device"] = await getDeviceInfo();
    json["errorCodes"] = mapErrors(result.errorCode);
    debugPrint('final result -> $json');
    return json;
  }

  Future<Map<String, dynamic>> isJsFidoSupported(List<dynamic> args) async {
    return {
      "success": true,
      "message": "",
      "isSupported": Platform.isWindows,
      "device": await getDeviceInfo(),
    };
  }

  Future<Map<String, dynamic>> auth(List<dynamic> args) async {
    debugPrint("----args--------$args");
    final params = processArgs(args);
    final data = params['params'];
    final missingFields = validateRequiredFields(data['publicKey'], [
      "challenge",
      "rpId",
    ]);

    if (missingFields.isNotEmpty) {
      return FieldErrorModel(
        device: await getDeviceInfo(),
      ).toMap();
    }

    final List<dynamic> allowCreds = data["publicKey"]["allowCredentials"];
    final List<AllowedCredentials> mappedAllowedCreds = allowCreds.map(
      (e) {
        final Map<String, dynamic> id = e["id"];
        return AllowedCredentials(
          id: base64UrlEncode(convertAlgToUInt(id)),
          type: e["type"],
          rawId: convertAlgToUInt(id),
        );
      },
    ).toList();

    final request = FidoVerifyRequest(
      challenge: convertAlgToUInt(data['publicKey']['challenge']),
      rpID: data['publicKey']['rpId'],
      allowCredentials: mappedAllowedCreds,
    );
    final result = (await _eztoVerifyFido.verifyFido(request));
    final json = result.toJson();
    json["device"] = await getDeviceInfo();
    json["errorCodes"] = mapErrors(result.errorCode);
    debugPrint("Final Result -> $json");
    return json;
  }

  Uint8List convertAlgToUInt(Map<String, dynamic> input) {
    Uint8List uint8List = Uint8List.fromList(
      List.generate(input.length, (index) => input[index.toString()] ?? 0),
    );
    return uint8List;
  }
}
