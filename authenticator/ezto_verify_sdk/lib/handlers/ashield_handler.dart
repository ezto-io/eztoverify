import 'package:eztoverify/enums/error_code.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:eztoverify/interface/js_handler.dart';
// import 'package:eztoverify/model/field_error_model.dart';
// import 'package:eztoverify/model/permission_error_model.dart';
// import 'package:ezto_ashield/ezto_verify_ashield.dart';

class AshieldHandler extends JsHandler {
  // final EztoVerifyAshield _eztoVerifyAshield = EztoVerifyAshield();

  Future<Map<String, dynamic>> register(List<dynamic> args) async {
    // final data = processArgs(args);
    // final missingFields = validateRequiredFields(data, [
    //   "mid",
    //   "regTransactionId",
    //   "regSignature",
    //   "authTransactionId",
    //   "authSignature",
    // ]);

    // if (missingFields.isNotEmpty) {
    //   return FieldErrorModel(
    //     device: await getDeviceInfo(),
    //   ).toMap();
    // }

    // final failedPermissions = await checkForPermissions();
    // if (failedPermissions.isNotEmpty) {
    //   return PermissionErrorModel(
    //     device: await getDeviceInfo(),
    //     errorCodes: failedPermissions,
    //   ).toMap();
    // }
    // final request = AshieldRegRequest(
    //   mid: data["mid"],
    //   regTransactionId: data["regTransactionId"],
    //   regSignature: data["regSignature"],
    //   authTransactionId: data["authTransactionId"],
    //   authSignature: data["authSignature"],
    //   authFlow: 'No',
    //   selectedSim: 0,
    // );
    // final result = await _eztoVerifyAshield.registrationAshield(request);
    Map<String, dynamic> json = {};
    json["success"] = false;
    json["device"] = await getDeviceInfo();
    json["errorCodes"] = ErrorCode.unsupported;
    return json;
  }

  Future<Map<String, dynamic>> auth(List<dynamic> args) async {
    // final data = processArgs(args);
    // final missingFields = validateRequiredFields(data, [
    //   "mid",
    //   "authTransactionId",
    //   "authSignature",
    // ]);

    // if (missingFields.isNotEmpty) {
    //   return FieldErrorModel(
    //     device: await getDeviceInfo(),
    //   ).toMap();
    // }
    // final failedPermissions = await checkForPermissions();
    // if (failedPermissions.isNotEmpty) {
    //   return PermissionErrorModel(
    //     errorCodes: failedPermissions,
    //     device: await getDeviceInfo(),
    //   ).toMap();
    // }
    // final request = AshieldAuthRequest(
    //   mid: data["mid"],
    //   authTransactionId: data["authTransactionId"],
    //   authSignature: data["authSignature"],
    //   authFlow: 'Yes',
    //   selectedSim: 0,
    // );

    // final result = (await _eztoVerifyAshield.authAshield(request));
    // final json = result.toJson();
    // json["device"] = await getDeviceInfo();
    // json["errorCodes"] = mapErrors(result.errorCode);
    // return json;

    Map<String, dynamic> json = {};
    json["success"] = false;
    json["device"] = await getDeviceInfo();
    json["errorCodes"] = ErrorCode.unsupported;
    return json;
  }

  @override
  List<Permission> get requiredPermissions => [];

  // @override
  // List<Permission> get requiredPermissions => [
  //       Permission.sms,
  //       Permission.phone,
  //     ];
}
