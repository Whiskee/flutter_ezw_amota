//
//  Untitled.swift
//  flutter_ezw_amota
//
//  Created by Whiskee on 2025/4/18.
//
//  AmOta服务类 - 负责处理设备OTA升级相关功能
//  主要功能:
//  1. 初始化OTA服务
//  2. 执行OTA升级
//  3. 处理升级过程中的各种回调
//  4. 管理升级状态和进度

import OSLog
import amOtaApi

public class AmOtaService: NSObject {
    
    public static let shared = AmOtaService()
    
    //  =========== Private Constants
    private let log: Logger = Logger()
    //  设置默认缓冲区域
    private let defaultBrick: UInt16 = 128
    
    //  =========== Private Variables
    //  是否已经初始化了
    private var isInitialized = false
    //  - 蓝牙管理工具
    private var centralManager: CBCentralManager?
    //  - 已连接设备
    private var peripheral: CBPeripheral?
    //  OTA文件 - 数据
    fileprivate var fileBytes: UnsafePointer<UInt8>?
    //  OTA文件 - 大小
    fileprivate var fileSize: UInt32 = 0
    //  是否升级中
    private var isOtaUpgrading: Bool = false
    
    /// Get:
    //  - 获取AmOta接口
    private var mAmOtaApi: amOtaApi {
        get {
            return amOtaApi.sharedInstance()
        }
    }
       
    private override init() {}
    
    /**
     * 初始化OTA服务，必须实现
     * 步骤:
     * 1. 设置初始化标志
     * 2. 设置AmOtaAPI升级状态的代理
     * 3. 添加Amota配置确认的通知监听
     */
    func initialize() {
        isInitialized = true
        centralManager = CBCentralManager(delegate: self, queue: nil)
        mAmOtaApi.amotaApiUpdateAppDataDelegate = self
        //  Amota监听 - 配置是否成功
        NotificationCenter.default.addObserver(self,
                                               selector: #selector(amotaEnableConfirmRsp),
                                               name: NSNotification.Name(aMotaEnableConfirmNoti),
                                               object: nil)
    }
    
    /**
     * 开始OTA升级流程
     * 步骤:
     * 1. 检查初始化状态
     * 2. 检查是否正在升级
     * 3. 通过蓝牙设置中已连接获取
     * 4. 加载OTA文件
     * 5. 启用OTA升级接口
     */
    func startOtaUpgrade(uuid: String, filePath: String) {
        guard isInitialized else {
            log.error("AmOtaService - Start OTA: You must initOta first")
            AmotaEC.upgradeStatus.event?(eAmotaStatus.otaNotInitialized)
            return
        }
        guard !isOtaUpgrading else {
            log.warning("AmOtaService - Start OTA: Is upgrading")
            return
        }
        guard let peripheral = centralManager?.retrieveConnectedPeripherals(withServices: [CBUUID(string: UUID_AMOTA_SERVICE)]).first(where: { peripheral in
            peripheral.identifier.uuidString == uuid
        }) else {
            log.error("AmOtaService - Start OTA: You must provide peripheral")
            return
        }
        guard loadOtaFile(filePath: filePath) else {
            return
        }
        self.peripheral = peripheral
        self.peripheral?.delegate = self
        centralManager?.connect(self.peripheral!)
        log.info("AmOtaService - Start OTA: \(uuid) start connecting")
    }
    
    /**
     * 停止OTA升级
     * 步骤:
     * 1. 检查初始化状态
     * 2. 检查设备连接状态
     * 3. 检查当前是否在升级
     * 4. 清理升级状态
     * 5. 暂停OTA操作
     */
    func stopOtaUpgrade() {
        guard isInitialized else {
            log.warning("AmOtaService - Stop OTA: You must initOta first")
            AmotaEC.upgradeStatus.event?(eAmotaStatus.otaNotInitialized)
            return
        }
        guard isOtaUpgrading else {
            log.warning("AmOtaService - Stop OTA: Is already stop")
            return
        }
        resetUpgradeProperties()
        mAmOtaApi.amOtaPause(peripheral, with: ePauseReq.fileReload)
        AmotaEC.upgradeStatus.event?(eAmotaStatus.upgradeStop)
        log.info("AmOtaService - Stop OTA")
    }
    
    /**
     * 释放OTA服务资源
     * 步骤:
     * 1. 停止当前升级
     * 2. 重置初始化状态
     * 3. 清空设备引用
     * 4. 移除代理
     * 5. 移除所有通知监听
     */
    func release() {
        stopOtaUpgrade()
        isInitialized = false
        centralManager = nil
        //  移除Amota升级监听
        mAmOtaApi.amotaApiUpdateAppDataDelegate = nil
        //  移除所有监听
        NotificationCenter.default.removeObserver(self)
        log.info("AmOtaService - Already release")
    }
   
}

/// MARK - Private Method
extension AmOtaService {

    /**
     * 加载并验证OTA文件
     * 步骤:
     * 1. 检查文件是否可访问
     * 2. 读取文件数据
     * 3. 获取文件字节指针
     * 4. 记录文件大小
     * 5. 验证数据有效性
     */
    private func loadOtaFile(filePath: String) -> Bool {
        let fileUrl = URL(fileURLWithPath: filePath)
        do {
            let isReachable = try fileUrl.checkResourceIsReachable()
            if !isReachable {
                AmotaEC.upgradeStatus.event?(eAmotaStatus.fileReadError)
                log.error("AmOtaService - Start OTA: File not reachable: \(filePath)")
                return false
            }
        } catch {
            AmotaEC.upgradeStatus.event?(eAmotaStatus.fileReadError)
            log.error("AmOtaService - Start OTA: File check failed: \(error.localizedDescription), path = \(filePath)")
            return false
        }
        guard let fileData = NSData(contentsOfFile: filePath) else {
            AmotaEC.upgradeStatus.event?(eAmotaStatus.fileReadError)
            log.error("AmOtaService - Start OTA: File data is null, path = \(filePath)")
            return false
        }
        fileBytes = fileData.bytes.assumingMemoryBound(to: UInt8.self)
        fileSize = UInt32(fileData.length)
        let isFileEnable = fileBytes != nil && fileSize > 0
        AmotaEC.upgradeStatus.event?(eAmotaStatus.fileReadError)
        log.info("AmOtaService - Start OTA: File path = \(filePath), size = \(self.fileSize), is enable = \(isFileEnable)")
        return isFileEnable
    }
    
    /**
     * 处理Amota配置确认通知
     * 步骤:
     * 1. 解析通知数据
     * 2. 检查使能状态
     * 3. 设置升级状态
     * 4. 延迟执行OTA升级
     * 5. 配置数据包大小并开始升级
     */
    @objc fileprivate func amotaEnableConfirmRsp(notify: Notification) {
        guard let dictStatus = notify.object as? [String: Any] else {
            return
        }
        let otaEnableStatus = dictStatus[keyAmotaEnableConfirm]
        guard let status = otaEnableStatus as? NSNumber, status == 1 else {
            AmotaEC.upgradeStatus.event?(eAmotaStatus.unknownError.rawValue)
            log.error("AmOtaService - Start OTA: Config failed!!!")
            return
        }
        //  设置状态为升级状态
        isOtaUpgrading = true
        //  延迟3， 等待属性全部读完后再执行OTA升级
        DispatchQueue.main.asyncAfter(deadline: .now() + 3.0) { [weak self] in
            guard let self = self else {
                return
            }
            self.mAmOtaApi.amOtaSetupBrickSize(self.defaultBrick * 4)
            self.mAmOtaApi.amOtaStart(self.peripheral, withDataByte: self.fileBytes, withLength: self.fileSize)
            self.log.info("AmOtaService - Start OTA: Ota is upgrading")
        }
        log.info("AmOtaService - Start OTA: Config is enable, start ota upgrade after 3s")
    }
    
    /**
     * 重置升级属性
     * 步骤:
     * 1. 清除文件字节指针
     * 2. 重置文件大小
     * 3. 清除设备引用
     * 4. 重置升级状态
     */
    private func resetUpgradeProperties() {
        fileBytes = nil
        fileSize = 0
        peripheral = nil
        isOtaUpgrading = false
    }
    
}

/// MARK - CBPeripheralDelegate
extension AmOtaService: CBCentralManagerDelegate {
    
    public func centralManagerDidUpdateState(_ central: CBCentralManager) {
        log.info("AmOtaService - Init OTA: \(central.state.rawValue) ")
    }
    
    public func centralManager(_ central: CBCentralManager, didConnect peripheral: CBPeripheral) {
        //  启用OTA升级接口：执行服务查询
        mAmOtaApi.amOtaEnable(peripheral, withServiceUUID: UUID_AMOTA_SERVICE)
        AmotaEC.upgradeStatus.event?(eAmotaStatus.upgrading)
        log.info("AmOtaService - Start OTA: \(peripheral.identifier.uuidString) had connected, start ota upgrade")
    }
    
}

/// MARK - CBPeripheralDelegate
extension AmOtaService: CBPeripheralDelegate {
    
    // 蓝牙服务发现回调
    public func peripheral(_ peripheral: CBPeripheral, didDiscoverServices error: Error?) {
        peripheral.services?.forEach { service in
            peripheral.discoverCharacteristics(nil, for: service)
        }
        log.info("AmOtaService - Start OTA: \(peripheral.services == nil ? "Not find service" : "Finding service") ")
    }

    // 蓝牙特征发现回调
    public func peripheral(_ peripheral: CBPeripheral, didDiscoverCharacteristicsFor service: CBService, error: Error?) {
        //  遍历所有特征并发现其描述符
        service.characteristics?.forEach { chars in
            peripheral.discoverDescriptors(for: chars)
            log.info("AmOtaService - Start OTA: Characteristic uuid = \(chars.uuid.uuidString), properties = \(chars.properties.rawValue)")
        }
        //  Amota: 接收服务
        let dictSvcInfo: [String: Any?] = [
            keyBleDiscvrdCharForPeriNoti: peripheral,
            keyBleDiscvrdCharForServiceNoti: service,
            keyBleDiscvrdCharForErrorNoti: error
        ]
        NotificationCenter.default.post(
            name: Notification.Name(bleDiscoveredCharToOtaApiNoti),
            object: dictSvcInfo,
            userInfo: nil
        )
    }
    
    // 蓝牙描述符发现回调
    public func peripheral(_ peripheral: CBPeripheral, didDiscoverDescriptorsFor characteristic: CBCharacteristic, error: (any Error)?) {
        characteristic.descriptors?.forEach { descriptor in
            peripheral.readValue(for: descriptor)
            log.info("AmOtaService - Start OTA: Descriptor value = \(String(describing: descriptor.value ?? "")), uuid = \(descriptor.uuid.uuidString)")
        }
    }
    
    // 特征值更新回调
    public func peripheral(_ peripheral: CBPeripheral, didUpdateValueFor characteristic: CBCharacteristic, error: (any Error)?) {
        //  Amota: 接收指令数据
        peripheral.services?.forEach { service in
            let dictSvcInfo: [String: Any?] = [
                keyBleUpdateValueCharToOtaForPeriNoti: peripheral,
                keyBleUpdateValueCharToOtaForServiceNoti: service,
                keyBleUpdateValueCharToOtaForCharNoti: characteristic,
                keyBleDiscvrdCharForErrorNoti: error
            ]
            NotificationCenter.default.post(
                name: Notification.Name(bleUpdateValueCharToOtaApiNoti),
                object: dictSvcInfo,
                userInfo: nil
            )
        }
        log.info("AmOtaService - Start OTA: Characteristic uuid = \(characteristic.uuid.uuidString)), error = \(error?.localizedDescription ?? "null")")
    }
    
    // 通知状态更新回调
    public func peripheral(_ peripheral: CBPeripheral, didUpdateNotificationStateFor characteristic: CBCharacteristic, error: (any Error)?) {
        if error == nil {
            peripheral.readValue(for: characteristic)
        }
        log.info("AmOtaService - Start OTA: Update notification state, error = \(error?.localizedDescription ?? "null")")
    }
    
    // 描述符值更新回调
    public func peripheral(_ peripheral: CBPeripheral, didUpdateValueFor descriptor: CBDescriptor, error: (any Error)?) {
        log.info("AmOtaService - Start OTA: Descriptor uuid = \(descriptor.uuid.uuidString), update value = \(String(describing: descriptor.value ?? ""))")
    }
}

/// MARK - AmotaApiUpdateAppDataDelegate
extension AmOtaService: AmotaApiUpdateAppDataDelegate {
    
    /**
     * 固件头信息响应处理
     * 步骤:
     * 1. 发送升级状态
     * 2. 检查响应状态
     */
    public func didAmOtaFwHeaderRsp(_ svrStatus: eAmotaStatus) {
        guard !sendOtaUpgradeStatus(status: svrStatus) else {
            return
        }
        log.error("AmOtaService - Upgrading: Fw Header rsp error = \(svrStatus.rawValue)")
    }
    
    /**
     * 固件数据包发送响应处理
     * 步骤:
     * 1. 发送升级状态
     * 2. 更新升级进度
     * 3. 检查响应状态
     */
    public func didAmOtaFwDataRsp(_ pkgSentStatus: eAmotaStatus, withDataLengthSent length: UInt32) {
        guard !sendOtaUpgradeStatus(status: pkgSentStatus, type: 1) else {
            AmotaEC.upgradeProgress.event?(Int(length / fileSize))
            return
        }
        log.error("AmOtaService - Upgrading: Fw data rsp error = Length = \(length), status = \(pkgSentStatus.rawValue)")
    }
    
     /**
     * 用户命令响应处理
     * 步骤:
     * 1. 发送升级状态
     * 2. 检查响应状态
     */
    public func didAmOtaUserCmdRsp(_ curCmd: eAmotaUserCmd, with status: eAmotaStatus) {
        let type = curCmd == .verify ? 2 : 3
        guard !sendOtaUpgradeStatus(status: status, type: type) else {
            return
        }
        log.error("AmOtaService - Upgrading: User cmd rsp error = Cmd = \(curCmd.rawValue), status = \(status.rawValue)")
    }

    /**
     * 发送OTA升级状态
     *
     * @param eAmotaStatus 状态
     * @param Int? 类型, 0 = 固件：头信息，1 = 固件：数据， 2 = 固件升级完成检验指令， 3 = 系统重置指令
     *
     * 步骤:
     * 1. 检查状态是否成功
     * 2. 暂停OTA操作
     * 3. 触发状态事件
     */
    private func sendOtaUpgradeStatus(status: eAmotaStatus, type: Int = 0) -> Bool {
        guard status != eAmotaStatus.success else {
            //  校验指令发送成功后，表示升级完成
            if type == 2 {
                DispatchQueue.main.asyncAfter(deadline:.now() + 1.5) { [weak self] in
                    //    重置升级属性
                    self?.resetUpgradeProperties()
                    //  发送完成
                    AmotaEC.upgradeStatus.event?(eAmotaStatus.success.rawValue)
                }
            }
            log.info("AmOtaService - Upgrading: Success, type = \(type == 0 ? "Fw Header" : type == 1 ? "Fw Data" : type == 2 ? "Fw Verify" : "OS Reset")")
            return true
        }
        mAmOtaApi.amOtaPause(peripheral, with: ePauseReq.fileReload)
        AmotaEC.upgradeStatus.event?(status.rawValue)
        return false
    }
}
