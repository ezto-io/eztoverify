import 'dart:convert';

import 'package:eztoverify/enums/push_support.dart';

class GetTokenModel {
  final String pushToken;
  final PushSupport pushType;

  GetTokenModel({
    required this.pushToken,
    required this.pushType,
  });

  Map<String, dynamic> toMap() {
    return {
      'pushToken': pushToken,
      'pushType': pushType.name,
    };
  }

  String toJson() => json.encode(toMap());
}
