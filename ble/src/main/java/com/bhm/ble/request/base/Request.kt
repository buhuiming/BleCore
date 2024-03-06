/*
 * Copyright (c) 2022-2032 buhuiming
 * 不能修改和删除上面的版权声明
 * 此代码属于buhuiming编写，在未经允许的情况下不得传播复制
 */
package com.bhm.ble.request.base

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import com.bhm.ble.BleManager
import com.bhm.ble.control.BleTask
import com.bhm.ble.data.Constants.DEFAULT_OPERATE_INTERVAL
import com.bhm.ble.data.Constants.DEFAULT_OPERATE_MILLIS_TIMEOUT
import com.bhm.ble.device.BleConnectedDevice
import com.bhm.ble.device.BleConnectedDeviceManager
import com.bhm.ble.device.BleDevice
import java.util.UUID


/**
 * 所有Request的基类
 * @author Buhuiming
 * @date 2023年05月26日 13时59分
 */
internal abstract class Request {

    /**
     * 获取BleManager
     */
    fun getBleManager() = BleManager.get()

    /**
     * 获取BleOptions
     */
    fun getBleOptions() = getBleManager().getOptions()

    /**
     * 获取操作时间
     */
    fun getOperateTime(): Long {
        var operateTime = getBleOptions()?.operateMillisTimeOut ?: DEFAULT_OPERATE_MILLIS_TIMEOUT
        if (operateTime <= 0) {
            operateTime = DEFAULT_OPERATE_MILLIS_TIMEOUT
        }
        return operateTime
    }

    /**
     * 获取操作间隔
     */
    fun getOperateInterval(): Long {
        var operateInterval = getBleOptions()?.operateInterval ?: DEFAULT_OPERATE_INTERVAL
        if (operateInterval <= 0) {
            operateInterval = DEFAULT_OPERATE_INTERVAL
        }
        return operateInterval
    }

    /**
     * 生成一个任务
     */
    fun getTask(
        taskId: String,
        block: suspend BleTask.() -> Unit,
        interrupt: (task: BleTask, throwable: Throwable?) -> Unit,
        callback: (task: BleTask, throwable: Throwable?) -> Unit
    ): BleTask {
        return getTask(
            taskId,
            getOperateTime(),
            block,
            interrupt,
            callback
        )
    }

    /**
     * 生成一个任务
     */
    fun getTask(
        taskId: String,
        durationTimeMillis: Long,
        block: suspend BleTask.() -> Unit,
        interrupt: (task: BleTask, throwable: Throwable?) -> Unit,
        callback: (task: BleTask, throwable: Throwable?) -> Unit
    ): BleTask {
        return BleTask(
            taskId = taskId,
            durationTimeMillis = durationTimeMillis,
            operateInterval = getOperateInterval(),
            callInMainThread = false,
            autoDoNextTask = true,
            block = block,
            interrupt = interrupt,
            callback = callback
        )
    }

    /**
     * 获取连接设备
     */
    fun getBleConnectedDevice(bleDevice: BleDevice): BleConnectedDevice? {
        return BleConnectedDeviceManager.get().getBleConnectedDevice(bleDevice)
    }

    /**
     * 获取BluetoothGatt
     */
    fun getBluetoothGatt(bleDevice: BleDevice): BluetoothGatt? {
        return getBleConnectedDevice(bleDevice)?.getBluetoothGatt()
    }

    /**
     * 获取Characteristic
     */
    fun getCharacteristic(bleDevice: BleDevice,
                          serviceUUID: String,
                          characteristicUUID: String
    ): BluetoothGattCharacteristic? {
        val gattService = getBluetoothGatt(bleDevice)?.getService(UUID.fromString(serviceUUID))
        return gattService?.getCharacteristic(UUID.fromString(characteristicUUID))
    }
}