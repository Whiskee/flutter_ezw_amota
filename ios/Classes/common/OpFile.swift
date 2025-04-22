//
//  opFile.swift
//  flutter_ezw_amota
//
//  文件操作工具类，提供沙盒目录获取、文件/目录创建、删除、读写等常用方法
//  用于iOS端Flutter插件的数据持久化与文件管理
//
//  Created by Whiskee on 2025/4/18.

import Foundation

class OpFile {

    static let shared = OpFile()
    
    private init() {}

    /// 获取App沙盒Documents目录
    func getAppDocumentsDir() -> String {
        NSSearchPathForDirectoriesInDomains(.documentDirectory, .userDomainMask, true).first ?? ""
    }

    /// 创建目录
    func createDirectory(_ directoryName: String) -> Bool {
        let path = (getAppDocumentsDir() as NSString).appendingPathComponent(directoryName)
        do {
            try FileManager.default.createDirectory(atPath: path, withIntermediateDirectories: true, attributes: nil)
            return true
        } catch {
            return false
        }
    }

    /// 创建指定类型的文件
    func createFile(fileName: String, fileType: String) -> Bool {
        let path = (getAppDocumentsDir() as NSString).appendingPathComponent("\(fileName).\(fileType)")
        return FileManager.default.createFile(atPath: path, contents: nil, attributes: nil)
    }

    /// 创建文件并写入默认内容
    func createFile(fileName: String, defaultContents: String) -> String? {
        let path = (getAppDocumentsDir() as NSString).appendingPathComponent(fileName)
        do {
            try defaultContents.write(toFile: path, atomically: true, encoding: .utf8)
            return path
        } catch {
            return nil
        }
    }

    /// 获取文件完整路径
    func getFileAtDocPath(_ fileName: String) -> String {
        (getAppDocumentsDir() as NSString).appendingPathComponent(fileName)
    }

    /// 删除指定类型的文件
    func deleteFile(fileName: String, fileType: String) -> Bool {
        let path = (getAppDocumentsDir() as NSString).appendingPathComponent("\(fileName).\(fileType)")
        do {
            try FileManager.default.removeItem(atPath: path)
            return true
        } catch {
            return false
        }
    }

    /// 写入NSData到文件
    func writeFile(fileName: String, data: Data) {
        let path = (getAppDocumentsDir() as NSString).appendingPathComponent(fileName)
        FileManager.default.createFile(atPath: path, contents: data, attributes: nil)
    }

    /// 写入字符串到文件
    func writeFile(fileName: String, contents: String) -> Bool {
        let path = (getAppDocumentsDir() as NSString).appendingPathComponent(fileName)
        do {
            try contents.write(toFile: path, atomically: true, encoding: .utf8)
            return true
        } catch {
            return false
        }
    }

    /// 指定路径写入字符串
    func writeFile(filePath: String, contents: String) {
        try? contents.write(toFile: filePath, atomically: true, encoding: .utf8)
    }

    /// 读取文件为字典
    func readFile(_ fileName: String) -> [String: Any]? {
        let path = (getAppDocumentsDir() as NSString).appendingPathComponent(fileName)
        return NSDictionary(contentsOfFile: path) as? [String: Any]
    }

    /// 读取txt文件内容
    func readTxtFile(_ fileName: String) -> String? {
        let path = (getAppDocumentsDir() as NSString).appendingPathComponent(fileName)
        return try? String(contentsOfFile: path, encoding: .utf8)
    }
}
