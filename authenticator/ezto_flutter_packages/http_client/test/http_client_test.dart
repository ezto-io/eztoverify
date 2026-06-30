import 'package:flutter_test/flutter_test.dart';
import 'package:ezto_http_client/http_client_platform_interface.dart';
import 'package:ezto_http_client/http_client_method_channel.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

class MockHttpClientPlatform
    with MockPlatformInterfaceMixin
    implements HttpClientPlatform {
  @override
  Future<String?> getPlatformVersion() => Future.value('42');
}

void main() {
  final HttpClientPlatform initialPlatform = HttpClientPlatform.instance;

  test('$MethodChannelHttpClient is the default instance', () {
    expect(initialPlatform, isInstanceOf<MethodChannelHttpClient>());
  });

  test('getPlatformVersion', () async {
    // HttpClient httpClientPlugin = HttpClient();
    // MockHttpClientPlatform fakePlatform = MockHttpClientPlatform();
    // HttpClientPlatform.instance = fakePlatform;

    // expect(await httpClientPlugin.getPlatformVersion(), '42');
  });
}
