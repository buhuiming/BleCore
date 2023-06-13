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
import com.bhm.ble.callback.*
import com.bhm.ble.control.BleTaskQueue
import com.bhm.ble.request.*


/**
 * 每个连接设备对应一个BleConnectedDevice对象
 * 每一个BleConnectedDevice对象包含一个请求队列、连接请求、Notify请求、Indicate请求、Rssi请求、mtu请求、
 * 设置优先级请求、读特征值数据请求、写数据请求
 *
 * @author Buhuiming
 * @date 2023年06月07日 11时48分
 */
internal class BleConnectedDevice(val bleDevice: BleDevice) : BluetoothGattCallback() {

    private var bleTaskQueue = BleTaskQueue()

    private val bleConnectRequest = BleConnectRequest(bleDevice, this)

    private val bleNotifyRequest = BleNotifyRequest(bleDevice, bleTaskQueue)

    private val bleIndicateRequest = BleIndicateRequest(bleDevice, bleTaskQueue)

    private val bleRssiRequest = BleRssiRequest(bleDevice, bleTaskQueue)

    private val bleMtuRequest = BleMtuRequest(bleDevice, bleTaskQueue)

    private val bleSetPriorityRequest = BleSetPriorityRequest(bleDevice)

    private val bleReadRequest = BleReadRequest(bleDevice, bleTaskQueue)

    private val bleWriteRequest = BleWriteRequest(bleDevice, bleTaskQueue)

    /**
     * 当连接上设备或者失去连接时会触发
     */
    override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
        super.onConnectionStateChange(gatt, status, newState)
        bleConnectRequest.onConnectionStateChange(gatt, status, newState)
    }

    /**
     * 当设备是否找到服务[bluetoothGatt?.discoverServices()]时会触发
     */
    override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
        super.onServicesDiscovered(gatt, status)
        bleConnectRequest.onServicesDiscovered(gatt, status)
    }

    /**
     * 设备发出通知时会时会触发
     */
    override fun onCharacteristicChanged(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray
    ) {
        super.onCharacteristicChanged(gatt, characteristic, value)
        bleNotifyRequest.onCharacteristicChanged(characteristic, value)
        bleIndicateRequest.onCharacteristicChanged(characteristic, value)
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
        bleNotifyRequest.onDescriptorWrite(descriptor, status)
        bleIndicateRequest.onDescriptorWrite(descriptor, status)
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
        super.onCharacteristicRead(gatt, characteristic, value, status)
        bleReadRequest.onCharacteristicRead(characteristic, value, status)
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
        bleWriteRequest.onCharacteristicWrite(characteristic, status)
    }

    /**
     * 读取信号值后会触发
     */
    override fun onReadRemoteRssi(gatt: BluetoothGatt?, rssi: Int, status: Int) {
        super.onReadRemoteRssi(gatt, rssi, status)
        bleRssiRequest.onReadRemoteRssi(rssi, status)
    }

    /**
     * 设置Mtu值后会触发
     */
    override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
        super.onMtuChanged(gatt, mtu, status)
        bleMtuRequest.onMtuChanged(mtu, status)
    }

    /**
     * 连接设备
     */
    fun connect(bleConnectCallback: BleConnectCallback) {
        bleConnectRequest.connect(bleConnectCallback)
    }

    /**
     * 主动断开连接，上层API调用
     */
    fun disConnect() {
        bleConnectRequest.disConnect()
    }

    /**
     * 获取设备的BluetoothGatt对象
     */
    fun getBluetoothGatt(): BluetoothGatt? {
        return bleConnectRequest.getBluetoothGatt()
    }

    /**
     * notify
     */
    fun enableCharacteristicNotify(serviceUUID: String,
                                   notifyUUID: String,
                                   useCharacteristicDescriptor: Boolean,
                                   bleNotifyCallback: BleNotifyCallback
    ) {
        bleNotifyRequest.enableCharacteristicNotify(
            serviceUUID,
            notifyUUID,
            useCharacteristicDescriptor,
            bleNotifyCallback
        )
    }

    /**
     * stop notify
     */
    fun disableCharacteristicNotify(serviceUUID: String,
                                    notifyUUID: String,
                                    useCharacteristicDescriptor: Boolean
    ): Boolean {
        return bleNotifyRequest.disableCharacteristicNotify(
            serviceUUID,
            notifyUUID,
            useCharacteristicDescriptor
        )
    }

    /**
     * indicate
     */
    fun enableCharacteristicIndicate(serviceUUID: String,
                                     indicateUUID: String,
                                     useCharacteristicDescriptor: Boolean,
                                     bleIndicateCallback: BleIndicateCallback
    ) {
        bleIndicateRequest.enableCharacteristicIndicate(
            serviceUUID,
            indicateUUID,
            useCharacteristicDescriptor,
            bleIndicateCallback
        )
    }

    /**
     * stop indicate
     */
    fun disableCharacteristicIndicate(serviceUUID: String,
                                      indicateUUID: String,
                                      useCharacteristicDescriptor: Boolean
    ): Boolean {
        return bleIndicateRequest.disableCharacteristicIndicate(
            serviceUUID,
            indicateUUID,
            useCharacteristicDescriptor
        )
    }

    /**
     * 读取信号值
     */
    fun readRemoteRssi(bleRssiCallback: BleRssiCallback) {
        bleRssiRequest.readRemoteRssi(bleRssiCallback)
    }

    /**
     * 设置mtu
     */
    fun setMtu(mtu: Int, bleMtuChangedCallback: BleMtuChangedCallback) {
        bleMtuRequest.setMtu(mtu, bleMtuChangedCallback)
    }

    /**
     * 设置设备的传输优先级
     * connectionPriority 必须是 [BluetoothGatt.CONNECTION_PRIORITY_BALANCED]、
     * [BluetoothGatt.CONNECTION_PRIORITY_HIGH]、
     * [BluetoothGatt.CONNECTION_PRIORITY_LOW_POWER]的其中一个
     *
     */
    fun setConnectionPriority(connectionPriority: Int): Boolean {
        return bleSetPriorityRequest.setConnectionPriority(connectionPriority)
    }

    /**
     * 读特征值数据
     */
    fun readData(serviceUUID: String,
                 readUUID: String,
                 bleIndicateCallback: BleReadCallback) {
        bleReadRequest.readCharacteristic(serviceUUID, readUUID, bleIndicateCallback)
    }

    /**
     * 写数据
     */
    fun writeData(serviceUUID: String,
                  writeUUID: String,
                  dataArray: SparseArray<ByteArray>,
                  bleWriteCallback: BleWriteCallback) {
        //以时间戳为id，来标记一次写操作
        bleWriteRequest.writeData(serviceUUID,
            writeUUID,
            System.currentTimeMillis().toString(),
            dataArray,
            bleWriteCallback
        )
    }

    fun removeNotifyCallback(uuid: String?) {
        bleNotifyRequest.removeNotifyCallback(uuid)
    }

    @Synchronized
    fun removeIndicateCallback(uuid: String?) {
        bleIndicateRequest.removeIndicateCallback(uuid)
    }

    @Synchronized
    fun removeWriteCallback(uuid: String?, bleWriteCallback: BleWriteCallback? = null) {
        bleWriteRequest.removeWriteCallback(uuid, bleWriteCallback)
    }

    @Synchronized
    fun removeReadCallback(uuid: String?) {
        bleReadRequest.removeReadCallback(uuid)
    }

    @Synchronized
    fun removeRssiCallback() {
        bleRssiRequest.removeRssiCallback()
    }

    @Synchronized
    fun removeMtuChangedCallback() {
        bleMtuRequest.removeMtuChangedCallback()
    }

    @Synchronized
    fun removeBleConnectCallback() {
        bleConnectRequest.removeBleConnectCallback()
    }

    @Synchronized
    fun removeAllCharacterCallback() {
        removeRssiCallback()
        removeMtuChangedCallback()
        clearCharacterCallback()
    }

    @Synchronized
    fun clearCharacterCallback() {
        bleNotifyRequest.removeAllNotifyCallback()
        bleIndicateRequest.removeAllIndicateCallback()
        bleWriteRequest.removeAllWriteCallback()
        bleReadRequest.removeAllReadCallback()
    }

    fun release() {
        bleConnectRequest.release()
        bleTaskQueue.clear()
    }
}