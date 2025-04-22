//
//  ePauseReq+.swift
//  flutter_ezw_amota
//
//  Created by Whiskee on 2025/4/18.
//

import amOtaApi

extension eAmotaStatus {
    //  升级成功
    static let success: eAmotaStatus = eAmotaStatus(0)
    //  CRC校验失败
    static let crcError: eAmotaStatus = eAmotaStatus(1)
    //  无效的头信息
    static let invalidHeaderInfo: eAmotaStatus = eAmotaStatus(2)
    //  无效的包长
    static let invalidPkgLength: eAmotaStatus = eAmotaStatus(3)
    //  缓冲区空间不足
    static let insufficientBuffer: eAmotaStatus = eAmotaStatus(4)
    //  Flash异常
    static let insufficientFlash: eAmotaStatus = eAmotaStatus(5)
    //  未知错误
    static let unknownError: eAmotaStatus = eAmotaStatus(6)
    //  包长超出最大值
    static let max: eAmotaStatus = eAmotaStatus(7)
    //  文件读取失败
    static let fileReadError: Int = 8
    //  指令发送失败
    static let cmdSendError: Int = 9
    //  升级中
    static let upgrading: Int = 10
    //  升级停止
    static let upgradeStop: Int = 11
    //  初始化失败
    static let otaNotInitialized: Int = 12
}
