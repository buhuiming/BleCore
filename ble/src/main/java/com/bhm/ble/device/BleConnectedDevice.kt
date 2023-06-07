/*
 * Copyright (c) 2022-2032 buhuiming
 * 不能修改和删除上面的版权声明
 * 此代码属于buhuiming编写，在未经允许的情况下不得传播复制
 */
package com.bhm.ble.device

import android.bluetooth.BluetoothGatt
import com.bhm.ble.callback.*
import com.bhm.ble.request.BleConnectRequest


/**
 * 一个已连接设备的管理类
 *
 * @author Buhuiming
 * @date 2023年06月07日 11时48分
 */
internal class BleConnectedDevice(val bleDevice: BleDevice) {

    private val bleConnectRequest = BleConnectRequest(bleDevice)

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
        bleConnectRequest.enableCharacteristicNotify(
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
        return bleConnectRequest.disableCharacteristicNotify(
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
        bleConnectRequest.enableCharacteristicIndicate(
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
        return bleConnectRequest.disableCharacteristicIndicate(
            serviceUUID,
            indicateUUID,
            useCharacteristicDescriptor
        )
    }

    /**
     * 读取信号值
     */
    fun readRemoteRssi(bleRssiCallback: BleRssiCallback) {
        bleConnectRequest.readRemoteRssi(bleRssiCallback)
    }

    /**
     * 设置mtu
     */
    fun setMtu(mtu: Int, bleMtuChangedCallback: BleMtuChangedCallback) {
        bleConnectRequest.setMtu(mtu, bleMtuChangedCallback)
    }

    /**
     * 设置设备的优先级
     * connectionPriority 必须是 [BluetoothGatt.CONNECTION_PRIORITY_BALANCED]、
     * [BluetoothGatt.CONNECTION_PRIORITY_HIGH]、
     * [BluetoothGatt.CONNECTION_PRIORITY_LOW_POWER]的其中一个
     *
     */
    fun setConnectionPriority(connectionPriority: Int): Boolean {
        return bleConnectRequest.setConnectionPriority(connectionPriority)
    }

    fun removeNotifyCallback(uuid: String?) {
        bleConnectRequest.removeNotifyCallback(uuid)
    }

    @Synchronized
    fun removeIndicateCallback(uuid: String?) {
        bleConnectRequest.removeIndicateCallback(uuid)
    }

    @Synchronized
    fun removeWriteCallback(uuid: String?) {
        bleConnectRequest.removeWriteCallback(uuid)
    }

    @Synchronized
    fun removeReadCallback(uuid: String?) {
        bleConnectRequest.removeReadCallback(uuid)
    }

    @Synchronized
    fun removeRssiCallback() {
        bleConnectRequest.removeRssiCallback()
    }

    @Synchronized
    fun removeMtuChangedCallback() {
        bleConnectRequest.removeMtuChangedCallback()
    }

    fun removeBleConnectCallback() {
        bleConnectRequest.removeBleConnectCallback()
    }

    fun release() {
        bleConnectRequest.release()
    }
}