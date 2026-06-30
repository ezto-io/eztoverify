import 'package:ezto_http_client/src/http_native_message.g.dart';

class HttpNativeClient {
  final HttpNativeClientHostApi _client = HttpNativeClientHostApi();

  Future<HttpClientResponse> getUrl(String url) {
    return _client.getUrl(url);
  }
}
