package com.fzfstudio.ezw_amota.models

/**
 * OTA 升级指令
 */
enum class AmotaCmd {
    //  发送固件头信息命令
    AMOTA_CMD_FW_HEADER,
    //  传输固件数据命令
    AMOTA_CMD_FW_DATA,
    //  固件验证命令
    AMOTA_CMD_FW_VERIFY,
    //  设备重置命令
    AMOTA_CMD_FW_RESET,
    //  枚举最大值（边界标记，非实际命令）
    AMOTA_CMD_MAX,
    //  未知命令类型
    AMOTA_CMD_UNKNOWN;

    /**
     * 将指令转换为字节
     */
    val amOtaCmd2Byte: Byte
        get() =  when (this) {
            AMOTA_CMD_FW_HEADER -> 1
            AMOTA_CMD_FW_DATA -> 2
            AMOTA_CMD_FW_VERIFY -> 3
            AMOTA_CMD_FW_RESET -> 4
            AMOTA_CMD_MAX -> 5
            else -> 0
        }

    companion object {
        /**
         * 将字节转指令
         */
        fun byte2OtaCmd(cmd: Int): AmotaCmd = when (cmd) {
            1 -> AMOTA_CMD_FW_HEADER
            2 -> AMOTA_CMD_FW_DATA
            3 -> AMOTA_CMD_FW_VERIFY
            4 -> AMOTA_CMD_FW_RESET
            5 -> AMOTA_CMD_MAX
            else ->  AMOTA_CMD_UNKNOWN
        }
    }
}