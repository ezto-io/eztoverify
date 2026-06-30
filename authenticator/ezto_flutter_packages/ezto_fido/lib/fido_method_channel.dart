import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'fido_platform_interface.dart';

/// An implementation of [FidoPlatform] that uses method channels.
class MethodChannelFido extends FidoPlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('fido');

  @override
  Future<String?> getPlatformVersion() async {
    final version = await methodChannel.invokeMethod<String>('getPlatformVersion');
    return version;
  }
}
