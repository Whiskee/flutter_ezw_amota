package com.fzfstudio.ezw_amota.models

import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.EventChannel

/**
 * OTA Event事件
 */
enum class AmotaEvent {
    //  升级状态
    UPGRADE_STATUS,
    //  升级进度
    UPGRADE_PROGRESS,
    //  写入指令
    OTA_CMD_HANDLE;

    companion object {
        //  保存所有的EventChannel
        private val otaSinks = mutableMapOf<String, EventChannel.EventSink>()
    }

    //  - 事件Key
    val eventKey: String
        get() = when (this) {
            UPGRADE_STATUS -> "upgradeStatus"
            UPGRADE_PROGRESS -> "upgradeProgress"
            OTA_CMD_HANDLE -> "otaCmdHandle"
        }

    // - 获取EventSink
    val sink: EventChannel.EventSink?
        get() = otaSinks[this.eventKey]

    /**
     * 注册EventChannel
     */
    fun registerEventChannel(binaryMessenger: BinaryMessenger) {
        val label = this.eventKey
        EventChannel(binaryMessenger, label).setStreamHandler(
            object : EventChannel.StreamHandler {
                override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
                    events?.let { sink -> otaSinks[label] = sink  }
                }
                override fun onCancel(arguments: Any?) {
                    otaSinks.remove(label)
                }
            }
        )
    }
}