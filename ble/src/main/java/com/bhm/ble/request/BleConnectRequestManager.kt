/*
 * Copyright (c) 2022-2032 buhuiming
 * 不能修改和删除上面的版权声明
 * 此代码属于buhuiming编写，在未经允许的情况下不得传播复制
 */
@file:Suppress("SENSELESS_COMPARISON")

package com.bhm.ble.request

import com.bhm.ble.BleManager
import com.bhm.ble.attribute.BleOptions
import com.bhm.ble.control.BleLruHashMap
import com.bhm.ble.data.BleDevice


/**
 * 连接设备BleConnectRequest管理池
 *
 * @author Buhuiming
 * @date 2023年05月26日 08时54分
 */
internal class BleConnectRequestManager private constructor() {

    private val bleLruHashMap: BleLruHashMap<String, BleConnectRequest?> =
        BleLruHashMap(BleManager.get().getOptions()?.maxConnectNum
            ?: BleOptions.DEFAULT_MAX_CONNECT_NUM)

    companion object {

        private var instance: BleConnectRequestManager = BleConnectRequestManager()

        @Synchronized
        fun get(): BleConnectRequestManager {
            if (instance == null) {
                instance = BleConnectRequestManager()
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

    fun removeAll() {
        bleLruHashMap.clear()
    }
}