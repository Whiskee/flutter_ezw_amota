import 'dart:io';
import 'dart:typed_data';

import 'package:flutter_ezw_amota/flutter_ezw_amota_even_channel.dart';

import 'flutter_ezw_amota_platform_interface.dart';

class FlutterEzwAmota {
  static final FlutterEzwAmota to = FlutterEzwAmota._();

  FlutterEzwAmota._();

  FlutterEzwAmotaPlatform get mc => FlutterEzwAmotaPlatform.instance;

  /// 获取当前平台
  Future<String?> getPlatformVersion() => mc.getPlatformVersion();

  /// 升级状态
  Stream<int> upgradeStatus = FlutterEzwAmotaEc.upgradeStatus.ec.map(
    (data) => data as int? ?? 0,
  );

  /// 升级进度
  Stream<int> upgradeProgress = FlutterEzwAmotaEc.upgradeProgress.ec.map(
    (data) => data as int? ?? 0,
  );

  /// 仅支持Android， OTA 升级指令处理
  Stream<Uint8List>? otaCmdHandle =
      Platform.isIOS
          ? null
          : FlutterEzwAmotaEc.otaCmdHandle.ec.map(
            (data) => Uint8List.fromList(data),
          );
}
