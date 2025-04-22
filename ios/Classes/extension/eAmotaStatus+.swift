//
//  ePauseReq+.swift
//  flutter_ezw_amota
//
//  Created by Whiskee on 2025/4/18.
//

import amOtaApi

extension eAmotaStatus {
    static let success: eAmotaStatus = eAmotaStatus(0)
    static let crcError: eAmotaStatus = eAmotaStatus(1)
    static let invalidHeaderInfo: eAmotaStatus = eAmotaStatus(2)
    static let invalidPkgLength: eAmotaStatus = eAmotaStatus(3)
    static let insufficientBuffer: eAmotaStatus = eAmotaStatus(4)
    static let insufficientFlash: eAmotaStatus = eAmotaStatus(5)
    static let unknownError: eAmotaStatus = eAmotaStatus(6)
    static let max: eAmotaStatus = eAmotaStatus(7)
}
