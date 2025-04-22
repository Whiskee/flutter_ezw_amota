import 'package:flutter_ezw_amota/flutter_ezw_amota_method_channel.dart';
import 'package:flutter_ezw_amota/flutter_ezw_amota_platform_interface.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

class MockFlutterEzwAmotaPlatform
    with MockPlatformInterfaceMixin
    implements FlutterEzwAmotaPlatform {
  @override
  Future<String?> getPlatformVersion() => Future.value('42');

  @override
  Future<void> initialize() async {}

  @override
  Future<void> release() async {}

  @override
  Future<void> startOtaUpgrade(String filePath, {String? uuid}) async {}

  @override
  Future<void> stopOtaUpgrade() async {}

  @override
  Future<void> otaCmdResponse(List<int> data) async {}
}

void main() {
  final FlutterEzwAmotaPlatform initialPlatform =
      FlutterEzwAmotaPlatform.instance;

  test('$MethodChannelFlutterEzwAmota is the default instance', () {
    expect(initialPlatform, isInstanceOf<MethodChannelFlutterEzwAmota>());
  });

  test('getPlatformVersion', () async {
    // FlutterEzwAmota flutterEzwAmotaPlugin = FlutterEzwAmota.to;
    // MockFlutterEzwAmotaPlatform fakePlatform = MockFlutterEzwAmotaPlatform();
    // FlutterEzwAmotaPlatform.instance = fakePlatform;
    // expect(await flutterEzwAmotaPlugin.getPlatformVersion(), '42');
  });
}
