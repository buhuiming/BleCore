/*
 * Copyright (c) 2022-2032 buhuiming
 * 不能修改和删除上面的版权声明
 * 此代码属于buhuiming编写，在未经允许的情况下不得传播复制
 */
package com.bhm.ble.control

import android.bluetooth.BluetoothGatt
import android.os.Looper
import com.bhm.ble.callback.BleConnectCallback
import com.bhm.ble.callback.BleMtuChangedCallback
import com.bhm.ble.callback.BleRssiCallback
import com.bhm.ble.data.BleDevice


/**
 * 用来维护连接设备的连接、断开、读、写等操作
 * 一个设备对应一个BleDeviceController对象
 *
 * @author Buhuiming
 * @date 2023年05月26日 08时54分
 */
internal class BleDeviceController(val bleDevice: BleDevice) {

    private var bleConnectCallback: BleConnectCallback? = null

    private val bleRssiCallback: BleRssiCallback? = null

    private val bleMtuChangedCallback: BleMtuChangedCallback? = null

//    private val bleNotifyCallbackHashMap: HashMap<String, BleNotifyCallback> =
//        HashMap<String, BleNotifyCallback>()
//
//    private val bleIndicateCallbackHashMap: HashMap<String, BleIndicateCallback> =
//        HashMap<String, BleIndicateCallback>()
//
//    private val bleWriteCallbackHashMap: HashMap<String, BleWriteCallback> =
//        HashMap<String, BleWriteCallback>()
//
//    private val bleReadCallbackHashMap: HashMap<String, BleReadCallback> =
//        HashMap<String, BleReadCallback>()
//
//    private val lastState: LastState? = null

    private val isActiveDisconnect = false

    private val bluetoothGatt: BluetoothGatt? = null

    private val connectRetryCount = 0

    @Synchronized
    fun connect(bleConnectCallback: BleConnectCallback) {
        this.bleConnectCallback = bleConnectCallback
    }

    @Synchronized
    fun disconnect() {

    }

    fun getKey() = bleDevice.getKey()
}