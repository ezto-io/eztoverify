import 'package:ezto_device_info/ezto_device_info.dart';
import 'package:eztoverify/interface/js_handler.dart';
import 'package:eztoverify/service/service_locator.dart';
import 'package:ezto_silent_binding/ezto_silent_binding.dart';

class SilentBindingHandler extends JsHandler {
  final silentBindingService = SilentBindingService();

  Future<Map<String, dynamic>> registerSilentBinding(List<dynamic> args) async {
    final data = processArgs(args);
    final remotePublicKey = data["remotePublicKey"].toString();
    final bindingId = await sl<DeviceInfoService>().getBindingId();

    final result = await silentBindingService.registerSilentBinding(
      publicKey: remotePublicKey,
      bindingId: bindingId,
    );

    final json = result.toJson(device: await getDeviceInfo());

    return json;
  }

  Future<Map<String, dynamic>> verifySilentBinding(List<dynamic> args) async {
    final data = processArgs(args);
    final challenge = data["challenge"].toString();
    final bindingId = await sl<DeviceInfoService>().getBindingId();

    final result = await silentBindingService.verifySilentBinding(
      challenge: challenge,
      bindingId: bindingId,
    );

    final json = result.toJson(device: await getDeviceInfo());

    return json;
  }
}
