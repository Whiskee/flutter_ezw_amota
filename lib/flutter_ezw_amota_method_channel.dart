import 'dart:io';

import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'flutter_ezw_amota_platform_interface.dart';

/// An implementation of [FlutterEzwAmotaPlatform] that uses method channels.
class MethodChannelFlutterEzwAmota extends FlutterEzwAmotaPlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel("flutter_ezw_amota");

  @override
  Future<String?> getPlatformVersion() async {
    final version = await methodChannel.invokeMethod<String>(
      'getPlatformVersion',
    );
    return version;
  }

  /// 仅支持iOS，初始化OTA服务
  @override
  Future<void> initialize() async =>
      Platform.isAndroid
          ? await Future<void>.value()
          : await methodChannel.invokeMethod('initialize');

  /// 仅支持iOS，释放OTA服务
  @override
  Future<void> release() async =>
      Platform.isAndroid
          ? await Future<void>.value()
          : await methodChannel.invokeMethod('release');

  /// 开始OTA升级
  @override
  Future<void> startOtaUpgrade(String filePath, {String? uuid}) async =>
      await methodChannel.invokeMethod('startOtaUpgrade', {
        "filePath": filePath,
        "uuid": uuid,
      });

  /// 开始OTA升级
  @override
  Future<void> stopOtaUpgrade() async =>
      await methodChannel.invokeMethod('stopOtaUpgrade');

  /// 仅支持Android， OTA指令回复处理
  @override
  Future<void> otaCmdResponse(Uint8List data) async =>
      Platform.isIOS
          ? await Future<void>.value()
          : await methodChannel.invokeMethod('otaCmdResponse', data);
}
