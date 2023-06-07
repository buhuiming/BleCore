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
import com.bhm.ble.data.*
import com.bhm.ble.data.Constants.INDICATE
import com.bhm.ble.data.Constants.NOTIFY
import com.bhm.ble.device.BleConnectedDeviceManager
import com.bhm.ble.device.BleDevice
import kotlinx.coroutines.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


/**
 * 操作实现
 *
 * @author Buhuiming
 * @date 2023年05月22日 10时41分
 */
internal class BleRequestImp private constructor() : BleBaseRequest {

    private val mainScope = MainScope()

    private val bleConnectedDeviceManager = BleConnectedDeviceManager.get()

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
     * 扫描并连接，如果扫描到多个设备，则会连接第一个
     */
    override fun startScanAndConnect(bleScanCallback: BleScanCallback.() -> Unit,
                                     bleConnectCallback: BleConnectCallback.() -> Unit) {
        val scanCallback = BleScanCallback()
        scanCallback.apply(bleScanCallback)
        val connectCallback = BleConnectCallback()
        connectCallback.apply(bleConnectCallback)

        var device: BleDevice? = null
        connectCallback.launchInMainThread {
            suspendCoroutine { continuation ->
                startScan {
                    onScanStart {
                        scanCallback.callScanStart()
                    }
                    onLeScan { bleDevice, currentScanCount ->
                        scanCallback.callLeScan(bleDevice, currentScanCount)
                        if (device == null) {
                            device = bleDevice
                            stopScan()
                        }
                    }
                    onLeScanDuplicateRemoval { bleDevice, currentScanCount ->
                        scanCallback.callLeScanDuplicateRemoval(bleDevice, currentScanCount)
                    }
                    onScanFail {
                        scanCallback.callScanFail(it)
                    }
                    onScanComplete { bleDeviceList, bleDeviceDuplicateRemovalList ->
                        scanCallback.callScanComplete(bleDeviceList, bleDeviceDuplicateRemovalList)
                        continuation.resume(device)
                    }
                }
            }
            if (device == null || device?.deviceInfo == null) {
                connectCallback.callConnectFail(
                    BleDevice(null,
                        "",
                        "",
                        0,
                        0,
                        null,
                        0
                    ), BleConnectFailType.ScanNullableBluetoothDevice)
                return@launchInMainThread
            }
            connect(device!!) {
                onConnectStart {
                    connectCallback.callConnectStart()
                }
                onConnectSuccess { bleDevice, gatt ->
                    connectCallback.callConnectSuccess(bleDevice, gatt)
                }
                onDisConnected { isActiveDisConnected, bleDevice, gatt, status ->
                    connectCallback.callDisConnected(isActiveDisConnected, bleDevice, gatt, status)
                }
                onConnectFail { bleDevice, connectFailType ->
                    connectCallback.callConnectFail(bleDevice, connectFailType)
                }
            }
        }
    }

    /**
     * 开始连接
     */
    override fun connect(bleDevice: BleDevice, bleConnectCallback: BleConnectCallback.() -> Unit) {
        val callback = BleConnectCallback()
        callback.apply(bleConnectCallback)
        bleConnectedDeviceManager
            .buildBleConnectedDevice(bleDevice)
            ?.connect(callback)
    }

    /**
     * 断开连接
     */
    override fun disConnect(bleDevice: BleDevice) {
        bleConnectedDeviceManager
            .getBleConnectedDevice(bleDevice)
            ?.disConnect()
    }

    /**
     * 是否已连接
     */
    override fun isConnected(bleDevice: BleDevice): Boolean {
        return bleConnectedDeviceManager.isContainDevice(bleDevice)
    }

    /**
     * 移除该设备的连接回调
     */
    override fun removeBleConnectCallback(bleDevice: BleDevice) {
        bleConnectedDeviceManager.getBleConnectedDevice(bleDevice)?.removeBleConnectCallback()
    }

    /**
     * 获取设备的BluetoothGatt对象
     */
    override fun getBluetoothGatt(bleDevice: BleDevice): BluetoothGatt? {
        return bleConnectedDeviceManager.getBleConnectedDevice(bleDevice)?.getBluetoothGatt()
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
        val request = bleConnectedDeviceManager.getBleConnectedDevice(bleDevice)
        request?.let {
            it.enableCharacteristicNotify(serviceUUID, notifyUUID, useCharacteristicDescriptor, callback)
            return
        }
        callback.callNotifyFail(BleNotificationFailType.UnConnectedFailType(NOTIFY))
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
        val request = bleConnectedDeviceManager.getBleConnectedDevice(bleDevice)
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
        val request = bleConnectedDeviceManager.getBleConnectedDevice(bleDevice)
        request?.let {
            it.enableCharacteristicIndicate(serviceUUID, indicateUUID, useCharacteristicDescriptor, callback)
            return
        }
        callback.callIndicateFail(BleNotificationFailType.UnConnectedFailType(INDICATE))
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
        val request = bleConnectedDeviceManager.getBleConnectedDevice(bleDevice)
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
        val request = bleConnectedDeviceManager.getBleConnectedDevice(bleDevice)
        request?.let {
            it.readRemoteRssi(callback)
            return
        }
        callback.callRssiFail(UnConnectedException("读取Rssi失败，设备未连接"))
    }

    /**
     * 设置mtu
     */
    override fun setMtu(bleDevice: BleDevice, mtu: Int, bleMtuChangedCallback: BleMtuChangedCallback.() -> Unit) {
        val callback = BleMtuChangedCallback()
        callback.apply(bleMtuChangedCallback)
        val request = bleConnectedDeviceManager.getBleConnectedDevice(bleDevice)
        request?.let {
            it.setMtu(mtu, callback)
            return
        }
        callback.callSetMtuFail(UnConnectedException("设置mtu失败，设备未连接"))
    }

    /**
     * 设置设备的优先级
     * connectionPriority 必须是 [BluetoothGatt.CONNECTION_PRIORITY_BALANCED]、
     * [BluetoothGatt.CONNECTION_PRIORITY_HIGH]、
     * [BluetoothGatt.CONNECTION_PRIORITY_LOW_POWER]的其中一个
     *
     */
    override fun setConnectionPriority(bleDevice: BleDevice, connectionPriority: Int): Boolean {
        val request = bleConnectedDeviceManager.getBleConnectedDevice(bleDevice)
        return request?.setConnectionPriority(connectionPriority)?: false
    }

    /**
     * 读数据
     */
    override fun readData(bleDevice: BleDevice,
                      serviceUUID: String,
                      readUUID: String,
                      bleIndicateCallback: BleReadCallback.() -> Unit) {
        val callback = BleReadCallback()
        callback.apply(bleIndicateCallback)
        val request = bleConnectedDeviceManager.getBleConnectedDevice(bleDevice)
        request?.let {
            it.readData(serviceUUID, readUUID, callback)
            return
        }
        callback.callReadFail(UnConnectedException("读数据失败，设备未连接"))
    }

    /**
     * 移除该设备的Indicate回调
     */
    override fun removeBleIndicateCallback(bleDevice: BleDevice, indicateUUID: String) {
        val request = bleConnectedDeviceManager.getBleConnectedDevice(bleDevice)
        request?.removeIndicateCallback(indicateUUID)
    }

    /**
     * 移除该设备的Notify回调
     */
    override fun removeBleNotifyCallback(bleDevice: BleDevice, notifyUUID: String) {
        val request = bleConnectedDeviceManager.getBleConnectedDevice(bleDevice)
        request?.removeNotifyCallback(notifyUUID)
    }

    /**
     * 移除该设备的Read回调
     */
    override fun removeBleReadCallback(bleDevice: BleDevice, readUUID: String) {
        val request = bleConnectedDeviceManager.getBleConnectedDevice(bleDevice)
        request?.removeReadCallback(readUUID)
    }

    /**
     * 移除该设备的MtuChanged回调
     */
    override fun removeBleMtuChangedCallback(bleDevice: BleDevice) {
        val request = bleConnectedDeviceManager.getBleConnectedDevice(bleDevice)
        request?.removeMtuChangedCallback()
    }

    /**
     * 移除该设备的Rssi回调
     */
    override fun removeBleRssiCallback(bleDevice: BleDevice) {
        val request = bleConnectedDeviceManager.getBleConnectedDevice(bleDevice)
        request?.removeRssiCallback()
    }

    /**
     * 移除该设备的Write回调
     */
    override fun removeBleWriteCallback(bleDevice: BleDevice, writeUUID: String) {
        val request = bleConnectedDeviceManager.getBleConnectedDevice(bleDevice)
        request?.removeWriteCallback(writeUUID)
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
        bleConnectedDeviceManager.releaseAll()
        BleTaskQueue.get().clear()
    }

    /**
     * 断开某个设备的连接 释放资源
     */
    override fun release(bleDevice: BleDevice) {
        bleConnectedDeviceManager.release(bleDevice)
    }
}