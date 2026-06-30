// import 'package:eztoverify/interface/js_handler.dart';
// import 'package:flutter_miracl_sdk/pigeon.dart';

// class MiraclHandler extends JsHandler {
//   final MiraclSdk sdk = MiraclSdk();
//   Future<void> initMiraclSdk({
//     required String projectId,
//     required String clientId,
//     required String redirectUri,
//   }) async {
//     final config = MConfiguration(
//       projectId: projectId,
//       clientId: clientId,
//       redirectUri: redirectUri,
//     );

//     await sdk.initSdk(config);
//   }

//   Future<Map<String, dynamic>> isUserRegistered(List<dynamic> args) async {
//     final data = processArgs(args);
//     final projectId = data["projectId"];
//     final clientId = data["clientId"];
//     final redirectUri = data["redirectUri"];
//     final userId = data["userId"];
//     await initMiraclSdk(
//       projectId: projectId,
//       clientId: clientId,
//       redirectUri: redirectUri,
//     );

//     try {
//       final registeredUsers = await sdk.getUsers();

//       final user = registeredUsers.singleWhere(
//           (element) => element?.userId == userId,
//           orElse: () => null);

//       return {
//         "success": true,
//         "message": "",
//         "isUserAvailable": user != null,
//         "projectId": projectId,
//         "revoked": user?.revoked,
//         "userId": userId,
//         "device": await getDeviceInfo(),
//       };
//     } catch (e) {
//       return {
//         "success": false,
//         "message": e.toString(),
//         "isUserAvailable": false,
//         "projectId": projectId,
//         "revoked": null,
//         "userId": userId,
//         "device": await getDeviceInfo(),
//       };
//     }
//   }

//   Future<Map<String, dynamic>> registerMiraclUser(List<dynamic> args) async {
//     final data = processArgs(args);
//     final projectId = data["projectId"];
//     final clientId = data["clientId"];
//     final redirectUri = data["redirectUri"];
//     final deeplinkUrl = data["deeplinkUrl"];
//     await initMiraclSdk(
//       projectId: projectId,
//       clientId: clientId,
//       redirectUri: redirectUri,
//     );

//     try {
//       final actResponse = await sdk.getActivationToken(deeplinkUrl);
//       final user = await sdk.register(
//           actResponse.userId, actResponse.activationToken, "0000", "");

//       return {
//         "success": true,
//         "message": "",
//         "projectId": projectId,
//         "revoked": user.revoked,
//         "userId": user.userId,
//         "device": await getDeviceInfo(),
//       };
//     } catch (e) {
//       return {
//         "success": false,
//         "message": e.toString(),
//         "projectId": projectId,
//         "revoked": null,
//         "userId": "",
//         "device": await getDeviceInfo(),
//       };
//     }
//   }

//   Future<Map<String, dynamic>> loginMiraclUser(List<dynamic> args) async {
//     final data = processArgs(args);
//     final projectId = data["projectId"];
//     final clientId = data["clientId"];
//     final redirectUri = data["redirectUri"];
//     final userId = data["userId"];
//     await initMiraclSdk(
//       projectId: projectId,
//       clientId: clientId,
//       redirectUri: redirectUri,
//     );

//     try {
//       final token = await sdk.authenticate(userId, "0000");
//       return {
//         "success": true,
//         "message": "success",
//         "authCode": token,
//         "device": await getDeviceInfo(),
//       };
//     } catch (e) {
//       return {
//         "success": false,
//         "message": e.toString(),
//         "authCode": null,
//         "device": await getDeviceInfo(),
//       };
//     }
//   }
// }
