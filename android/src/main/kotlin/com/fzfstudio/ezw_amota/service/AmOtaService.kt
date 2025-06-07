package com.fzfstudio.ezw_amota.service

import android.util.Log
import com.fzfstudio.ezw_amota.models.AmotaCmd
import com.fzfstudio.ezw_amota.models.AmotaConstants.AMOTA_CRC_SIZE_IN_PKT
import com.fzfstudio.ezw_amota.models.AmotaConstants.AMOTA_FW_PACKET_SIZE
import com.fzfstudio.ezw_amota.models.AmotaConstants.AMOTA_HEADER_SIZE_IN_PKT
import com.fzfstudio.ezw_amota.models.AmotaConstants.MAXIMUM_APP_PAYLOAD
import com.fzfstudio.ezw_amota.models.AmotaEvent
import com.fzfstudio.ezw_amota.models.AmotaStatus
import com.fzfstudio.ezw_amota.utils.CrcCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.io.IOException
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit


/**
 * OTA 升级服务核心实现
 * 
 * OTA 升级流程：
 * 1. [amOtaStart] - 初始化蓝牙连接和文件输入流
 * 2. [sendFwHeader] - 发送固件头信息(AMOTA_CMD_FW_HEADER)
 * 3. [sendFwData] - 分块传输固件数据(AMOTA_CMD_FW_DATA)
 * 4. [sendVerifyCmd] - 发送验证命令(AMOTA_CMD_FW_VERIFY)
 *
 * @author Whiskee
 * @since 2023/06/01
 */
class AmOtaService {

    //  协程作用域（使用IO调度器处理阻塞操作，配合监管作业实现错误隔离）
    private val coroutineScope = CoroutineScope(IO + SupervisorJob())
    //  当前OTA任务作业对象（用于控制协程生命周期）
    private var otaJob: Job? = null
    //  协程作用域：主线程回调
    private val mainScope: CoroutineScope = MainScope()
    //  待升级固件文件的绝对路径
    private var filePath: String? = null
    //  OTA升级状态标志（true表示升级正在进行中）
    private var isOtaUpgrading = false
    //  固件文件输入流（用于读取二进制固件数据）
    private var fileInputStream: FileInputStream? = null
    //  当前文件读取偏移量（单位：字节，用于断点续传）
    private var fileOffset = 0
    //  固件文件总大小（单位：字节，从文件头解析得到）
    private var fileSize = 0
    //  命令响应同步信号量（用于等待设备响应，初始许可数0）
    private var cmdResponseSemaphore: Semaphore? = null

    /**
     * 命令响应到达处理
     */
    companion object {
        /// 日志标签
        private val TAG = AmOtaService::class.simpleName
        /// 单例实例
        val instance: AmOtaService by lazy { AmOtaService() }
    }

    private constructor()

    //* ================== Public Method: OTA ================== *//

    /**
     * 启动OTA升级流程
     * @param filePath 固件文件路径
     *
     * 执行流程：
     * 1. 校验当前升级状态
     * 2. 初始化信号量和文件流
     * 3. 启动协程执行升级任务
     */
    fun amOtaStart(filePath: String?) {
        if (isOtaUpgrading) {
            Log.i(TAG, "Start ota: Is already updating")
            return
        }
        this.filePath = filePath
        fileOffset = 0
        cmdResponseSemaphore = Semaphore(0)
        //  执行 OTA 升级任务
        otaJob = coroutineScope.launch {
            startOtaUpdate()
        }
        isOtaUpgrading = true
        mainEventSend(AmotaEvent.UPGRADE_STATUS, AmotaStatus.UPGRADING.code)
    }

    /**
     * 停止OTA升级
     * 
     * 执行流程：
     * 1. 重置文件路径和偏移量
     * 2. 释放信号量资源
     * 3. 取消协程任务
     * 4. 重置升级状态
     */
    fun amOtaStop() {
        filePath = ""
        fileOffset = 0
        cmdResponseSemaphore?.release()
        cmdResponseSemaphore = null
        isOtaUpgrading = false
        coroutineScope.cancel()
        //  停止OTA升级任务
        otaJob?.cancel()
        otaJob = null
        mainEventSend(AmotaEvent.UPGRADE_STATUS, AmotaStatus.UPGRADE_STOP.code)
    }

    /**
     * 处理设备响应数据
     * @param response 设备返回的原始字节数组
     * 
     * 执行流程：
     * 1. 解析命令类型（第3字节）
     * 2. 检查错误码（第4字节）
     * 3. 处理头信息响应时更新文件偏移量（字节4-7）
     * 4. 其他命令类型仅释放信号量
     * 5. 记录未知命令类型日志
     */
    fun otaCmdResponse(response: ByteArray) {
        if (response.isEmpty()) {
            Log.e(TAG, "OTA response: null")
            return
        }
        val cmd = AmotaCmd.byte2OtaCmd(response[2].toInt() and 0xff)
        val responseData = formatHex2String(response)
        if (cmd == AmotaCmd.AMOTA_CMD_UNKNOWN) {
            Log.e(TAG, "OTA response: Cmd = $cmd, got unknown response = $responseData")
            amOtaStop()
            return
        }
        // TODO: handle CRC error and some more here
        if ((response[3].toInt() and 0xff) != 0) {
            Log.e(TAG, "OTA response: Cmd = $cmd, error occurred, response = $responseData")
            mainEventSend(AmotaEvent.UPGRADE_STATUS, AmotaStatus.CRC_ERROR.code)
            amOtaStop()
            return
        }
        when (cmd) {
            AmotaCmd.AMOTA_CMD_FW_HEADER -> {
                fileOffset =
                    ((response[4].toInt() and 0xFF) + ((response[5].toInt() and 0xFF) shl 8) +
                            ((response[6].toInt() and 0xFF) shl 16) + ((response[7].toInt() and 0xFF) shl 24))
                Log.i(TAG, "OTA response:  Cmd = $cmd, mFileOffset = $fileOffset")
                cmdResponseArrived()
            }
            AmotaCmd.AMOTA_CMD_FW_DATA -> {
                Log.i(TAG, "OTA response: Cmd = $cmd response")
                cmdResponseArrived()
            }
            AmotaCmd.AMOTA_CMD_FW_VERIFY -> {
                Log.i(TAG, "OTA response: Cmd = $cmd response")
                cmdResponseArrived()
            }
            AmotaCmd.AMOTA_CMD_FW_RESET -> {
                Log.i(TAG, "OTA response: Cmd = $cmd response")
                cmdResponseArrived()
            }
            else -> Log.i(TAG, "OTA response: Cmd = $cmd, get response from unknown command")
        }
    }

    //* ================== Private Method: OTA ================== *//

    /**
     * 核心升级流程控制
     * 
     * 执行流程：
     * 1. 打开固件文件
     * 2. 验证文件有效性
     * 3. 分阶段执行升级步骤
     * 4. 异常处理和资源清理
     */
    private suspend fun startOtaUpdate() {
        try {
            //  使用use可以自动释放资源
            FileInputStream(filePath).use { inputStream ->
                fileInputStream = inputStream
                fileSize = inputStream.available()
                if (fileSize == 0) {
                    Log.w(TAG, "OTA upgrading: Open file error, path: $filePath")
                    mainEventSend(AmotaEvent.UPGRADE_STATUS, AmotaStatus.FILE_READ_ERROR.code)
                    return
                }
                if (!sendFwHeader()) {
                    if (!isOtaUpgrading) {
                        return
                    }
                    isOtaUpgrading = false
                    Log.e(TAG, "OTA upgrading: Send firmware header failed")
                    return
                }
                setFileOffset()
                if (!sendFwData()) {
                    if (!isOtaUpgrading) {
                        return
                    }
                    isOtaUpgrading = false
                    Log.e(TAG, "OTA upgrading: Send firmware data failed")
                    return
                }
                if (!sendVerifyCmd()) {
                    if (!isOtaUpgrading) {
                        return
                    }
                    isOtaUpgrading = false
                    Log.e(TAG, "OTA upgrading: Firmware verification failed")
                    return
                }
                mainEventSend(AmotaEvent.UPGRADE_STATUS, AmotaStatus.UPGRADE_SUCCESS.code)
            }
        } catch (e: IOException) {
            isOtaUpgrading = false
            mainEventSend(AmotaEvent.UPGRADE_STATUS, AmotaStatus.FILE_READ_ERROR.code)
            Log.e(TAG, "OTA upgrading: File operation error: ${e.message}")
        } catch (e: Exception) {
            isOtaUpgrading = false
            mainEventSend(AmotaEvent.UPGRADE_STATUS, AmotaStatus.UNKNOWN_ERROR.code)
            Log.e(TAG, "OTA upgrading: Failed: ${e.message}")
        } finally {
            fileInputStream = null
            Log.i(TAG, "OTA upgrading: Exit OTA update process")
        }
    }

    /**
     * 发送固件头信息
     * 执行流程：
     * 1. 读取48字节头数据
     * 2. 校验数据完整性（长度≥48字节）
     * 3. 解析字节8-11获取固件总大小
     * 4. 构造头信息命令包并发送
     * 5. 等待设备响应（超时3秒）
     */
    @Throws(IOException::class)
    private suspend fun sendFwHeader(): Boolean {
        // 步骤1：创建48字节缓冲区读取文件头
        val fwHeaderRead = ByteArray(48)
        // 步骤2：从文件流读取头信息（要求至少读取48字节）
        val ret: Int = fileInputStream!!.read(fwHeaderRead)
        // 步骤3：验证读取完整性（协议要求头长度必须≥48字节）
        if (ret < 48) {
            Log.w(TAG, "Send Header: Invalid packed firmware length")
            return false
        }
        // 步骤4：解析固件总大小（协议规定存储在字节8-11，小端格式）
        fileSize =
            ((fwHeaderRead[11].toInt() and 0xFF) shl 24) + // 字节11：最高位字节
            ((fwHeaderRead[10].toInt() and 0xFF) shl 16) + // 字节10
            ((fwHeaderRead[9].toInt() and 0xFF) shl 8) +   // 字节9
             (fwHeaderRead[8].toInt() and 0xFF)            // 字节8：最低位字节
        // 步骤5：记录头信息调试日志（含十六进制格式）
        Log.i(TAG, "Send Header: FileSize = $fileSize, rawHeader = ${fwHeaderRead.contentToString()}, headerHex = ${formatHex2String(fwHeaderRead)}")
        // 步骤6：发送AMOTA_CMD_FW_HEADER命令并等待3秒响应
        return sendOtaCmd(AmotaCmd.AMOTA_CMD_FW_HEADER, fwHeaderRead, fwHeaderRead.size)
    }

    /**
     * 构造并发送OTA命令包
     * @param cmd 命令类型枚举（AMOTA_CMD_FW_HEADER/AMOTA_CMD_FW_DATA等）
     * @param data 要发送的原始数据字节数组
     * @param len 有效数据长度（单位：字节）
     * @return Boolean 命令包构造和发送是否成功
     *
     * 执行流程：
     * 1. 计算总包长（头+数据+CRC）
     * 2. 初始化字节数组缓冲区
     * 3. 填充包头字段（数据长度小端存储）
     * 4. 计算数据CRC32校验和（仅当有数据时）
     * 5. 组装完整数据包（头+数据+CRC）
     * 6. 调用分帧发送方法
     * 7. 根据发送结果更新状态事件
     */
    private suspend fun sendOtaCmd(cmd: AmotaCmd, data: ByteArray, len: Int): Boolean {
        // 步骤1：初始化CRC校验和为0
        var checksum = 0
        // 步骤2：计算完整包长度（协议头长度 + 数据长度 + CRC字段长度）
        val packetLength =  AMOTA_HEADER_SIZE_IN_PKT + len + AMOTA_CRC_SIZE_IN_PKT
        // 步骤3：创建对应长度的字节缓冲区
        val packet = ByteArray(packetLength)
        // 步骤4：填充协议头（小端格式）
        // 数据长度字段（包含CRC长度）：低字节在前
        packet[0] = (len + AMOTA_CRC_SIZE_IN_PKT).toByte()       // 低字节
        packet[1] = ((len + AMOTA_CRC_SIZE_IN_PKT) shr 8).toByte() // 高字节
        packet[2] = cmd.amOtaCmd2Byte  // 命令码
        if (len != 0) {
            // 步骤5：计算数据部分CRC32校验和
            checksum = CrcCalculator.calcCrc32(len, data)
            // 步骤6：将原始数据拷贝到协议数据区
            System.arraycopy(data, 0, packet, AMOTA_HEADER_SIZE_IN_PKT, len)
        }
        // 步骤7：填充CRC字段（小端格式）
        packet[AMOTA_HEADER_SIZE_IN_PKT + len] = ((checksum).toByte())          // 字节0
        packet[AMOTA_HEADER_SIZE_IN_PKT + len + 1] = ((checksum shr 8).toByte())  // 字节1
        packet[AMOTA_HEADER_SIZE_IN_PKT + len + 2] = ((checksum shr 16).toByte()) // 字节2
        packet[AMOTA_HEADER_SIZE_IN_PKT + len + 3] = ((checksum shr 24).toByte()) // 字节3
        // 步骤8：发送完整数据包
        return if (sendPacket(packet, packetLength)) {
            true
        } else {
            if (!isOtaUpgrading) {
                false
            }
            // 错误处理：记录日志并发送状态更新
            Log.e(TAG, "Send Cmd: Failed, Cmd = ${cmd.name}")
            when (cmd) {
                // 根据命令类型发送对应的错误状态
                AmotaCmd.AMOTA_CMD_FW_HEADER -> mainEventSend(AmotaEvent.UPGRADE_STATUS,AmotaStatus.INVALID_HEADER_INFO.code)
                AmotaCmd.AMOTA_CMD_FW_DATA -> mainEventSend( AmotaEvent.UPGRADE_STATUS, AmotaStatus.INVALID_PACKAGE_LENGTH.code)
                AmotaCmd.AMOTA_CMD_FW_VERIFY -> mainEventSend(AmotaEvent.UPGRADE_STATUS, AmotaStatus.CMD_SEND_ERROR.code)
                else -> mainEventSend(AmotaEvent.UPGRADE_STATUS, AmotaStatus.UNKNOWN_ERROR.code)
            }
            false
        }
    }

    /**
     * 分帧发送数据包
     * 执行流程：
     * 1. 根据最大载荷分割数据包
     * 2. 循环发送每个子帧
     * 3. 任一子帧发送失败立即中止
     * 4. 全部发送成功返回true
     */
    private suspend fun sendPacket(data: ByteArray, len: Int): Boolean {
        var idx = 0
        while (idx < len) {
            val frameLen: Int = if ((len - idx) > MAXIMUM_APP_PAYLOAD) {
                MAXIMUM_APP_PAYLOAD
            } else {
                len - idx
            }
            val frame = ByteArray(frameLen)
            System.arraycopy(data, idx, frame, 0, frameLen)
            try {
                Log.e(TAG, "Send Packet: mini packet index = $idx")
                if (!sendOneFrame(frame, idx == AMOTA_FW_PACKET_SIZE)) {
                    return false
                }
            } catch (e: InterruptedException) {
                Log.e(TAG, "Send Packet: Failed, error = ${e.message}")
            }
            idx += frameLen
        }
        return true
    }

    /**
     * 发送单个数据帧
     *
     * @param data 待发送的数据字节数组
     * @param isNeedResponse 是否需要等待响应
     *
     * 执行流程：
     * 1. 检查OTA是否被中止
     * 2. 通过蓝牙服务写入特征值
     * 3. 等待GATT写入完成信号
     * 4. 返回写入结果状态
     */
    @OptIn(DelicateCoroutinesApi::class)
    @Throws(InterruptedException::class)
    private suspend fun sendOneFrame(data: ByteArray?, isNeedResponse: Boolean = false): Boolean {
        if (!isOtaUpgrading) {
            Log.e(TAG, "Send Cmd: OTA stopped due to application control")
            return false
        }
        //  发送指令
        mainEventSend(AmotaEvent.OTA_CMD_HANDLE, data)
        return if (isNeedResponse) {
            waitCmdResponse()
        } else {
            delay(35)
            true
        }
    }

    /**
     * 记录文件偏移量，用于断点续传
     * 执行流程：
     * 1. 检查文件偏移量是否大于0
     * 2. 跳过文件输入流到指定偏移量
     */
    @Throws(IOException::class)
    private fun setFileOffset() {
        if (fileOffset > 0) {
            fileInputStream!!.skip(fileOffset.toLong())
            Log.i(TAG, "Send Cmd: Set file offset = $fileOffset")
        }
    }

    /**
     * 发送单个数据包
     * 
     * 执行流程：
     * 1. 读取固定长度数据包（默认4096字节）
     * 2. 处理文件末尾不足包长的情况
     * 3. 构造数据命令包并发送
     * 4. 返回实际发送字节数
     * 
     * @return Int 成功发送的字节数，-1表示失败
     */
    private suspend fun sendFwData(): Boolean {
        val fwDataSize = fileSize
        var ret = -1
        var offset = fileOffset
        val packCount = fileSize / AMOTA_FW_PACKET_SIZE
        Log.i(TAG, "OTA upgrading - Send fw Data: file size = $fileSize，pack count = $packCount")
        while (offset < fwDataSize) {
            try {
                ret = sentFwDataPacket()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            if (ret < 0) {
                if (!isOtaUpgrading) {
                    return false
                }
                Log.e(TAG, "OTA upgrading - Send fw data: Sent packet failed")
                mainEventSend(AmotaEvent.UPGRADE_STATUS, AmotaStatus.INVALID_PACKAGE_LENGTH.code)
                return false
            }
            offset += ret
            //  每1k等待眼镜BUFF处理完
            if (offset % 1024 == 0) {
                delay(50)
            }
            mainEventSend(AmotaEvent.UPGRADE_PROGRESS, (offset * 100) / fwDataSize)
            Log.i(TAG, "OTA upgrading - Send fw Data: Total pack count = ${packCount}, had send pack count = ${offset / AMOTA_FW_PACKET_SIZE}")
        }
        Log.i(TAG, "OTA upgrading - Send Fw data: Send firmware data complete")
        return true
    }

    /**
     * 设置文件读取偏移量
     * @throws IOException 文件操作异常
     * 
     * 执行流程：
     * 1. 检查偏移量是否大于0
     * 2. 跳过指定字节数
     * 3. 记录当前偏移量日志
     */
    @Throws(IOException::class)
    private suspend fun sentFwDataPacket(): Int {
        val ret: Int
        var len = AMOTA_FW_PACKET_SIZE
        val fwData = ByteArray(len)
        ret = fileInputStream!!.read(fwData)
        if (ret <= 0) {
            Log.e(TAG, "OTA upgrading - Sent data packet: No data read from mFsInput")
            return -1
        }
        if (ret < AMOTA_FW_PACKET_SIZE) {
            len = ret
        }
        Log.i(TAG, "OTA upgrading - Sent data packet: fw data len = $len")
        return if (sendOtaCmd(AmotaCmd.AMOTA_CMD_FW_DATA, fwData, len)) {
            ret
        } else {
            -1
        }
    }

    /**
     * 发送验证命令
     * @return Boolean 是否收到有效响应
     * 
     * 执行流程：
     * 1. 构造空数据验证命令包
     * 2. 发送命令并等待响应（超时5秒）
     */
    private suspend fun sendVerifyCmd(): Boolean {
        Log.i(TAG, "OTA upgrading - Send cmd: Send fw verify cmd")
        return sendOtaCmd(AmotaCmd.AMOTA_CMD_FW_VERIFY, byteArrayOf(), 0)
    }

    //* ================== Private Method: Tools ================== *//

    /**
     * 字节数组转十六进制字符串
     * @param data 待转换的字节数组
     * @return String 十六进制字符串（空格分隔）
     * 
     * 执行流程
     * 1. 遍历字节数组
     * 2. 每个字节转为两位十六进制表示
     * 3. 用空格分隔每个字节
     */
    private fun formatHex2String(data: ByteArray): String {
        val stringBuilder = StringBuilder(data.size)
        for (byteChar in data) {
            stringBuilder.append(String.format("%02X ", byteChar))
        }
        return stringBuilder.toString()
    }

    /**
     * 等待命令响应
     * @param timeoutMs 最大等待时间（单位：毫秒，默认3000ms）
     * @return Boolean 是否在超时前收到响应
     * 
     * 执行流程：
     * 1. 尝试获取响应信号量
     * 2. 处理线程中断异常
     * 3. 返回信号量获取结果
     */
    private fun waitCmdResponse(timeoutMs: Long = 3000): Boolean = try {
        cmdResponseSemaphore!!.tryAcquire(timeoutMs, TimeUnit.MILLISECONDS)
    } catch (e: InterruptedException) {
        Log.e(TAG, "Send Cmd: Wait Cmd response error = ${e.message}")
        false
    }

    /**
     * 通知命令响应到达
     * 执行流程：
     * 释放响应信号量（许可数+1）
     */
    private fun cmdResponseArrived() = cmdResponseSemaphore?.release()

    /**
     *  主线程事件发送
     */
    private fun mainEventSend(event: AmotaEvent, argument: Any?) {
        mainScope.launch {
            event.sink?.success(argument)
        }
    }
}
