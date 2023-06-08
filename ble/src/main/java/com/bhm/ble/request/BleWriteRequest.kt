/*
 * Copyright (c) 2022-2032 buhuiming
 * 不能修改和删除上面的版权声明
 * 此代码属于buhuiming编写，在未经允许的情况下不得传播复制
 */
package com.bhm.ble.request

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.util.SparseArray
import com.bhm.ble.callback.BleWriteCallback
import com.bhm.ble.control.BleTaskQueue
import com.bhm.ble.data.Constants
import com.bhm.ble.data.UnConnectedException
import com.bhm.ble.data.UnDefinedException
import com.bhm.ble.device.BleDevice
import com.bhm.ble.utils.BleLogger


/**
 * 设备写请求
 *
 * @author Buhuiming
 * @date 2023年06月07日 15时58分
 */
internal class BleWriteRequest(
    bleDevice: BleDevice,
    private val bleTaskQueue: BleTaskQueue
) : Request() {

    private val bleWriteCallbackHashMap: HashMap<String, BleWriteCallback> = HashMap()

    @Synchronized
    fun addWriteCallback(uuid: String, bleWriteCallback: BleWriteCallback) {
        bleWriteCallbackHashMap[uuid] = bleWriteCallback
    }

    @Synchronized
    fun removeWriteCallback(uuid: String?) {
        if (bleWriteCallbackHashMap.containsKey(uuid)) {
            bleWriteCallbackHashMap.remove(uuid)
        }
    }

    @Synchronized
    fun removeAllWriteCallback() {
        bleWriteCallbackHashMap.clear()
    }

    @Synchronized
    fun writeData(serviceUUID: String,
                  writeUUID: String,
                  dataArray: SparseArray<ByteArray>,
                  bleWriteCallback: BleWriteCallback) {
        if (dataArray.size() == 0) {
            val exception = UnDefinedException("$writeUUID -> 写数据失败，数据为空")
            BleLogger.e(exception.message)
            bleWriteCallback.callWriteFail(exception)
            return
        }
        for (i in 0..dataArray.size()) {
            val data = dataArray.get(i)
            if (data == null || data.isEmpty()) {
                val exception = UnDefinedException("$writeUUID -> 写数据失败，存在空数据包")
                BleLogger.e(exception.message)
                bleWriteCallback.callWriteFail(exception)
                return
            }
            val mtu = getBleOptions()?.mtu?: Constants.DEFAULT_MTU
            if (data.size > mtu) {
                val exception = UnDefinedException("$writeUUID -> 写数据失败，" +
                        "第${i + 1}个数据包长度(${data.size})大于设定Mtu($mtu)")
                BleLogger.e(exception.message)
                bleWriteCallback.callWriteFail(exception)
                return
            }
        }
    }



    /**
     * 当向Characteristic写数据时会触发
     */
    fun onCharacteristicWrite(
        gatt: BluetoothGatt?,
        characteristic: BluetoothGattCharacteristic?,
        status: Int
    ) {

    }
}