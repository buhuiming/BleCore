/*
 * Copyright (c) 2022-2032 buhuiming
 * 不能修改和删除上面的版权声明
 * 此代码属于buhuiming编写，在未经允许的情况下不得传播复制
 */
package com.bhm.ble.data


/**
 * 常量
 *
 * @author Buhuiming
 * @date 2023年06月07日 13时52分
 */
object Constants {

    const val CONTAIN_SCAN_DEVICE_NAME = false

    const val AUTO_CONNECT = false

    const val ENABLE_LOG = true

    const val DEFAULT_SCAN_MILLIS_TIMEOUT: Long = 10000

    const val DEFAULT_SCAN_RETRY_COUNT: Int = 0

    const val DEFAULT_SCAN_RETRY_INTERVAL: Long = 1000

    const val DEFAULT_CONNECT_MILLIS_TIMEOUT: Long = 10000

    const val DEFAULT_CONNECT_RETRY_COUNT: Int = 0

    const val DEFAULT_CONNECT_RETRY_INTERVAL: Long = 1000

    const val DEFAULT_OPERATE_MILLIS_TIMEOUT: Long = 10000

    const val DEFAULT_OPERATE_INTERVAL: Long = 100

    const val DEFAULT_MAX_CONNECT_NUM: Int = 7

    const val DEFAULT_MTU: Int = 23

    const val DEFAULT_AUTO_SET_MTU = false

    val DEFAULT_TASK_QUEUE_TYPE = BleTaskQueueType.Default

    //系统提供接受通知自带的UUID
    const val UUID_CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR =
        "00002902-0000-1000-8000-00805f9b34fb"

    const val NOTIFY_TASK_ID = "1000"

    const val INDICATE_TASK_ID = "1001"

    const val SET_RSSI_TASK_ID = "1002"

    const val SET_MTU_TASK_ID = "1003"

    const val READ_TASK_ID = "1004"

    const val WRITE_TASK_ID = "1005"

    const val MARK = "#######----> "

    const val UN_COMPLETE = 0

    const val COMPLETED = 1

    const val CANCEL_UN_COMPLETE = 2

    const val CANCEL_WAIT_JOB_MESSAGE = "cancelWaitJobMessage"

    //Descriptor写数据失败
    const val EXCEPTION_CODE_DESCRIPTOR_FAIL = 100

    //设置通知失败，SetCharacteristicNotificationFail
    const val EXCEPTION_CODE_SET_CHARACTERISTIC_NOTIFICATION_FAIL = 101
}