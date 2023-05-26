/*
 * Copyright (c) 2022-2032 buhuiming
 * 不能修改和删除上面的版权声明
 * 此代码属于buhuiming编写，在未经允许的情况下不得传播复制
 */
package com.bhm.ble.control

import com.bhm.ble.callback.BleConnectCallback
import com.bhm.ble.data.BleDevice


/**
 * 用来维护连接设备的连接、断开、读、写等操作
 * 一个设备对应一个BleDeviceController对象
 *
 * @author Buhuiming
 * @date 2023年05月26日 08时54分
 */
internal class BleDeviceController(val bleDevice: BleDevice) {

    @Synchronized
    fun connect(bleConnectCallback: BleConnectCallback) {

    }

    @Synchronized
    fun disconnect() {

    }

    fun getKey() = bleDevice.getKey()
}