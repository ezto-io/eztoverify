// import 'dart:typed_data';
// import 'package:flutter/material.dart';
// import 'package:kobil_pass/kobil_pass.dart';
// import 'package:eztoverify/service/local_storage.dart';
// import 'package:eztoverify/model/nfc_result_model.dart';
// import 'package:eztoverify/utils/data_conversion.dart';
// import 'package:eztoverify/widget/nfc_bottom_sheet_widget.dart';

// class NfcService {
//   KobilPass kPass = KobilPass();

//   Future<dynamic> authentication(
//       {String? tagId, Uint8List? challenge, BuildContext? context}) async {
//     List<NfcResultModel>? result =
//         await LocalStorageService.getListFromJsonSharedPreferences();

//     String privateKey =
//         result!.firstWhere((element) => element.tagId == tagId).privateKey;
//     Uint8List localPrivateKey = DataConversion.converHexToUint8List(privateKey);

//     NfcBottomSheetWidget.nfcBottomSheet(context!);
//     final authResult = await kPass.authenticate(
//         privateKey: localPrivateKey, challenge: challenge!);
//     if (context.mounted) Navigator.pop(context);
//     if (authResult.isSuccess()) {
//       String newTagId =
//           DataConversion.convertUint8ListToHex(authResult.data!.tagId);
//       String signedChallenge = DataConversion.convertUint8ListToHex(
//           authResult.data!.signedChallenge);
//       return {'tagId': newTagId, 'signedChallenge': signedChallenge};
//     } else {
//       final String result = authResult.result.name;
//       return result;
//     }
//   }

//   Future<dynamic> newRegister() async {
//     Uint8List privateKey = Uint8List(16);
//     // Uint8List newKey = await kPass.generatekey();
//     Uint8List newKey = Uint8List(16);

//     final changeKeyResult = await kPass.changeMasterKey(
//       currentKey: privateKey,
//       newKey: newKey,
//     );
//     if (changeKeyResult.isSuccess()) {
//       String tagId =
//           DataConversion.convertUint8ListToHex(changeKeyResult.data!.tagId);
//       String convertNewKey = DataConversion.convertUint8ListToHex(newKey);
//       NfcResultModel result = NfcResultModel(tagId, convertNewKey);
//       LocalStorageService.addJsonToSharedPreferences(result);
//       return {'tagId': tagId, 'privateKey': convertNewKey};
//     } else {
//       final result = changeKeyResult.result.name;
//       return result;
//     }
//   }

//   Future<dynamic> changeKey({required String tagId}) async {
//     List<NfcResultModel>? result =
//         await LocalStorageService.getListFromJsonSharedPreferences();
//     String privateKey =
//         result!.firstWhere((element) => element.tagId == tagId).privateKey;
//     Uint8List localPrivateKey = DataConversion.converHexToUint8List(privateKey);
//     Uint8List newKey = Uint8List(16);
//     final changeKeyResult = await kPass.changeMasterKey(
//       currentKey: localPrivateKey,
//       newKey: newKey,
//     );
//     if (changeKeyResult.isSuccess()) {
//       LocalStorageService.removeJsonFromSharedPreferences(tagId: tagId);
//       String newTagId =
//           DataConversion.convertUint8ListToHex(changeKeyResult.data!.tagId);
//       String convertNewKey = DataConversion.convertUint8ListToHex(newKey);
//       return {'tagId': newTagId, 'privateKey': convertNewKey};
//     } else {
//       final result = changeKeyResult.result.name;
//       return result;
//     }
//   }
// }
