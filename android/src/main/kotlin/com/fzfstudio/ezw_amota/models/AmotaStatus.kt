package com.fzfstudio.ezw_amota.models

/**
 * OTA 升级状态
 */
enum class AmotaStatus(status: Int) {

    //  升级成功
    UPGRADE_SUCCESS(0),
    //  CRC校验失败
    CRC_ERROR(1),
    //  无效的头信息
    INVALID_HEADER_INFO(2),
    //  无效的包长
    INVALID_PACKAGE_LENGTH(3),
    //  缓冲区空间不足
    INSUFFICIENT_BUFFER(4),
    //  Flash异常
    INSUFFICIENT_FLASH(5),
    //  未知错误
    UNKNOWN_ERROR(6),
    //  包长超出最大值
    MAX(7),
    //  文件读取失败
    FILE_READ_ERROR(8),
    //  指令发送失败
    CMD_SEND_ERROR(9),
    //  升级中
    UPGRADING(10),
    //  升级停止
    UPGRADE_STOP(11);

    val code: Int
        get() = when(this) {
            CRC_ERROR -> 1
            INVALID_HEADER_INFO -> 2
            INVALID_PACKAGE_LENGTH -> 3
            INSUFFICIENT_BUFFER -> 4
            INSUFFICIENT_FLASH -> 5
            UNKNOWN_ERROR -> 6
            MAX -> 7
            FILE_READ_ERROR -> 8
            CMD_SEND_ERROR -> 9
            UPGRADING -> 10
            UPGRADE_STOP -> 11
            UPGRADE_SUCCESS -> 12
        }
}