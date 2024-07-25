/*
 * Copyright (c) 2022-2032 buhuiming
 * 不能修改和删除上面的版权声明
 * 此代码属于buhuiming编写，在未经允许的情况下不得传播复制
 */

package com.bhm.ble.request.base

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.content.Context
import android.content.IntentFilter
import android.os.Build
import android.util.SparseArray
import com.bhm.ble.BleManager
import com.bhm.ble.callback.BleConnectCallback
import com.bhm.ble.callback.BleEventCallback
import com.bhm.ble.callback.BleIndicateCallback
import com.bhm.ble.callback.BleMtuChangedCallback
import com.bhm.ble.callback.BleNotifyCallback
import com.bhm.ble.callback.BleReadCallback
import com.bhm.ble.callback.BleRssiCallback
import com.bhm.ble.callback.BleScanCallback
import com.bhm.ble.callback.BleWriteCallback
import com.bhm.ble.callback.BluetoothCallback
import com.bhm.ble.data.BleConnectFailType
import com.bhm.ble.data.BleDescriptorGetType
import com.bhm.ble.data.UnConnectedException
import com.bhm.ble.data.UnDefinedException
import com.bhm.ble.device.BleConnectedDeviceManager
import com.bhm.ble.device.BleDevice
import com.bhm.ble.receiver.BluetoothReceiver
import com.bhm.ble.request.BleScanRequest
import com.bhm.ble.log.BleLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
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

    private var bluetoothReceiver: BluetoothReceiver? = null

    companion object {

        private var instance: BleRequestImp? = BleRequestImp()

        fun get(): BleRequestImp {
            return instance?: BleRequestImp()
        }
    }

    fun getMainScope() = mainScope

    fun getIOScope() = ioScope

    fun getDefaultScope() = defaultScope

    /**
     * 开始扫描
     */
    override fun startScan(
        scanMillisTimeOut: Long?,
        scanRetryCount: Int?,
        scanRetryInterval: Long?,
        bleScanCallback: BleScanCallback.() -> Unit
    ) {
        val callback = BleScanCallback()
        callback.apply(bleScanCallback)
        BleScanRequest.get().startScan(
            scanMillisTimeOut,
            scanRetryCount,
            scanRetryInterval,
            callback
        )
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
    override fun startScanAndConnect(scanMillisTimeOut: Long?,
                                     scanRetryCount: Int?,
                                     scanRetryInterval: Long?,
                                     connectMillisTimeOut: Long?,
                                     connectRetryCount: Int?,
                                     connectRetryInterval: Long?,
                                     isForceConnect: Boolean,
                                     bleScanCallback: BleScanCallback.() -> Unit,
                                     bleConnectCallback: BleConnectCallback.() -> Unit) {
        val scanCallback = BleScanCallback()
        scanCallback.apply(bleScanCallback)
        val connectCallback = BleConnectCallback()
        connectCallback.apply(bleConnectCallback)

        var device: BleDevice? = null
        connectCallback.launchInMainThread {
            suspendCoroutine { continuation ->
                startScan(
                    scanMillisTimeOut,
                    scanRetryCount,
                    scanRetryInterval,
                ) {
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
                        try {
                            continuation.resume(device)
                        } catch (e: Exception) {
                            BleLogger.e(e.message)
                        }
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
                        null,
                    ), BleConnectFailType.ScanNullableBluetoothDevice)
                return@launchInMainThread
            }
            connect(
                device!!,
                connectMillisTimeOut,
                connectRetryCount,
                connectRetryInterval,
                isForceConnect
            ) {
                onConnectStart {
                    connectCallback.callConnectStart()
                    bleConnectedDeviceManager.getBleConnectedDevice(device!!)?.getBleEventCallback()?.callConnectStart()
                }
                onConnectSuccess { bleDevice, gatt ->
                    connectCallback.callConnectSuccess(bleDevice, gatt)
                    bleConnectedDeviceManager.getBleConnectedDevice(device!!)?.getBleEventCallback()?.callConnected(bleDevice, gatt)
                }
                onDisConnecting { isActiveDisConnected, bleDevice, gatt, status ->
                    connectCallback.callDisConnecting(isActiveDisConnected, bleDevice, gatt, status)
                    bleConnectedDeviceManager.getBleConnectedDevice(device!!)?.getBleEventCallback()?.callDisConnecting(
                        isActiveDisConnected, bleDevice, gatt, status
                    )
                }
                onDisConnected { isActiveDisConnected, bleDevice, gatt, status ->
                    connectCallback.callDisConnected(isActiveDisConnected, bleDevice, gatt, status)
                    bleConnectedDeviceManager.getBleConnectedDevice(device!!)?.getBleEventCallback()?.callDisConnected(
                        isActiveDisConnected, bleDevice, gatt, status
                    )
                }
                onConnectFail { bleDevice, connectFailType ->
                    connectCallback.callConnectFail(bleDevice, connectFailType)
                    bleConnectedDeviceManager.getBleConnectedDevice(device!!)?.getBleEventCallback()?.callConnectFail(
                        bleDevice, connectFailType
                    )
                }
            }
        }
    }

    /**
     * 开始连接
     */
    override fun connect(
        bleDevice: BleDevice,
        connectMillisTimeOut: Long?,
        connectRetryCount: Int?,
        connectRetryInterval: Long?,
        isForceConnect: Boolean,
        bleConnectCallback: BleConnectCallback.() -> Unit
    ) {
        val callback = BleConnectCallback()
        callback.apply(bleConnectCallback)
        val request = bleConnectedDeviceManager.buildBleConnectedDevice(bleDevice)
        request?.let {
            it.connect(
                connectMillisTimeOut,
                connectRetryCount,
                connectRetryInterval,
                isForceConnect,
                callback
            )
            return
        }
        val exception = UnDefinedException("${bleDevice.deviceAddress} -> 连接失败，BleConnectedDevice为空")
        BleLogger.e(exception.message)
        callback.callConnectFail(bleDevice, BleConnectFailType.ConnectException(exception))
        bleConnectedDeviceManager.getBleConnectedDevice(bleDevice)?.getBleEventCallback()?.callConnectFail(
            bleDevice, BleConnectFailType.ConnectException(exception))
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
     * 取消/停止连接
     */
    override fun stopConnect(bleDevice: BleDevice) {
        bleConnectedDeviceManager
            .getBleConnectedDevice(bleDevice)
            ?.stopConnect()
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
     * 替换该设备的连接回调
     */
    override fun replaceBleConnectCallback(bleDevice: BleDevice, bleConnectCallback: BleConnectCallback.() -> Unit) {
        val callback = BleConnectCallback()
        callback.apply(bleConnectCallback)
        val request = bleConnectedDeviceManager.getBleConnectedDevice(bleDevice)
        request?.replaceBleConnectCallback(callback)
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
                        bleDescriptorGetType: BleDescriptorGetType,
                        bleNotifyCallback: BleNotifyCallback.() -> Unit) {
        val callback = BleNotifyCallback()
        callback.apply(bleNotifyCallback)
        val request = bleConnectedDeviceManager.getBleConnectedDevice(bleDevice)
        request?.let {
            it.enableCharacteristicNotify(
                serviceUUID,
                notifyUUID,
                bleDescriptorGetType,
                callback
            )
            return
        }
        val exception = UnConnectedException("$notifyUUID -> 设置Notify失败，设备未连接")
        BleLogger.e(exception.message)
        callback.callNotifyFail(bleDevice, notifyUUID, exception)
    }

    /**
     * stop notify
     */
    override fun stopNotify(
        bleDevice: BleDevice,
        serviceUUID: String,
        notifyUUID: String,
        bleDescriptorGetType: BleDescriptorGetType
    ): Boolean {
        val request = bleConnectedDeviceManager.getBleConnectedDevice(bleDevice)
        request?.let {
            return it.disableCharacteristicNotify(
                serviceUUID,
                notifyUUID,
                bleDescriptorGetType
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
                          bleDescriptorGetType: BleDescriptorGetType,
                          bleIndicateCallback: BleIndicateCallback.() -> Unit) {
        val callback = BleIndicateCallback()
        callback.apply(bleIndicateCallback)
        val request = bleConnectedDeviceManager.getBleConnectedDevice(bleDevice)
        request?.let {
            it.enableCharacteristicIndicate(
                serviceUUID,
                indicateUUID,
                bleDescriptorGetType,
                callback
            )
            return
        }
        val exception = UnConnectedException("$indicateUUID -> 设置Indicate失败，设备未连接")
        BleLogger.e(exception.message)
        callback.callIndicateFail(bleDevice, indicateUUID, exception)
    }

    /**
     * stop indicate
     */
    override fun stopIndicate(
        bleDevice: BleDevice,
        serviceUUID: String,
        indicateUUID: String,
        bleDescriptorGetType: BleDescriptorGetType
    ): Boolean {
        val request = bleConnectedDeviceManager.getBleConnectedDevice(bleDevice)
        request?.let {
            return it.disableCharacteristicIndicate(
                serviceUUID,
                indicateUUID,
                bleDescriptorGetType
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
        callback.callRssiFail(bleDevice, exception)
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
        callback.callSetMtuFail(bleDevice, exception)
    }

    /**
     * 设置设备的传输优先级
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
        callback.callReadFail(bleDevice, exception)
    }

    /**
     * 写数据
     * 注意：因为分包后每一个包，可能是包含完整的协议，所以分包由业务层处理，组件只会根据包的长度和mtu值对比后是否拦截
     */
    override fun writeData(bleDevice: BleDevice,
                           serviceUUID: String,
                           writeUUID: String,
                           dataArray: SparseArray<ByteArray>,
                           writeType: Int?,
                           bleWriteCallback: BleWriteCallback.() -> Unit) {
        val callback = BleWriteCallback()
        callback.apply(bleWriteCallback)
        val request = bleConnectedDeviceManager.getBleConnectedDevice(bleDevice)
        request?.let {
            it.writeData(serviceUUID, writeUUID, dataArray, writeType, callback)
            return
        }
        val exception = UnConnectedException("$writeUUID -> 写数据失败，设备未连接")
        BleLogger.e(exception.message)
        callback.callWriteFail(bleDevice, 0, dataArray.size(), exception)
        callback.callWriteComplete(bleDevice, false)
    }

    /**
     * 放入一个写队列，写成功，则从队列中取下一个数据，写失败，则重试[retryWriteCount]次
     * 与[writeData]的区别在于，[writeData]写成功，则从队列中取下一个数据，写失败，则不再继续写后面的数据
     *
     * @param skipErrorPacketData 是否跳过数据长度为0的数据包
     * @param retryWriteCount 写失败后重试的次数
     */
    override fun writeQueueData(
        bleDevice: BleDevice,
        serviceUUID: String,
        writeUUID: String,
        dataArray: SparseArray<ByteArray>,
        skipErrorPacketData: Boolean,
        retryWriteCount: Int,
        retryDelayTime: Long,
        writeType: Int?,
        bleWriteCallback: BleWriteCallback.() -> Unit
    ) {
        val callback = BleWriteCallback()
        callback.apply(bleWriteCallback)
        val request = bleConnectedDeviceManager.getBleConnectedDevice(bleDevice)
        request?.let {
            it.writeQueueData(
                serviceUUID,
                writeUUID,
                dataArray,
                skipErrorPacketData,
                retryWriteCount,
                retryDelayTime,
                writeType,
                callback
            )
            return
        }
        val exception = UnConnectedException("$writeUUID -> 写数据失败，设备未连接")
        BleLogger.e(exception.message)
        callback.callWriteFail(bleDevice, 0, dataArray.size(), exception)
        callback.callWriteComplete(bleDevice, false)
    }

    /**
     * 获取所有已连接设备集合
     */
    override fun getAllConnectedDevice(): MutableList<BleDevice> {
        return bleConnectedDeviceManager.getAllConnectedDevice()
    }

    /**
     * 添加设备的连接状态发生变化、indicate/notify收到数据、mtu改变的回调
     */
    override fun addBleEventCallback(bleDevice: BleDevice, bleEventCallback: BleEventCallback.() -> Unit) {
        val callback = BleEventCallback()
        callback.apply(bleEventCallback)
        val request = bleConnectedDeviceManager.getBleConnectedDevice(bleDevice)
        request?.addBleEventCallback(callback)
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
    override fun removeBleWriteCallback(
        bleDevice: BleDevice,
        writeUUID: String,
        bleWriteCallback: BleWriteCallback?
    ) {
        val request = bleConnectedDeviceManager.getBleConnectedDevice(bleDevice)
        request?.removeWriteCallback(writeUUID, bleWriteCallback)
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
     * 移除该设备Event回调
     */
    override fun removeBleEventCallback(bleDevice: BleDevice) {
        val request = bleConnectedDeviceManager.getBleConnectedDevice(bleDevice)
        request?.removeBleEventCallback()
    }

    /**
     * 断开所有设备的连接
     */
    override fun disConnectAll() {
        bleConnectedDeviceManager.disConnectAll()
    }

    /**
     * 注册系统蓝牙广播
     */
    override fun registerBluetoothStateReceiver(bluetoothCallback: BluetoothCallback.() -> Unit) {
        if (bluetoothReceiver == null) {
            bluetoothReceiver = BluetoothReceiver()
            val callback = BluetoothCallback()
            callback.apply(bluetoothCallback)
            bluetoothReceiver?.setBluetoothCallback(callback)
            val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                BleManager.get().getContext()?.registerReceiver(
                    bluetoothReceiver,
                    filter,
                    Context.RECEIVER_EXPORTED
                )
            } else {
                BleManager.get().getContext()?.registerReceiver(
                    bluetoothReceiver,
                    filter
                )
            }
            BleLogger.d("注册系统蓝牙广播")
        }
    }

    /**
     * 取消注册系统蓝牙广播
     */
    override fun unRegisterBluetoothStateReceiver() {
        //取消注册系统蓝牙广播
        bluetoothReceiver?.let {
            BleManager.get().getContext()?.unregisterReceiver(it)
        }
        bluetoothReceiver = null
        BleLogger.d("取消注册系统蓝牙广播")
    }

    /**
     * 断开所有连接 释放资源
     */
    override fun closeAll() {
        mainScope.cancel()
        ioScope.cancel()
        defaultScope.cancel()
        unRegisterBluetoothStateReceiver()
        BleScanRequest.get().close()
        bleConnectedDeviceManager.closeAll()
        instance = null
    }

    /**
     * 断开某个设备的连接 释放资源
     */
    override fun close(bleDevice: BleDevice) {
        bleConnectedDeviceManager.close(bleDevice)
    }
}