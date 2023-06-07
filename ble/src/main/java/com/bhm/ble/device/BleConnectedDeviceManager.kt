/*
 * Copyright (c) 2022-2032 buhuiming
 * 不能修改和删除上面的版权声明
 * 此代码属于buhuiming编写，在未经允许的情况下不得传播复制
 */
@file:Suppress("SENSELESS_COMPARISON")

package com.bhm.ble.device

import com.bhm.ble.BleManager
import com.bhm.ble.attribute.BleOptions
import com.bhm.ble.control.BleLruHashMap
import com.bhm.ble.request.BleConnectRequest


/**
 * 连接设备BleConnectRequest管理池
 *
 * @author Buhuiming
 * @date 2023年05月26日 08时54分
 */
internal class BleConnectedDeviceManager private constructor() {

    private val bleLruHashMap: BleLruHashMap<String, BleConnectRequest?> =
        BleLruHashMap(BleManager.get().getOptions()?.maxConnectNum
            ?: BleOptions.DEFAULT_MAX_CONNECT_NUM)

    companion object {

        const val NOTIFY_TASK_ID = 1000

        const val INDICATE_TASK_ID = 1001

        const val SET_RSSI_TASK_ID = 1002

        const val SET_MTU_TASK_ID = 1003

        const val READ_TASK_ID = 1004

        const val WRITE_TASK_ID = 1005

        private var instance: BleConnectedDeviceManager = BleConnectedDeviceManager()

        @Synchronized
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
    fun buildBleConnectRequest(bleDevice: BleDevice): BleConnectRequest? {
        if (bleLruHashMap.containsKey(bleDevice.getKey())) {
            return bleLruHashMap[bleDevice.getKey()]
        }
        val bleConnectRequest = BleConnectRequest(bleDevice)
        bleLruHashMap[bleDevice.getKey()] = bleConnectRequest
        return bleConnectRequest
    }

    /**
     * 获取设备控制器
     */
    fun getBleConnectRequest(bleDevice: BleDevice): BleConnectRequest? {
        if (bleLruHashMap.containsKey(bleDevice.getKey())) {
            return bleLruHashMap[bleDevice.getKey()]
        }
        return null
    }

    /**
     * 移除设备控制器
     */
    @Synchronized
    fun removeBleConnectRequest(key: String) {
        if (bleLruHashMap.containsKey(key)) {
            bleLruHashMap.remove(key)
        }
    }

    /**
     * 是否存在该设备
     */
    @Synchronized
    fun isContainDevice(bleDevice: BleDevice): Boolean {
        return bleLruHashMap.containsKey(bleDevice.getKey())
    }

    /**
     * 断开某个设备的连接 释放资源
     */
    @Synchronized
    fun release(bleDevice: BleDevice) {
        getBleConnectRequest(bleDevice)?.release()
        bleLruHashMap.remove(bleDevice.getKey())
    }

    /**
     * 断开所有连接 释放资源
     */
    @Synchronized
    fun releaseAll() {
        bleLruHashMap.values.forEach {
            it?.release()
        }
        bleLruHashMap.clear()
    }
}