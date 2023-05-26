/*
 * Copyright (c) 2022-2032 buhuiming
 * 不能修改和删除上面的版权声明
 * 此代码属于buhuiming编写，在未经允许的情况下不得传播复制
 */
package com.bhm.ble.request

import com.bhm.ble.callback.BleConnectCallback
import com.bhm.ble.data.BleDevice
import com.bhm.ble.utils.BleLogger


/**
 * ble连接
 *
 * @author Buhuiming
 * @date 2023年05月24日 14时10分
 */
internal class BleConnectRequest {

    /**
     * 开始连接
     */
    fun connect(bleDevice: BleDevice, bleConnectCallback: BleConnectCallback) {
        BleLogger.e("开始连接：${bleDevice.deviceAddress}")
    }

    /**
     * 主动断开连接
     */
    fun disConnect(bleDevice: BleDevice) {
        BleLogger.e("断开连接：${bleDevice.deviceAddress}")
    }
}