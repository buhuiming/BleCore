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
 * @description 这是个独立的回调，设备的连接状态发生变化、
 * indicate/notify收到数据、mtu改变会触发
 * 不影响其他callback的回调
 * @author Buhuiming
 * @date 2023年10月31日 15:07:38
 */
open class BleEventCallback : BleBaseCallback() {

    private var startConnect: (() -> Unit)? = null

    private var connectFail: ((bleDevice: BleDevice, connectFailType: BleConnectFailType) -> Unit)? = null

    private var disConnecting: ((isActiveDisConnected: Boolean, bleDevice: BleDevice,
                                 gatt: BluetoothGatt?, status: Int) -> Unit)? = null

    private var connected: ((bleDevice: BleDevice, gatt: BluetoothGatt?) -> Unit)? = null

    private var disConnected: ((isActiveDisConnected: Boolean, bleDevice: BleDevice,
                                gatt: BluetoothGatt?, status: Int) -> Unit)? = null

    private var characteristicChanged: ((uuid: String?, type: Int, bleDevice: BleDevice, data: ByteArray) -> Unit)? = null

    private var mtuChanged: ((mtu: Int, bleDevice: BleDevice) -> Unit)? = null

    /**
     * 已连接
     */
    fun onConnected(value: (bleDevice: BleDevice, gatt: BluetoothGatt?) -> Unit) {
        connected = value
    }

    /**
     * 收到数据
     * type = 1 notify方式；type = 2 indicate方式
     */
    fun onCharacteristicChanged(value: ((uuid: String?, type: Int, bleDevice: BleDevice, data: ByteArray) -> Unit)) {
        characteristicChanged = value
    }

    fun onMtuChanged(value: ((mtu: Int, bleDevice: BleDevice) -> Unit)) {
        mtuChanged = value
    }

    /**
     * 开始连接
     */
    fun onConnectStart(value: () -> Unit) {
        startConnect = value
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
            startConnect?.invoke()
        }
    }

    open fun callConnectFail(bleDevice: BleDevice, connectFailType: BleConnectFailType) {
        launchInMainThread {
            connectFail?.invoke(bleDevice, connectFailType)
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

    open fun callConnected(bleDevice: BleDevice, gatt: BluetoothGatt?) {
        launchInMainThread {
            connected?.invoke(bleDevice, gatt)
        }
    }

    open fun callCharacteristicChanged(uuid: String?, type: Int, bleDevice: BleDevice, data: ByteArray) {
        //数据处理的线程需要自行切换
        characteristicChanged?.invoke(uuid, type, bleDevice, data)
    }

    open fun callMtuChanged(mtu: Int, bleDevice: BleDevice) {
        launchInMainThread {
            mtuChanged?.invoke(mtu, bleDevice)
        }
    }
}