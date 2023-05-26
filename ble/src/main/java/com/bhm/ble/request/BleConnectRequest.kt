/*
 * Copyright (c) 2022-2032 buhuiming
 * 不能修改和删除上面的版权声明
 * 此代码属于buhuiming编写，在未经允许的情况下不得传播复制
 */
package com.bhm.ble.request

import com.bhm.ble.callback.BleConnectCallback
import com.bhm.ble.control.BleDeviceControllerManager
import com.bhm.ble.data.BleConnectFailType
import com.bhm.ble.data.BleDevice
import com.bhm.ble.utils.BleLogger
import com.bhm.ble.utils.BleUtil


/**
 * ble连接、断开连接请求
 *
 * @author Buhuiming
 * @date 2023年05月24日 14时10分
 */
internal class BleConnectRequest : Request(){

    /**
     * 连接设备
     */
    @Synchronized
    fun connect(bleDevice: BleDevice, bleConnectCallback: BleConnectCallback) {
        if (bleDevice.deviceInfo == null) {
            BleLogger.e("连接失败：BluetoothDevice为空")
            bleConnectCallback.callConnectFail(bleDevice, BleConnectFailType.NullableBluetoothDevice)
            return
        }
        val bleManager = getBleManager()
        if (!BleUtil.isPermission(bleManager.getContext()?.applicationContext)) {
            BleLogger.e("权限不足，请检查")
            bleConnectCallback.callConnectFail(bleDevice, BleConnectFailType.NoBlePermissionType)
            return
        }
        if (!bleManager.isBleSupport()) {
            BleLogger.e("设备不支持蓝牙")
            bleConnectCallback.callConnectFail(bleDevice, BleConnectFailType.UnTypeSupportBle)
            return
        }
        if (!bleManager.isBleEnable()) {
            BleLogger.e("蓝牙未打开")
            bleConnectCallback.callConnectFail(bleDevice, BleConnectFailType.BleDisable)
            return
        }
        BleDeviceControllerManager.get()
            .buildBleDeviceController(bleDevice)
            .connect(bleConnectCallback)
    }

    /**
     * 主动断开连接
     */
    fun disConnect(bleDevice: BleDevice) {
        BleLogger.e("断开连接：${bleDevice.deviceAddress}")
    }
}