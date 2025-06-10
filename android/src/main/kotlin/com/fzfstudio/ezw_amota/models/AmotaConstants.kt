package com.fzfstudio.ezw_amota.models

/**
 * OTA 协议常量定义
 */
object AmotaConstants {
    /// 数据包结构相关
    //  - 固件包总大小 = 数据段(512) + 头信息(16)
    const val AMOTA_PACKET_SIZE = 512 + 16
    //  - 固件数据段大小（字节）
    const val AMOTA_FW_PACKET_SIZE = 4096
    //  - BLE单次传输最大载荷（字节）
    const val MAXIMUM_APP_PAYLOAD = 240

    /// 协议字段长度定义
    //  - 长度字段占字节数
    const val AMOTA_LENGTH_SIZE_IN_PKT = 2
    //  - 命令类型字段占字节数
    const val AMOTA_CMD_SIZE_IN_PKT = 1
    //  - CRC校验字段占字节数
    const val AMOTA_CRC_SIZE_IN_PKT = 4
    //  - 协议头总长度 = 长度字段 + 命令字段
    const val AMOTA_HEADER_SIZE_IN_PKT = AMOTA_LENGTH_SIZE_IN_PKT + AMOTA_CMD_SIZE_IN_PKT
}