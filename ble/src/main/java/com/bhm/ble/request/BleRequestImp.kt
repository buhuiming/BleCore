/*
 * Copyright (c) 2022-2032 buhuiming
 * 不能修改和删除上面的版权声明
 * 此代码属于buhuiming编写，在未经允许的情况下不得传播复制
 */
@file:Suppress("SENSELESS_COMPARISON")

package com.bhm.ble.request

import android.bluetooth.BluetoothGatt
import com.bhm.ble.callback.*
import com.bhm.ble.control.*
import com.bhm.ble.data.BleDevice
import kotlinx.coroutines.*


/**
 * 操作实现
 *
 * @author Buhuiming
 * @date 2023年05月22日 10时41分
 */
internal class BleRequestImp private constructor() : BleBaseRequest {

    private val mainScope = MainScope()

    companion object {

        private var instance: BleRequestImp = BleRequestImp()

        @Synchronized
        fun get(): BleRequestImp {
            if (instance == null) {
                instance = BleRequestImp()
            }
            return instance
        }
    }

    fun getMainScope() = mainScope

    /**
     * 开始扫描
     */
    override fun startScan(bleScanCallback: BleScanCallback.() -> Unit) {
        val callback = BleScanCallback()
        callback.apply(bleScanCallback)
        BleScanRequest.get().startScan(callback)
    }

    /**
     * 是否扫描中
     */
    override fun isScanning(): Boolean {
        return BleScanRequest.get().isScanning()
    }

    /**
     * 停止扫描
     */
    override fun stopScan() {
        BleScanRequest.get().stopScan()
    }

    /**
     * 开始连接
     */
    override fun connect(bleDevice: BleDevice, bleConnectCallback: BleConnectCallback.() -> Unit) {
        val callback = BleConnectCallback()
        callback.apply(bleConnectCallback)
        BleConnectRequestManager.get()
            .buildBleConnectRequest(bleDevice)
            ?.connect(callback)
    }

    /**
     * 断开连接
     */
    override fun disConnect(bleDevice: BleDevice) {
        BleConnectRequestManager.get()
            .getBleConnectRequest(bleDevice)
            ?.disConnect()
    }

    /**
     * 是否已连接
     */
    override fun isConnected(bleDevice: BleDevice): Boolean {
        return BleConnectRequestManager.get().isContainDevice(bleDevice)
    }

    /**
     * 移除该设备的连接回调
     */
    override fun removeBleConnectCallback(bleDevice: BleDevice) {
        BleConnectRequestManager.get().getBleConnectRequest(bleDevice)?.removeBleConnectCallback()
    }

    /**
     * 获取设备的BluetoothGatt对象
     */
    override fun getBluetoothGatt(bleDevice: BleDevice): BluetoothGatt? {
        return BleConnectRequestManager.get().getBleConnectRequest(bleDevice)?.getBluetoothGatt()
    }

    /**
     * notify
     */
    override fun notify(bleDevice: BleDevice,
                        serviceUUID: String,
                        notifyUUID: String,
                        useCharacteristicDescriptor: Boolean,
                        bleNotifyCallback: BleNotifyCallback.() -> Unit) {
        val callback = BleNotifyCallback()
        callback.apply(bleNotifyCallback)
        val request = BleConnectRequestManager.get().getBleConnectRequest(bleDevice)
        request?.let {
            it.enableCharacteristicNotify(serviceUUID, notifyUUID, useCharacteristicDescriptor, callback)
            return
        }
        callback.callNotifyFail(NotificationFailException.UnConnectedException(NOTIFY))
    }

    /**
     * stop notify
     */
    override fun stopNotify(
        bleDevice: BleDevice,
        serviceUUID: String,
        notifyUUID: String,
        useCharacteristicDescriptor: Boolean
    ): Boolean {
        val request = BleConnectRequestManager.get().getBleConnectRequest(bleDevice)
        request?.let {
            return it.disableCharacteristicNotify(serviceUUID, notifyUUID, useCharacteristicDescriptor)
        }
        return false
    }

    /**
     * indicate
     */
    override fun indicate(bleDevice: BleDevice,
                          serviceUUID: String,
                          indicateUUID: String,
                          useCharacteristicDescriptor: Boolean,
                          bleIndicateCallback: BleIndicateCallback.() -> Unit) {
        val callback = BleIndicateCallback()
        callback.apply(bleIndicateCallback)
        val request = BleConnectRequestManager.get().getBleConnectRequest(bleDevice)
        request?.let {
            it.enableCharacteristicIndicate(serviceUUID, indicateUUID, useCharacteristicDescriptor, callback)
            return
        }
        callback.callIndicateFail(NotificationFailException.UnConnectedException(INDICATE))
    }

    /**
     * stop indicate
     */
    override fun stopIndicate(
        bleDevice: BleDevice,
        serviceUUID: String,
        indicateUUID: String,
        useCharacteristicDescriptor: Boolean
    ): Boolean {
        val request = BleConnectRequestManager.get().getBleConnectRequest(bleDevice)
        request?.let {
            return it.disableCharacteristicIndicate(serviceUUID, indicateUUID, useCharacteristicDescriptor)
        }
        return false
    }

    /**
     * 读取信号值
     */
    override fun readRssi(bleDevice: BleDevice, bleRssiCallback: BleRssiCallback.() -> Unit) {
        val callback = BleRssiCallback()
        callback.apply(bleRssiCallback)
        val request = BleConnectRequestManager.get().getBleConnectRequest(bleDevice)
        request?.let {
            it.readRemoteRssi(callback)
            return
        }
        callback.callRssiFail(UnConnectedThrowable("读取Rssi失败，设备未连接"))
    }

    /**
     * 设置mtu
     */
    override fun setMtu(bleDevice: BleDevice, mtu: Int, bleMtuChangedCallback: BleMtuChangedCallback.() -> Unit) {
        val callback = BleMtuChangedCallback()
        callback.apply(bleMtuChangedCallback)
        val request = BleConnectRequestManager.get().getBleConnectRequest(bleDevice)
        request?.let {
            it.setMtu(mtu, callback)
            return
        }
        callback.callSetMtuFail(UnConnectedThrowable("设置mtu失败，设备未连接"))
    }

    /**
     * 移除该设备的Indicate回调
     */
    override fun removeBleIndicateCallback(bleDevice: BleDevice, indicateUUID: String) {
        val request = BleConnectRequestManager.get().getBleConnectRequest(bleDevice)
        request?.let {
            return it.removeIndicateCallback(indicateUUID)
        }
    }

    /**
     * 移除该设备的Notify回调
     */
    override fun removeBleNotifyCallback(bleDevice: BleDevice, notifyUUID: String) {
        val request = BleConnectRequestManager.get().getBleConnectRequest(bleDevice)
        request?.let {
            return it.removeNotifyCallback(notifyUUID)
        }
    }

    /**
     * 移除该设备的Read回调
     */
    override fun removeBleReadCallback(bleDevice: BleDevice, readUUID: String) {
        val request = BleConnectRequestManager.get().getBleConnectRequest(bleDevice)
        request?.let {
            return it.removeReadCallback(readUUID)
        }
    }

    /**
     * 移除该设备的MtuChanged回调
     */
    override fun removeBleMtuChangedCallback(bleDevice: BleDevice) {
        val request = BleConnectRequestManager.get().getBleConnectRequest(bleDevice)
        request?.let {
            return it.removeMtuChangedCallback()
        }
    }

    /**
     * 移除该设备的Rssi回调
     */
    override fun removeBleRssiCallback(bleDevice: BleDevice) {
        val request = BleConnectRequestManager.get().getBleConnectRequest(bleDevice)
        request?.let {
            return it.removeRssiCallback()
        }
    }

    /**
     * 移除该设备的Write回调
     */
    override fun removeBleWriteCallback(bleDevice: BleDevice, writeUUID: String) {
        val request = BleConnectRequestManager.get().getBleConnectRequest(bleDevice)
        request?.let {
            return it.removeWriteCallback(writeUUID)
        }
    }

    /**
     * 移除该设备的Scan回调
     */
    override fun removeBleScanCallback() {
        BleScanRequest.get().removeBleScanCallback()
    }

    /**
     * 断开所有连接 释放资源
     */
    override fun releaseAll() {
        mainScope.cancel()
        BleConnectRequestManager.get().releaseAll()
        BleTaskQueue.get().clear()
    }

    /**
     * 断开某个设备的连接 释放资源
     */
    override fun release(bleDevice: BleDevice) {
        BleConnectRequestManager.get().release(bleDevice)
    }
}