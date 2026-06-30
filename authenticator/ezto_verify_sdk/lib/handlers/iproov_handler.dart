// import 'package:eztoverify/enums/status.dart';
// import 'package:eztoverify/interface/js_handler.dart';
// import 'package:eztoverify/model/field_error_model.dart';
// import 'package:ezto_iproov/ezto_iproov.dart';
// import 'package:permission_handler/permission_handler.dart';

// class IproovHandler extends JsHandler {
//   @override
//   List<Permission> get requiredPermissions => [Permission.camera];

//   Future<Map<String, dynamic>> register(List<dynamic> args) {
//     return _init(args);
//   }

//   Future<Map<String, dynamic>> verify(List<dynamic> args) {
//     return _init(args);
//   }

//   Future<Map<String, dynamic>> _init(List<dynamic> args) async {
//     final IproovService iproovService = IproovService();

//     final data = processArgs(args);
//     final missingFields = validateRequiredFields(
//       data,
//       ["streamingUrl", "token"],
//     );
//     if (missingFields.isNotEmpty) {
//       return FieldErrorModel(
//         device: await getDeviceInfo(),
//       ).toMap();
//     }

//     List<Status> permissions = await checkForPermissions();
//     if (permissions.isNotEmpty) {
//       return FieldErrorModel(
//         message: "camera permission required",
//         errorCodes: [Status.osCameraPermission],
//         device: await getDeviceInfo(),
//       ).toMap();
//     }

//     final result = await iproovService.iproovInit(
//       streamingUrl: data['streamingUrl'],
//       token: data['token'],
//     );
//     final json = result.toJson(device: await getDeviceInfo());
//     return json;
//   }
// }
