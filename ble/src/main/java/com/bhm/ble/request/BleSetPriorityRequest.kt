/*
 * Copyright (c) 2022-2032 buhuiming
 * 不能修改和删除上面的版权声明
 * 此代码属于buhuiming编写，在未经允许的情况下不得传播复制
 */
package com.bhm.ble.request

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGatt
import com.bhm.ble.device.BleDevice
import com.bhm.ble.request.base.Request
import com.bhm.ble.log.BleLogger
import com.bhm.ble.utils.BleUtil


/**
 * 设置设备的传输优先级请求
 *
 * @author Buhuiming
 * @date 2023年06月07日 15时45分
 */
internal class BleSetPriorityRequest(private val bleDevice: BleDevice) : Request() {

    /**
     * 设置设备的传输优先级
     * connectionPriority 必须是 [BluetoothGatt.CONNECTION_PRIORITY_BALANCED]、
     * [BluetoothGatt.CONNECTION_PRIORITY_HIGH]、
     * [BluetoothGatt.CONNECTION_PRIORITY_LOW_POWER]的其中一个
     *
     */
    @SuppressLint("MissingPermission")
    fun setConnectionPriority(connectionPriority: Int): Boolean {
        if (!BleUtil.isPermission(getBleManager().getContext())) {
            BleLogger.e("${bleDevice.deviceAddress} -> 设置设备的传输优先级失败，没有权限")
            return false
        }
        val result = getBluetoothGatt(bleDevice)?.requestConnectionPriority(connectionPriority)?: false
        if (result) {
            BleLogger.i("${bleDevice.deviceAddress} -> 设置设备的传输优先级成功")
        } else {
            BleLogger.e("${bleDevice.deviceAddress} -> 设置设备的传输优先级失败")
        }
        return result
    }
}