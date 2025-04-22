import 'package:flutter/services.dart';

enum FlutterEzwAmotaEc { 
  //  升级状态
  //  - SUCCESS = 0， 升级成功
  //  - CRC_ERROR = 1， CRC校验失败
  //  - INVALID_HEADER_INFO = 2， 无效文件头（头信息解析失败）
  //  - INVALID_PACKAGE_LENGTH -> 3，无效的数据包长度
  //  - INSUFFICIENT_BUFFER -> 4，缓存空间不足，无法写入新的固件数据，导致升级失败。
  //  - INSUFFICIENT_FLASH -> 5，闪存空间不足，无法写入新的固件数据，导致升级失败。
  //  - UNKNOWN_ERROR -> 6，物质错误
  //  - MAX -> 7，分块数超过最大限制
  //  - FILE_OPEN_ERROR -> 8，固件打开失败
  //  - CMD_SEND_ERROR -> 9，发送指令失败，仅Android适用
  upgradeStatus, 
  //  升级进度
  upgradeProgress, 
  //  处理升级指令，仅Android可用
  otaCmdHandle 
  }

extension FlutterEzwAmotaEcExt on FlutterEzwAmotaEc {
  static final List<(FlutterEzwAmotaEc, Stream<dynamic>)> _otaECs = [];

  Stream<dynamic> get ec {
    Stream? myEc;
    try {
      myEc = _otaECs.firstWhere((config) => config.$1 == this).$2;
    } catch (e) {
      myEc = null;
    }
    if (myEc != null) {
      return myEc;
    }
    final newEc = EventChannel(name).receiveBroadcastStream(name);
    _otaECs.add((this, newEc));
    return newEc;
  }
}