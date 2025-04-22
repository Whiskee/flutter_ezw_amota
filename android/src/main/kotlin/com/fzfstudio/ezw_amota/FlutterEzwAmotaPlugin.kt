package com.fzfstudio.ezw_amota

import com.fzfstudio.ezw_amota.models.AmotaEvent
import com.fzfstudio.ezw_amota.service.AmOtaService
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler

/** FlutterEzwAmotaPlugin */
class FlutterEzwAmotaPlugin: FlutterPlugin, MethodCallHandler {
  /// The MethodChannel that will the communication between Flutter and native Android
  ///
  /// This local reference serves to register the plugin with the Flutter Engine and unregister it
  /// when the Flutter Engine is detached from the Activity
  private lateinit var channel : MethodChannel

  override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    channel = MethodChannel(flutterPluginBinding.binaryMessenger, "flutter_ezw_amota")
    channel.setMethodCallHandler(this)
    //  注册Event事件
    AmotaEvent.entries.forEach {
      it.registerEventChannel(flutterPluginBinding.binaryMessenger)
    }
  }

  override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
    if (call.method == "getPlatformVersion") {
      result.success("Android ${android.os.Build.VERSION.RELEASE}")
      return
    } else if (call.method == "startOtaUpgrade") {
      val params = call.arguments as Map<*, *>?
      val filePath = params?.get("filePath") as String? ?: ""
      AmOtaService.instance.amOtaStart(filePath)
    } else if (call.method == "stopOtaUpgrade") {
      AmOtaService.instance.amOtaStop()
    } else if (call.method == "otaCmdResponse") {
      val response = call.arguments as ByteArray
      AmOtaService.instance.otaCmdResponse(response)
    } else {
      result.notImplemented()
      return
    }
    result.success(null)
  }

  override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
    channel.setMethodCallHandler(null)
  }

}
