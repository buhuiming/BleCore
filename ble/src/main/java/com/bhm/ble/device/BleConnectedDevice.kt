/*
 * Copyright (c) 2022-2032 buhuiming
 * 不能修改和删除上面的版权声明
 * 此代码属于buhuiming编写，在未经允许的情况下不得传播复制
 */
package com.bhm.ble.device

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.util.SparseArray
import com.bhm.ble.callback.BleConnectCallback
import com.bhm.ble.callback.BleEventCallback
import com.bhm.ble.callback.BleIndicateCallback
import com.bhm.ble.callback.BleMtuChangedCallback
import com.bhm.ble.callback.BleNotifyCallback
import com.bhm.ble.callback.BleReadCallback
import com.bhm.ble.callback.BleRssiCallback
import com.bhm.ble.callback.BleWriteCallback
import com.bhm.ble.control.BleTaskQueue
import com.bhm.ble.data.BleDescriptorGetType
import com.bhm.ble.request.BleConnectRequest
import com.bhm.ble.request.BleIndicateRequest
import com.bhm.ble.request.BleMtuRequest
import com.bhm.ble.request.BleNotifyRequest
import com.bhm.ble.request.BleReadRequest
import com.bhm.ble.request.BleRssiRequest
import com.bhm.ble.request.BleSetPriorityRequest
import com.bhm.ble.request.BleWriteRequest


/**
 * 每个连接设备对应一个BleConnectedDevice对象
 * 每一个BleConnectedDevice对象包含一个请求队列、连接请求、Notify请求、Indicate请求、Rssi请求、mtu请求、
 * 设置优先级请求、读特征值数据请求、写数据请求
 *
 * @author Buhuiming
 * @date 2023年06月07日 11时48分
 */
internal class BleConnectedDevice(val bleDevice: BleDevice) : BluetoothGattCallback() {

    private var bleTaskQueue = BleTaskQueue("${bleDevice.deviceAddress}共享队列")

    private var bleConnectRequest: BleConnectRequest? = null

    private var bleSetPriorityRequest: BleSetPriorityRequest? = null

    private var bleRssiRequest: BleRssiRequest? = null

    private var bleMtuRequest: BleMtuRequest? = null

    private var bleNotifyRequest: BleNotifyRequest? = null

    private var bleIndicateRequest: BleIndicateRequest? = null

    private var bleReadRequest: BleReadRequest? = null

    private var bleWriteRequest: BleWriteRequest? = null

    private var bleEventCallback: BleEventCallback? = null

    private fun initBleConnectRequest() {
        if (bleConnectRequest == null) {
            bleConnectRequest = BleConnectRequest(bleDevice, this)
        }
    }

    private fun initBleSetPriorityRequest() {
        if (bleSetPriorityRequest == null) {
            bleSetPriorityRequest = BleSetPriorityRequest(bleDevice)
        }
    }

    private fun initBleRssiRequest() {
        if (bleRssiRequest == null) {
            bleRssiRequest = BleRssiRequest(bleDevice)
        }
    }

    private fun initBleMtuRequest() {
        if (bleMtuRequest == null) {
            bleMtuRequest = BleMtuRequest(bleDevice, bleTaskQueue)
        }
    }

    private fun initBleNotifyRequest() {
        if (bleNotifyRequest == null) {
            bleNotifyRequest = BleNotifyRequest(bleDevice)
        }
    }

    private fun initBleIndicateRequest() {
        if (bleIndicateRequest == null) {
            bleIndicateRequest = BleIndicateRequest(bleDevice)
        }
    }

    private fun initBleReadRequest() {
        if (bleReadRequest == null) {
            bleReadRequest = BleReadRequest(bleDevice)
        }
    }

    private fun initBleWriteRequest() {
        if (bleWriteRequest == null) {
            bleWriteRequest = BleWriteRequest(bleDevice)
        }
    }

    /**
     * 当连接上设备或者失去连接时会触发
     */
    override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
        super.onConnectionStateChange(gatt, status, newState)
        bleConnectRequest?.onConnectionStateChange(gatt, status, newState)
    }

    /**
     * 当设备是否找到服务[bluetoothGatt?.discoverServices()]时会触发
     */
    override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
        super.onServicesDiscovered(gatt, status)
        bleConnectRequest?.onServicesDiscovered(gatt, status)
    }

    /**
     * 设备发出通知时会时会触发
     */
    override fun onCharacteristicChanged(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray
    ) {
        /*android 13调用的方法*/
//        super.onCharacteristicChanged(gatt, characteristic, value)
        val properties = characteristic.properties
        if (properties and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0) {
            // 这是 Indicate
            bleIndicateRequest?.onCharacteristicChanged(characteristic, value)
            bleEventCallback?.callCharacteristicChanged(characteristic.uuid?.toString(), 2, bleDevice, value)
        } else if (properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) {
            // 这是 Notify
            bleNotifyRequest?.onCharacteristicChanged(characteristic, value)
            bleEventCallback?.callCharacteristicChanged(characteristic.uuid?.toString(), 1, bleDevice,  value)
        }
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onCharacteristicChanged(
        gatt: BluetoothGatt?,
        characteristic: BluetoothGattCharacteristic?
    ) {
        super.onCharacteristicChanged(gatt, characteristic)
        /*android 13过时的方法*/
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//            return
//        }
        characteristic?.let {
            val properties = characteristic.properties
            if (properties and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0) {
                // 这是 Indicate
                bleIndicateRequest?.onCharacteristicChanged(it, it.value)
                bleEventCallback?.callCharacteristicChanged(it.uuid?.toString(), 2, bleDevice, it.value)
            } else if (properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) {
                // 这是 Notify
                bleNotifyRequest?.onCharacteristicChanged(it, it.value)
                bleEventCallback?.callCharacteristicChanged(it.uuid?.toString(), 1, bleDevice, it.value)
            } else {

            }
        }
    }

    /**
     * 当向设备Descriptor中写数据时会触发
     */
    override fun onDescriptorWrite(
        gatt: BluetoothGatt?,
        descriptor: BluetoothGattDescriptor?,
        status: Int
    ) {
        super.onDescriptorWrite(gatt, descriptor, status)
        bleNotifyRequest?.onDescriptorWrite(descriptor, status)
        bleIndicateRequest?.onDescriptorWrite(descriptor, status)
    }

    /**
     * 当读取设备数据时会触发
     */
    override fun onCharacteristicRead(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray,
        status: Int
    ) {
        //必须注释掉，否则会导致bleReadRequest?.onCharacteristicRead(characteristic, value, status)方法调用两次
//        super.onCharacteristicRead(gatt, characteristic, value, status)
        bleReadRequest?.onCharacteristicRead(characteristic, value, status)
    }

    /**
     * 兼容老的android系统版本，特别是华为(鸿蒙)系统个别版本会调用这个方法，而不触发[onCharacteristicRead]方法
     */
    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onCharacteristicRead(
        gatt: BluetoothGatt?,
        characteristic: BluetoothGattCharacteristic?,
        status: Int
    ) {
        super.onCharacteristicRead(gatt, characteristic, status)
        characteristic?.let {
            bleReadRequest?.onCharacteristicRead(characteristic, characteristic.value?: byteArrayOf(), status)
        }
    }

    /**
     * 当向Characteristic写数据时会触发
     */
    override fun onCharacteristicWrite(
        gatt: BluetoothGatt?,
        characteristic: BluetoothGattCharacteristic?,
        status: Int
    ) {
        super.onCharacteristicWrite(gatt, characteristic, status)
        bleWriteRequest?.onCharacteristicWrite(characteristic, status)
    }

    /**
     * 读取信号值后会触发
     */
    override fun onReadRemoteRssi(gatt: BluetoothGatt?, rssi: Int, status: Int) {
        super.onReadRemoteRssi(gatt, rssi, status)
        bleRssiRequest?.onReadRemoteRssi(rssi, status)
    }

    /**
     * 设置Mtu值后会触发
     */
    override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
        super.onMtuChanged(gatt, mtu, status)
        bleMtuRequest?.onMtuChanged(mtu, status)
        bleEventCallback?.callMtuChanged(mtu, bleDevice)
    }

    /**
     * 连接设备
     */
    fun connect(
        connectMillisTimeOut: Long?,
        connectRetryCount: Int?,
        connectRetryInterval: Long?,
        isForceConnect: Boolean = false,
        bleConnectCallback: BleConnectCallback
    ) {
        initBleConnectRequest()
        bleConnectRequest?.connect(
            connectMillisTimeOut,
            connectRetryCount,
            connectRetryInterval,
            isForceConnect,
            bleConnectCallback
        )
    }

    /**
     * 主动断开连接，上层API调用
     */
    fun disConnect() {
        bleConnectRequest?.disConnect()
    }

    /**
     * 取消/停止连接
     */
    fun stopConnect() {
        bleConnectRequest?.stopConnect()
    }

    /**
     * 获取设备的BluetoothGatt对象
     */
    fun getBluetoothGatt(): BluetoothGatt? {
        initBleConnectRequest()
        return bleConnectRequest?.getBluetoothGatt()
    }

    /**
     * notify
     */
    @Synchronized
    fun enableCharacteristicNotify(serviceUUID: String,
                                   notifyUUID: String,
                                   bleDescriptorGetType: BleDescriptorGetType,
                                   bleNotifyCallback: BleNotifyCallback
    ) {
        initBleNotifyRequest()
        bleNotifyRequest?.enableCharacteristicNotify(
            serviceUUID,
            notifyUUID,
            bleDescriptorGetType,
            bleNotifyCallback
        )
    }

    /**
     * stop notify
     */
    @Synchronized
    fun disableCharacteristicNotify(serviceUUID: String,
                                    notifyUUID: String,
                                    bleDescriptorGetType: BleDescriptorGetType
    ): Boolean {
        initBleNotifyRequest()
        return bleNotifyRequest?.disableCharacteristicNotify(
            serviceUUID,
            notifyUUID,
            bleDescriptorGetType
        )?: false
    }

    /**
     * indicate
     */
    @Synchronized
    fun enableCharacteristicIndicate(serviceUUID: String,
                                     indicateUUID: String,
                                     bleDescriptorGetType: BleDescriptorGetType,
                                     bleIndicateCallback: BleIndicateCallback
    ) {
        initBleIndicateRequest()
        bleIndicateRequest?.enableCharacteristicIndicate(
            serviceUUID,
            indicateUUID,
            bleDescriptorGetType,
            bleIndicateCallback
        )
    }

    /**
     * stop indicate
     */
    @Synchronized
    fun disableCharacteristicIndicate(serviceUUID: String,
                                      indicateUUID: String,
                                      bleDescriptorGetType: BleDescriptorGetType
    ): Boolean {
        initBleIndicateRequest()
        return bleIndicateRequest?.disableCharacteristicIndicate(
            serviceUUID,
            indicateUUID,
            bleDescriptorGetType
        )?: false
    }

    /**
     * 读取信号值
     */
    @Synchronized
    fun readRemoteRssi(bleRssiCallback: BleRssiCallback) {
        initBleRssiRequest()
        bleRssiRequest?.readRemoteRssi(bleRssiCallback)
    }

    /**
     * 设置mtu
     */
    @Synchronized
    fun setMtu(mtu: Int, bleMtuChangedCallback: BleMtuChangedCallback) {
        initBleMtuRequest()
        bleMtuRequest?.setMtu(mtu, bleMtuChangedCallback)
    }

    /**
     * 设置设备的传输优先级
     * connectionPriority 必须是 [BluetoothGatt.CONNECTION_PRIORITY_BALANCED]、
     * [BluetoothGatt.CONNECTION_PRIORITY_HIGH]、
     * [BluetoothGatt.CONNECTION_PRIORITY_LOW_POWER]的其中一个
     *
     */
    @Synchronized
    fun setConnectionPriority(connectionPriority: Int): Boolean {
        initBleSetPriorityRequest()
        return bleSetPriorityRequest?.setConnectionPriority(connectionPriority)?: false
    }

    /**
     * 读特征值数据
     */
    @Synchronized
    fun readData(serviceUUID: String,
                 readUUID: String,
                 bleIndicateCallback: BleReadCallback) {
        initBleReadRequest()
        bleReadRequest?.readCharacteristic(serviceUUID, readUUID, bleIndicateCallback)
    }

    /**
     * 写数据
     */
    @Synchronized
    fun writeData(serviceUUID: String,
                  writeUUID: String,
                  dataArray: SparseArray<ByteArray>,
                  writeType: Int?,
                  bleWriteCallback: BleWriteCallback) {
        //以时间戳为id，来标记一次写操作
        initBleWriteRequest()
        bleWriteRequest?.writeData(
            serviceUUID,
            writeUUID,
            System.currentTimeMillis().toString(),
            dataArray,
            writeType,
            bleWriteCallback
        )
    }

    /**
     * 放入一个写队列，写成功，则从队列中取下一个数据，写失败，则重试[retryWriteCount]次
     * 与[writeData]的区别在于，[writeData]写成功，则从队列中取下一个数据，写失败，则不再继续写后面的数据
     */
    @Synchronized
    fun writeQueueData(serviceUUID: String,
                       writeUUID: String,
                       dataArray: SparseArray<ByteArray>,
                       skipErrorPacketData: Boolean = false,
                       retryWriteCount: Int = 0,
                       retryDelayTime: Long = 0L,
                       writeType: Int? = null,
                       bleWriteCallback: BleWriteCallback) {
        //以时间戳为id，来标记一次写操作
        initBleWriteRequest()
        bleWriteRequest?.writeQueueData(
            serviceUUID,
            writeUUID,
            System.currentTimeMillis().toString(),
            dataArray,
            skipErrorPacketData,
            retryWriteCount,
            retryDelayTime,
            writeType,
            bleWriteCallback
        )
    }

    fun getShareBleTaskQueue() = bleTaskQueue

    @Synchronized
    fun addBleEventCallback(bleEventCallback: BleEventCallback) {
        this.bleEventCallback = bleEventCallback
    }

    fun getBleEventCallback() = bleEventCallback

    @Synchronized
    fun removeNotifyCallback(uuid: String?) {
        bleNotifyRequest?.removeNotifyCallback(uuid)
    }

    @Synchronized
    fun removeIndicateCallback(uuid: String?) {
        bleIndicateRequest?.removeIndicateCallback(uuid)
    }

    @Synchronized
    fun removeWriteCallback(uuid: String?, bleWriteCallback: BleWriteCallback? = null) {
        bleWriteRequest?.removeWriteCallback(uuid, bleWriteCallback)
    }

    @Synchronized
    fun removeReadCallback(uuid: String?) {
        bleReadRequest?.removeReadCallback(uuid)
    }

    @Synchronized
    fun removeRssiCallback() {
        bleRssiRequest?.removeRssiCallback()
    }

    @Synchronized
    fun removeMtuChangedCallback() {
        bleMtuRequest?.removeMtuChangedCallback()
    }

    @Synchronized
    fun removeBleConnectCallback() {
        bleConnectRequest?.removeBleConnectCallback()
    }

    fun removeBleEventCallback() {
        bleEventCallback = null
    }

    @Synchronized
    fun replaceBleConnectCallback(bleConnectCallback: BleConnectCallback) {
        bleConnectRequest?.removeBleConnectCallback()
        bleConnectRequest?.addBleConnectCallback(bleConnectCallback)
    }

    @Synchronized
    fun removeAllCharacterCallback() {
        removeRssiCallback()
        removeMtuChangedCallback()
        clearCharacterCallback()
    }

    private fun clearCharacterCallback() {
        bleNotifyRequest?.removeAllNotifyCallback()
        bleIndicateRequest?.removeAllIndicateCallback()
        bleWriteRequest?.removeAllWriteCallback()
        bleReadRequest?.removeAllReadCallback()
        bleEventCallback = null
    }

    fun close() {
        bleNotifyRequest?.close()
        bleIndicateRequest?.close()
        bleReadRequest?.close()
        bleWriteRequest?.close()
        bleRssiRequest?.close()
        bleConnectRequest?.close()
        bleTaskQueue.clear()
        bleNotifyRequest = null
        bleIndicateRequest = null
        bleReadRequest = null
        bleWriteRequest = null
        bleRssiRequest = null
        bleConnectRequest = null
        bleEventCallback = null
    }
}