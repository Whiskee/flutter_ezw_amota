import Flutter
import UIKit
import amOtaApi

public class FlutterEzwAmotaPlugin: NSObject, FlutterPlugin {
    
    
    
    public static func register(with registrar: FlutterPluginRegistrar) {
        let instance = FlutterEzwAmotaPlugin()
        //  MethodChannel
        let methodChannel = FlutterMethodChannel(name: "flutter_ezw_amota", binaryMessenger: registrar.messenger())
        registrar.addMethodCallDelegate(instance, channel: methodChannel)
        //  EvenChannel
        AmotaEC.allCases.forEach { child in
            child.registerEventChannel(registrar: registrar, streamHandler: instance)
        }
    }
    
    public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
        AmotaMC(rawValue: call.method)?.handle(arguments: call.arguments, result: result)
    }
    
}
