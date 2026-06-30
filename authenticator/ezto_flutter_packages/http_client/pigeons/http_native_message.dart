import 'package:pigeon/pigeon.dart';

@ConfigurePigeon(PigeonOptions(
  dartOut: 'lib/src/http_native_message.g.dart',
  dartOptions: DartOptions(),
  kotlinOut:
      'android/src/main/kotlin/io/ezto/http/client/http_client/HttpNativeMessage.g.kt',
  kotlinOptions: KotlinOptions(),
  swiftOut: 'ios/Classes/HttpNativeMessage.g.swift',
  swiftOptions: SwiftOptions(),
  dartPackageName: 'http_client',
))
class HttpClientResponse {
  int statusCode;
  String body;
  HttpClientResponse({
    required this.statusCode,
    required this.body,
  });
}

@HostApi()
abstract class HttpNativeClientHostApi {
  @async
  HttpClientResponse getUrl(String url);
}
