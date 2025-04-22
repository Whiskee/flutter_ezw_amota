import 'dart:typed_data';

import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'flutter_ezw_amota_method_channel.dart';

abstract class FlutterEzwAmotaPlatform extends PlatformInterface {
  /// Constructs a FlutterEzwAmotaPlatform.
  FlutterEzwAmotaPlatform() : super(token: _token);

  static final Object _token = Object();

  static FlutterEzwAmotaPlatform _instance = MethodChannelFlutterEzwAmota();

  /// The default instance of [FlutterEzwAmotaPlatform] to use.
  ///
  /// Defaults to [MethodChannelFlutterEzwAmota].
  static FlutterEzwAmotaPlatform get instance => _instance;

  /// Platform-specific implementations should set this with their own
  /// platform-specific class that extends [FlutterEzwAmotaPlatform] when
  /// they register themselves.
  static set instance(FlutterEzwAmotaPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<String?> getPlatformVersion() {
    throw UnimplementedError('platformVersion() has not been implemented.');
  }

  /// 仅支持iOS，初始化OTA服务
  Future<void> initialize() {
    throw UnimplementedError('initialize() has not been implemented.');
  }

  /// 仅支持iOS，释放OTA服务
  Future<void> release() {
    throw UnimplementedError('release() has not been implemented.');
  }

  /// 开始升级
  /// 
  /// - filePath: 升级文件路径
  /// - serviceUuid: 服务UUID，仅iOS需要传递
  /// - readUuid: 读取UUID，仅iOS需要传递
  /// 
  Future<void> startOtaUpgrade(String filePath) {
    throw UnimplementedError('startOtaUpgrade() has not been implemented.');
  }

  /// 停止升级
  Future<void> stopOtaUpgrade() {
    throw UnimplementedError('stopOtaUpgrade() has not been implemented.');
  }

  /// 仅支持Android，指令回复
  Future<void> otaCmdResponse(Uint8List data) {
    throw UnimplementedError('otaCmdResponse() has not been implemented.');
  }
}
