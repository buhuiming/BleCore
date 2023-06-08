/*
 * Copyright (c) 2022-2032 buhuiming
 * 不能修改和删除上面的版权声明
 * 此代码属于buhuiming编写，在未经允许的情况下不得传播复制
 */
@file:Suppress("SENSELESS_COMPARISON")

package com.bhm.ble.request

import android.bluetooth.BluetoothGatt
import android.util.SparseArray
import com.bhm.ble.callback.*
import com.bhm.ble.control.*
import com.bhm.ble.data.*
import com.bhm.ble.device.BleConnectedDeviceManager
import com.bhm.ble.device.BleDevice
import com.bhm.ble.utils.BleLogger
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

    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val defaultScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

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

    fun getIOScope() = ioScope

    fun getDefaultScope() = defaultScope

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
        val request = bleConnectedDeviceManager.buildBleConnectedDevice(bleDevice)
        request?.let {
            it.connect(callback)
            return
        }
        val exception = UnDefinedException("${bleDevice.deviceAddress} -> 连接失败，BleConnectedDevice为空")
        BleLogger.e(exception.message)
        callback.callConnectFail(bleDevice, BleConnectFailType.ConnectException(exception))
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
            it.enableCharacteristicNotify(
                serviceUUID,
                notifyUUID,
                useCharacteristicDescriptor,
                callback
            )
            return
        }
        val exception = UnConnectedException("$notifyUUID -> 设置Notify失败，设备未连接")
        BleLogger.e(exception.message)
        callback.callNotifyFail(exception)
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
            return it.disableCharacteristicNotify(
                serviceUUID,
                notifyUUID,
                useCharacteristicDescriptor
            )
        }
        BleLogger.e("$notifyUUID -> StopNotify失败，设备未连接")
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
            it.enableCharacteristicIndicate(
                serviceUUID,
                indicateUUID,
                useCharacteristicDescriptor,
                callback
            )
            return
        }
        val exception = UnConnectedException("$indicateUUID -> 设置Indicate失败，设备未连接")
        BleLogger.e(exception.message)
        callback.callIndicateFail(exception)
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
            return it.disableCharacteristicIndicate(
                serviceUUID,
                indicateUUID,
                useCharacteristicDescriptor
            )
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
        val exception = UnConnectedException("${bleDevice.deviceAddress} -> 读取Rssi失败，设备未连接")
        BleLogger.e(exception.message)
        callback.callRssiFail(exception)
    }

    /**
     * 设置mtu
     */
    override fun setMtu(bleDevice: BleDevice,
                        mtu: Int,
                        bleMtuChangedCallback: BleMtuChangedCallback.() -> Unit) {
        val callback = BleMtuChangedCallback()
        callback.apply(bleMtuChangedCallback)
        val request = bleConnectedDeviceManager.getBleConnectedDevice(bleDevice)
        request?.let {
            it.setMtu(mtu, callback)
            return
        }
        val exception = UnConnectedException("${bleDevice.deviceAddress} -> 设置mtu失败，设备未连接")
        BleLogger.e(exception.message)
        callback.callSetMtuFail(exception)
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
     * 读特征值数据
     */
    override fun readData(bleDevice: BleDevice,
                          serviceUUID: String,
                          readUUID: String,
                          bleReadCallback: BleReadCallback.() -> Unit) {
        val callback = BleReadCallback()
        callback.apply(bleReadCallback)
        val request = bleConnectedDeviceManager.getBleConnectedDevice(bleDevice)
        request?.let {
            it.readData(serviceUUID, readUUID, callback)
            return
        }
        val exception = UnConnectedException("$readUUID -> 读特征值数据失败，设备未连接")
        BleLogger.e(exception.message)
        callback.callReadFail(exception)
    }

    /**
     * 写数据
     * 注意：因为分包后每一个包，可能是包含完整的协议，所以分包由业务层处理，组件只会根据包的长度和mtu值对比后是否拦截
     */
    override fun writeData(bleDevice: BleDevice,
                  serviceUUID: String,
                  writeUUID: String,
                  dataArray: SparseArray<ByteArray>,
                  bleWriteCallback: BleWriteCallback.() -> Unit) {
        val callback = BleWriteCallback()
        callback.apply(bleWriteCallback)
        val request = bleConnectedDeviceManager.getBleConnectedDevice(bleDevice)
        request?.let {
            it.writeData(serviceUUID, writeUUID, dataArray, callback)
            return
        }
        val exception = UnConnectedException("$writeUUID -> 写数据失败，设备未连接")
        BleLogger.e(exception.message)
        callback.callWriteFail(exception)
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
     * 移除该设备回调，BleConnectCallback除外
     */
    override fun removeAllCharacterCallback(bleDevice: BleDevice) {
        val request = bleConnectedDeviceManager.getBleConnectedDevice(bleDevice)
        request?.removeAllCharacterCallback()
    }

    /**
     * 断开所有连接 释放资源
     */
    override fun releaseAll() {
        mainScope.cancel()
        ioScope.cancel()
        defaultScope.cancel()
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