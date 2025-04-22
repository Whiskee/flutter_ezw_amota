//
//  AmotaEc.swift
//  flutter_ezw_amota
//
//  Created by Whiskee on 2025/4/18.
//

import Flutter

typealias EvenConnectStreamHandler = NSObject & FlutterStreamHandler

/// Event Channel 事件存储
private var amotaEvents: Dictionary<String, FlutterEventSink> = [:]

/// Method Channel
enum AmotaMC: String {
    case getPlatformVersion
    //  初始化/释放OTA升级服务
    case initialize
    case release
    //  开启OTA升级
    case startOtaUpgrade
    //  关闭OTA升级
    case stopOtaUpgrade
    
    /**
     *  处理回调结果
     */
    func handle(arguments: Any?,  result: @escaping FlutterResult) {
        switch self {
        case .getPlatformVersion:
            result("iOS " + UIDevice.current.systemVersion)
            return
        case .initialize:
            AmOtaService.shared.initialize()
            break
        case .release:
            AmOtaService.shared.release()
            break
        case .startOtaUpgrade:
            let jsonData: [String:Any] = arguments as? [String:Any] ?? [:]
            let uuid = jsonData["uuid"] as? String ?? ""
            let filePath = jsonData["filePath"] as? String ?? ""
            AmOtaService.shared.startOtaUpgrade(uuid: uuid, filePath: filePath)
            break
        case .stopOtaUpgrade:
            AmOtaService.shared.stopOtaUpgrade()
            break
        }
        result(nil)
    }
}


/// Event Channel
enum AmotaEC: String, CaseIterable {
    
    //  升级状态
    case upgradeStatus
    //  升级进度
    case upgradeProgress
    
    /**
     *  注册EventChannel
     */
    func registerEventChannel(registrar: FlutterPluginRegistrar, streamHandler: EvenConnectStreamHandler) {
        let eventChannel = FlutterEventChannel(name: rawValue, binaryMessenger: registrar.messenger())
        eventChannel.setStreamHandler(streamHandler)
    }
    
    /**
     *  获取event
     */
    var event: FlutterEventSink? {
        get {
            guard amotaEvents.contains(where: { (key, _) in
                key == rawValue
            }) else {
                return nil
            }
            return amotaEvents[rawValue]
        }
    }
}


/// 事件频道信息流处理对象
extension FlutterEzwAmotaPlugin: FlutterStreamHandler {
    /**
     *  接收监听事件
     *  - 说明：Flutter层创建EventChannel时必须在receiveBroadcastStream中添加接收对象的Tag，即：EventChannel(tag).receiveBroadcastStream(tag)，否则arguments永远为空
     */
    public func onListen(withArguments arguments: Any?, eventSink events: @escaping FlutterEventSink) -> FlutterError? {
        guard let eventName = arguments as? String else {
            return nil
        }
        amotaEvents[eventName] = events
        return nil
    }
    
    public func onCancel(withArguments arguments: Any?) -> FlutterError? {
        guard let eventName = arguments as? String else {
            return nil
        }
        amotaEvents.removeValue(forKey: eventName)
        return nil
    }
    
}
