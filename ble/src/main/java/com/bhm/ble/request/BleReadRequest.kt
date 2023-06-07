/*
 * Copyright (c) 2022-2032 buhuiming
 * 不能修改和删除上面的版权声明
 * 此代码属于buhuiming编写，在未经允许的情况下不得传播复制
 */
package com.bhm.ble.request

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import com.bhm.ble.callback.BleReadCallback
import com.bhm.ble.control.BleTaskQueue
import com.bhm.ble.device.BleDevice


/**
 * 设备读请求
 *
 * @author Buhuiming
 * @date 2023年06月07日 15时58分
 */
internal class BleReadRequest(
    bleDevice: BleDevice,
    private val bleTaskQueue: BleTaskQueue
) : Request(){

    private val bleReadCallbackHashMap: HashMap<String, BleReadCallback> = HashMap()

    @Synchronized
    fun addReadCallback(uuid: String, bleReadCallback: BleReadCallback) {
        bleReadCallbackHashMap[uuid] = bleReadCallback
    }

    @Synchronized
    fun removeReadCallback(uuid: String?) {
        if (bleReadCallbackHashMap.containsKey(uuid)) {
            bleReadCallbackHashMap.remove(uuid)
        }
    }

    @Synchronized
    fun removeAllReadCallback() {
        bleReadCallbackHashMap.clear()
    }

    /**
     * 当读取设备时会触发
     */
    fun onCharacteristicRead(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray,
        status: Int
    ) {

    }
}