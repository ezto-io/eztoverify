import 'package:plugin_platform_interface/plugin_platform_interface.dart';

abstract class EztoPlatformInterface extends PlatformInterface {
  EztoPlatformInterface() : super(token: _token);

  static final Object _token = Object();

  static late EztoPlatformInterface _instance;

  static EztoPlatformInterface get instance => _instance;

  static set instance(EztoPlatformInterface instance) {
    PlatformInterface.verify(instance, _token);
    _instance = instance;
  }

  Future<Map<String, dynamic>> executeHandler(
    String functionName,
    Map<String, dynamic> data,
  );

  Map<String, dynamic> get availableHandlers;
}
