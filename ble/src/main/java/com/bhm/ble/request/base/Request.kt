/*
 * Copyright (c) 2022-2032 buhuiming
 * 不能修改和删除上面的版权声明
 * 此代码属于buhuiming编写，在未经允许的情况下不得传播复制
 */
package com.bhm.ble.request.base

import com.bhm.ble.BleManager
import com.bhm.ble.control.BleTask
import com.bhm.ble.data.Constants.DEFAULT_OPERATE_INTERVAL
import com.bhm.ble.data.Constants.DEFAULT_OPERATE_MILLIS_TIMEOUT
import com.bhm.ble.device.BleConnectedDevice
import com.bhm.ble.device.BleConnectedDeviceManager
import com.bhm.ble.device.BleDevice


/**
 * 所有Request的基类
 * @author Buhuiming
 * @date 2023年05月26日 13时59分
 */
internal open class Request {

    fun getBleManager() = BleManager.get()

    fun getBleOptions() = getBleManager().getOptions()

    fun getOperateTime(): Long {
        var operateTime = getBleOptions()?.operateMillisTimeOut ?: DEFAULT_OPERATE_MILLIS_TIMEOUT
        if (operateTime <= 0) {
            operateTime = DEFAULT_OPERATE_MILLIS_TIMEOUT
        }
        return operateTime
    }

    fun getOperateInterval(): Long {
        var operateInterval = getBleOptions()?.operateInterval ?: DEFAULT_OPERATE_INTERVAL
        if (operateInterval <= 0) {
            operateInterval = DEFAULT_OPERATE_INTERVAL
        }
        return operateInterval
    }

    fun getBleConnectedDevice(bleDevice: BleDevice): BleConnectedDevice? {
        return BleConnectedDeviceManager.get().getBleConnectedDevice(bleDevice)
    }

    fun getTask(
        taskId: Int,
        block: suspend BleTask.() -> Unit,
        interrupt: (task: BleTask, throwable: Throwable?) -> Unit,
        callback: (task: BleTask, throwable: Throwable?) -> Unit
    ): BleTask {
        return BleTask(
            taskId = taskId,
            durationTimeMillis = getOperateTime(),
            operateInterval = getOperateInterval(),
            callInMainThread = false,
            autoDoNextTask = true,
            block = block,
            interrupt = interrupt,
            callback = callback
        )
    }
}