import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'http_client_method_channel.dart';

abstract class HttpClientPlatform extends PlatformInterface {
  /// Constructs a HttpClientPlatform.
  HttpClientPlatform() : super(token: _token);

  static final Object _token = Object();

  static HttpClientPlatform _instance = MethodChannelHttpClient();

  /// The default instance of [HttpClientPlatform] to use.
  ///
  /// Defaults to [MethodChannelHttpClient].
  static HttpClientPlatform get instance => _instance;

  /// Platform-specific implementations should set this with their own
  /// platform-specific class that extends [HttpClientPlatform] when
  /// they register themselves.
  static set instance(HttpClientPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<String?> getPlatformVersion() {
    throw UnimplementedError('platformVersion() has not been implemented.');
  }
}
