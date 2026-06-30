import 'package:pigeon/pigeon.dart';

@ConfigurePigeon(
  PigeonOptions(
    dartOut: 'lib/src/fido_messages.g.dart',
    kotlinOut: 'android/src/main/kotlin/io/ezto/verify/fido/FidoMessages.g.kt',
    kotlinOptions: KotlinOptions(package: "io.ezto.verify.fido"),
    swiftOut: 'darwin/Classes/FidoMessages.g.swift',
  ),
)
class AllowedCredentials {
  final String id;
  final String type;
  final Uint8List rawId;

  const AllowedCredentials({
    required this.id,
    required this.type,
    required this.rawId,
  });
}

//------Register Response Model---------
class FidoRegResponse {
  bool success;
  String? message;
  String? data;
  String? errorCode;

  FidoRegResponse(
      {required this.success, this.message, this.data, this.errorCode});
}

//------Verify Response Model-----------

class FidoVerifyResponse {
  bool success;
  String? message;
  String? data;
  String? errorCode;

  FidoVerifyResponse(
      {required this.success, this.message, this.data, this.errorCode});
}

class FidoRegRequest {
  Uint8List challenge;
  String rpEntityId;
  String rpEntityName;
  Uint8List userId;
  String userName;
  String userDisplayName;
  List<Object?> pubKeyCredParams;
  List<int?> pubKeyIds;
  FidoRegRequest({
    required this.challenge,
    required this.rpEntityId,
    required this.rpEntityName,
    required this.userId,
    required this.userName,
    required this.userDisplayName,
    required this.pubKeyCredParams,
    required this.pubKeyIds,
  });
}

class FidoVerifyRequest {
  final Uint8List challenge;
  final String rpID;
  final List<AllowedCredentials?> allowCredentials;
  FidoVerifyRequest({
    required this.challenge,
    required this.rpID,
    required this.allowCredentials,
  });
}

@HostApi()
abstract class FidoApi {
  @async
  FidoRegResponse registrationFido(FidoRegRequest request);

  @async
  FidoVerifyResponse verifyFido(FidoVerifyRequest request);
}
