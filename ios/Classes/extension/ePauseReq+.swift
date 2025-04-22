//
//  ePauseReq+.swift
//  flutter_ezw_amota
//
//  Created by Whiskee on 2025/4/18.
//

import amOtaApi

extension ePauseReq {
    static let none: ePauseReq = ePauseReq(0)
    static let pause: ePauseReq = ePauseReq(1)
    static let resume: ePauseReq = ePauseReq(2)
    static let fileReload: ePauseReq = ePauseReq(3)
}
