/*
 * Copyright (c) 2022-2032 buhuiming
 * 不能修改和删除上面的版权声明
 * 此代码属于buhuiming编写，在未经允许的情况下不得传播复制
 */
package com.bhm.ble.callback

import android.bluetooth.BluetoothGatt
import com.bhm.ble.BleManager
import com.bhm.ble.device.BleDevice
import com.bhm.ble.utils.BleLogger
import kotlinx.coroutines.delay

/**
 * @description 这是个独立的回调，设备的连接状态发生变化、
 * indicate/notify收到数据、mtu改变会触发
 * 不影响其他callback的回调
 * @author Buhuiming
 * @date 2023年10月31日 15:07:38
 */
open class BleEventCallback : BleBaseCallback() {

    private var connected: ((bleDevice: BleDevice, gatt: BluetoothGatt?) -> Unit)? = null

    private var disConnected: ((isActiveDisConnected: Boolean, bleDevice: BleDevice,
                                gatt: BluetoothGatt?, status: Int) -> Unit)? = null

    private var characteristicChanged: ((uuid: String?, type: Int, data: ByteArray) -> Unit)? = null

    private var mtuChanged: ((mtu: Int) -> Unit)? = null

    /**
     * 已连接
     */
    fun onConnected(value: (bleDevice: BleDevice, gatt: BluetoothGatt?) -> Unit) {
        connected = value
    }

    /**
     * 断开连接
     * isActiveDisConnected = true 主动断开
     */
    fun onDisConnected(value: (isActiveDisConnected: Boolean, bleDevice: BleDevice,
                               gatt: BluetoothGatt?, status: Int) -> Unit) {
        disConnected = value
    }

    /**
     * 收到数据
     * type = 1 notify方式；type = 2 indicate方式
     */
    fun onCharacteristicChanged(value: ((uuid: String?, type: Int, data: ByteArray) -> Unit)) {
        characteristicChanged = value
    }

    fun onMtuChanged(value: ((mtu: Int) -> Unit)) {
        mtuChanged = value
    }

    open fun callConnected(bleDevice: BleDevice, gatt: BluetoothGatt?) {
        launchInMainThread {
            connected?.invoke(bleDevice, gatt)
        }
    }

    open fun callDisConnected(isActiveDisConnected: Boolean, bleDevice: BleDevice,
                              gatt: BluetoothGatt?, status: Int) {
        launchInMainThread {
            val start = System.currentTimeMillis()
            while (BleManager.get().isConnected(bleDevice, true)) {
                //主动断开，需要等待gatt释放的时间更长一些
                delay(if (isActiveDisConnected) 80 else 4)
            }
            val end = System.currentTimeMillis()
            BleLogger.i("触发onDisConnecting，${(end - start)}毫秒后触发onDisConnected")
            disConnected?.invoke(isActiveDisConnected, bleDevice, gatt, status)
        }
    }

    open fun callCharacteristicChanged(uuid: String?, type: Int, data: ByteArray) {
        //数据处理放在非主线程
        launchInIOThread {
            characteristicChanged?.invoke(uuid, type, data)
        }
    }

    open fun callMtuChanged(mtu: Int) {
        launchInMainThread {
            mtuChanged?.invoke(mtu)
        }
    }
}