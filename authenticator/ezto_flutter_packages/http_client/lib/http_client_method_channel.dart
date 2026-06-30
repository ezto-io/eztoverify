import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'http_client_platform_interface.dart';

/// An implementation of [HttpClientPlatform] that uses method channels.
class MethodChannelHttpClient extends HttpClientPlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('http_client');

  @override
  Future<String?> getPlatformVersion() async {
    final version = await methodChannel.invokeMethod<String>('getPlatformVersion');
    return version;
  }
}
