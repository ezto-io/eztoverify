import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'fido_method_channel.dart';

abstract class FidoPlatform extends PlatformInterface {
  /// Constructs a FidoPlatform.
  FidoPlatform() : super(token: _token);

  static final Object _token = Object();

  static FidoPlatform _instance = MethodChannelFido();

  /// The default instance of [FidoPlatform] to use.
  ///
  /// Defaults to [MethodChannelFido].
  static FidoPlatform get instance => _instance;

  /// Platform-specific implementations should set this with their own
  /// platform-specific class that extends [FidoPlatform] when
  /// they register themselves.
  static set instance(FidoPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<String?> getPlatformVersion() {
    throw UnimplementedError('platformVersion() has not been implemented.');
  }
}
