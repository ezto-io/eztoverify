import 'dart:convert';

import 'package:ezto_fido/src/fido_messages.g.dart';
export 'package:ezto_fido/src/fido_messages.g.dart';

class EztoVerifyFido {
  final _api = FidoApi();

  Future<FidoRegResponse> registrationFido(FidoRegRequest request) {
    return _api.registrationFido(request);
  }

  Future<FidoVerifyResponse> verifyFido(FidoVerifyRequest request) {
    return _api.verifyFido(request);
  }
}

extension FidoRegResponseJson on FidoRegResponse {
  Map<String, dynamic> toJson() {
    return {
      "success": success,
      "message": message,
      "data": data != null ? jsonDecode(data!) : null
    };
  }
}

extension FidoVerifyResponseJson on FidoVerifyResponse {
  Map<String, dynamic> toJson() {
    return {
      "success": success,
      "message": message,
      "data": data != null ? jsonDecode(data!) : null
    };
  }
}
