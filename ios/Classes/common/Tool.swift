//
//  Tool.swift
//  flutter_ezw_amota
//
//  工具类，提供系统版本、MD5、URL编码解码、颜色转换、时间戳等常用静态方法
//  用于iOS端Flutter插件的通用功能支持
//  Created by Whiskee on 2025/4/18.
//

import UIKit
import CommonCrypto

class Tool {
    /// 获取系统版本号
    static func machineSystemVersion() -> Float {
        (UIDevice.current.systemVersion as NSString).floatValue
    }

    /// 字符串MD5
    static func md5(_ str: String) -> String {
        let data = Data(str.utf8)
        var digest = [UInt8](repeating: 0, count: Int(CC_MD5_DIGEST_LENGTH))
        data.withUnsafeBytes {
            _ = CC_MD5($0.baseAddress, CC_LONG(data.count), &digest)
        }
        return digest.map { String(format: "%02x", $0) }.joined()
    }

    /// URL编码
    static func encodeString(_ unencodedString: String) -> String {
        unencodedString.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? ""
    }

    /// URL解码
    static func decodeString(_ encodedString: String) -> String {
        encodedString.removingPercentEncoding ?? ""
    }

    /// 16进制转UIColor
    static func colorWithHexValue(_ rgbValue: Int) -> UIColor {
        UIColor(
            red: CGFloat((rgbValue & 0xFF0000) >> 16) / 255.0,
            green: CGFloat((rgbValue & 0x00FF00) >> 8) / 255.0,
            blue: CGFloat(rgbValue & 0x0000FF) / 255.0,
            alpha: 1.0
        )
    }

    /// 日期转时间戳字符串
    static func dateToTimeStamp(_ date: Date) -> String {
        String(Int(date.timeIntervalSince1970))
    }

    /// 时间戳字符串转日期
    static func timeStampToDate(_ timeStamp: String) -> Date? {
        guard let interval = TimeInterval(timeStamp) else { return nil }
        return Date(timeIntervalSince1970: interval)
    }
}
