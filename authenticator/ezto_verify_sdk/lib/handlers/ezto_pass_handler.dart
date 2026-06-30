// import 'dart:convert';

// import 'package:eztoverify/enums/status.dart';
// import 'package:eztoverify/interface/js_handler.dart';
// import 'package:flutter/cupertino.dart';
// import 'package:flutter_rust_nfc/model/ndef_result.dart';
// import 'package:flutter_rust_nfc/nfc_pass.dart';
// import 'package:flutter_rust_nfc/src/rust/api/models.dart';

// class EztoPassHandler extends JsHandler {
//   Future<Map<String, dynamic>> register(
//     BuildContext context,
//     List<dynamic> args,
//   ) async {
//     final data = processArgs(args);

//     final missingRequiredFields = validateRequiredFields(data, [
//       "currentKey",
//       "newKey",
//     ]);

//     if (missingRequiredFields.isNotEmpty) {
//       return {
//         "success": false,
//         "message": "Required fields missing",
//         "errorCodes": [Status.jsMissingFields.name],
//         "device": await getDeviceInfo(),
//       };
//     }

//     final String currentKey = data["currentKey"];
//     final String newKey = data["newKey"];

//     final nfcPass = await NfcPass.init();
//     final result = await nfcPass.changeMasterKey(
//       context: context,
//       currentKey: base64Decode(currentKey),
//       newKey: base64Decode(newKey),
//     );

//     return {
//       "success": result.result == NfcResult.success,
//       "message": "",
//       "errorCodes":
//           result.result == NfcResult.success ? [] : [result.result.name],
//       "device": await getDeviceInfo(),
//       "tagId": result.tagId == null ? "" : base64Encode(result.tagId!)
//     };
//   }

//   Future<Map<String, dynamic>> createSdm(
//     BuildContext context,
//     List<dynamic> args,
//   ) async {
//     final data = processArgs(args);

//     final missingRequiredFields = validateRequiredFields(data, [
//       "privateKey",
//       "baseUrl",
//     ]);

//     if (missingRequiredFields.isNotEmpty) {
//       return {
//         "success": false,
//         "message": "Required fields missing",
//         "errorCodes": [Status.jsMissingFields.name],
//         "device": await getDeviceInfo(),
//       };
//     }

//     final String privateKey = data["privateKey"];
//     final String baseUrl = data["baseUrl"];

//     final nfcPass = await NfcPass.init();
//     final result = await nfcPass.createSdm(
//       context: context,
//       privateKey: base64Decode(privateKey),
//       baseUrl: baseUrl,
//     );

//     return {
//       "success": result.result == NfcResult.success,
//       "errorCodes":
//           result.result == NfcResult.success ? [] : [result.result.name],
//       "message": "",
//       "device": await getDeviceInfo(),
//     };
//   }

//   Future<Map<String, dynamic>> getSdmNdef(
//     BuildContext context,
//     List<dynamic> args,
//   ) async {
//     final nfcPass = await NfcPass.init();
//     final NdefResult result = await nfcPass.getSdmNdef(context: context);

//     return {
//       "success": result.result == NfcResult.success,
//       "errorCodes":
//           result.result == NfcResult.success ? [] : [result.result.name],
//       "url": result.url,
//       "picc": result.picc,
//       "mac": result.mac,
//       "device": await getDeviceInfo(),
//     };
//   }
// }
