package com.bhm.ble.log

/**
 * @description 日志级别
 * @author Buhuiming
 * @date 2024/05/08/ 10:28
 */
sealed class BleLogLevel {
    object Debug : BleLogLevel()
    object Info : BleLogLevel()
    object Warn : BleLogLevel()
    object Error : BleLogLevel()
}
