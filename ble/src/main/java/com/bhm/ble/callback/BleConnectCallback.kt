/*
 * Copyright (c) 2022-2032 buhuiming
 * 不能修改和删除上面的版权声明
 * 此代码属于buhuiming编写，在未经允许的情况下不得传播复制
 */
package com.bhm.ble.callback

import android.bluetooth.BluetoothGatt
import com.bhm.ble.BleManager
import com.bhm.ble.data.BleConnectFailType
import com.bhm.ble.device.BleDevice
import com.bhm.ble.log.BleLogger
import kotlinx.coroutines.delay


/**
 * Ble连接回调
 * 在某些型号手机上，connectGatt必须在主线程才能有效，所以把连接过程放在主线程，回调也在主线程
 * @author Buhuiming
 * @date 2023年05月24日 14时00分
 */
open class BleConnectCallback : BleBaseCallback() {

    private var start: (() -> Unit)? = null

    private var connectSuccess: ((bleDevice: BleDevice, gatt: BluetoothGatt?) -> Unit)? = null

    private var connectFail: ((bleDevice: BleDevice, connectFailType: BleConnectFailType) -> Unit)? = null

    private var disConnecting: ((isActiveDisConnected: Boolean, bleDevice: BleDevice,
                                gatt: BluetoothGatt?, status: Int) -> Unit)? = null

    private var disConnected: ((isActiveDisConnected: Boolean, bleDevice: BleDevice,
                                gatt: BluetoothGatt?, status: Int) -> Unit)? = null

    /**
     * 开始连接
     */
    fun onConnectStart(value: () -> Unit) {
        start = value
    }

    /**
     * 连接成功
     */
    fun onConnectSuccess(value: (bleDevice: BleDevice, gatt: BluetoothGatt?) -> Unit) {
        connectSuccess = value
    }

    /**
     * 连接失败
     */
    fun onConnectFail(value: (bleDevice: BleDevice, connectFailType: BleConnectFailType) -> Unit) {
        connectFail = value
    }

    /**
     * 触发断开，此时的设备有可能还是连接状态，未完全断开
     */
    fun onDisConnecting(value: (isActiveDisConnected: Boolean, bleDevice: BleDevice,
                               gatt: BluetoothGatt?, status: Int) -> Unit) {
        disConnecting = value
    }

    /**
     * 连接断开，特指连接后再断开的情况。在这里可以监控设备的连接状态，一旦连接断开，可以根据自身情况考虑对BleDevice
     * 对象进行重连操作。需要注意的是，断开和重连之间最好间隔一段时间，否则可能会出现长时间连接不上的情况。此外，
     * 如果通过调用[com.bhm.ble.BleManager.disConnect]方法，主动断开蓝牙连接的结果也会在这个方法中回调，
     * 此时isActiveDisConnected将会是true。
     */
    fun onDisConnected(value: (isActiveDisConnected: Boolean, bleDevice: BleDevice,
                               gatt: BluetoothGatt?, status: Int) -> Unit) {
        disConnected = value
    }

    open fun callConnectStart() {
        launchInMainThread {
            start?.invoke()
        }
    }

    open fun callConnectFail(bleDevice: BleDevice, connectFailType: BleConnectFailType) {
        launchInMainThread {
            connectFail?.invoke(bleDevice, connectFailType)
        }
    }

    open fun callConnectSuccess(bleDevice: BleDevice, gatt: BluetoothGatt?) {
        launchInMainThread {
            connectSuccess?.invoke(bleDevice, gatt)
        }
    }

    open fun callDisConnecting(isActiveDisConnected: Boolean, bleDevice: BleDevice,
                              gatt: BluetoothGatt?, status: Int) {
        launchInMainThread {
            disConnecting?.invoke(isActiveDisConnected, bleDevice, gatt, status)
        }
        callDisConnected(isActiveDisConnected, bleDevice, gatt, status)
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
}