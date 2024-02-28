/*
 * Copyright (c) 2022-2032 buhuiming
 * 不能修改和删除上面的版权声明
 * 此代码属于buhuiming编写，在未经允许的情况下不得传播复制
 */
@file:Suppress("SENSELESS_COMPARISON")

package com.bhm.ble.device

import com.bhm.ble.BleManager
import com.bhm.ble.control.BleLruHashMap
import com.bhm.ble.data.Constants.DEFAULT_MAX_CONNECT_NUM


/**
 * 连接设备BleConnectedDevice管理池
 *
 * @author Buhuiming
 * @date 2023年05月26日 08时54分
 */
internal class BleConnectedDeviceManager private constructor() {

    private val bleLruHashMap: BleLruHashMap =
        BleLruHashMap(BleManager.get().getOptions()?.maxConnectNum
            ?: DEFAULT_MAX_CONNECT_NUM)

    companion object {

        private var instance: BleConnectedDeviceManager = BleConnectedDeviceManager()

        fun get(): BleConnectedDeviceManager {
            if (instance == null) {
                instance = BleConnectedDeviceManager()
            }
            return instance
        }
    }

    /**
     * 添加设备控制器
     */
    fun buildBleConnectedDevice(bleDevice: BleDevice): BleConnectedDevice? {
        if (bleLruHashMap.containsKey(bleDevice.getKey())) {
            return bleLruHashMap[bleDevice.getKey()]
        }
        val bleConnectedDevice = BleConnectedDevice(bleDevice)
        bleLruHashMap[bleDevice.getKey()] = bleConnectedDevice
        return bleConnectedDevice
    }

    /**
     * 获取设备控制器
     */
    fun getBleConnectedDevice(bleDevice: BleDevice): BleConnectedDevice? {
        if (bleLruHashMap.containsKey(bleDevice.getKey())) {
            return bleLruHashMap[bleDevice.getKey()]
        }
        return null
    }

    /**
     * 移除设备控制器
     */
    fun removeBleConnectedDevice(key: String) {
        if (bleLruHashMap.containsKey(key)) {
            bleLruHashMap.remove(key)
        }
    }

    /**
     * 是否存在该设备
     */
    fun isContainDevice(bleDevice: BleDevice): Boolean {
        return bleLruHashMap.containsKey(bleDevice.getKey())
    }

    /**
     * 获取所有已连接设备集合
     */
    fun getAllConnectedDevice(): MutableList<BleDevice> {
        val list = mutableListOf<BleDevice>()
        bleLruHashMap.forEach {
            it.value?.let { device ->
                if (BleManager.get().isConnected(device.bleDevice)) {
                    list.add(device.bleDevice)
                }
            }
        }
        return list
    }

    /**
     * 断开某个设备的连接 释放资源
     */
    fun close(bleDevice: BleDevice) {
        getBleConnectedDevice(bleDevice)?.close()
        bleLruHashMap.remove(bleDevice.getKey())
    }

    /**
     * 断开所有设备的连接
     */
    fun disConnectAll() {
        bleLruHashMap.values.forEach {
            it?.disConnect()

        }
        closeAll()
    }

    /**
     * 断开所有连接 释放资源
     */
    fun closeAll() {
        bleLruHashMap.values.forEach {
            it?.close()
        }
        bleLruHashMap.clear()
    }
}