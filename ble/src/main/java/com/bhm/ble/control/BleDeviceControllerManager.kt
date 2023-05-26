/*
 * Copyright (c) 2022-2032 buhuiming
 * 不能修改和删除上面的版权声明
 * 此代码属于buhuiming编写，在未经允许的情况下不得传播复制
 */
@file:Suppress("SENSELESS_COMPARISON")

package com.bhm.ble.control

import com.bhm.ble.BleManager
import com.bhm.ble.attribute.BleOptions
import com.bhm.ble.data.BleDevice


/**
 * 连接设备BleDeviceController管理池
 *
 * @author Buhuiming
 * @date 2023年05月26日 08时54分
 */
internal class BleDeviceControllerManager private constructor() {

    private val bleLruHashMap: BleLruHashMap<String, BleDeviceController> =
        BleLruHashMap(BleManager.get().getOptions()?.maxConnectNum
            ?: BleOptions.DEFAULT_MAX_CONNECT_NUM)

    companion object {

        private var instance: BleDeviceControllerManager = BleDeviceControllerManager()

        @Synchronized
        fun get(): BleDeviceControllerManager {
            if (instance == null) {
                instance = BleDeviceControllerManager()
            }
            return instance
        }
    }

    /**
     * 添加设备控制器
     */
    fun buildBleDeviceController(bleDevice: BleDevice): BleDeviceController{
        val bleDeviceController = BleDeviceController(bleDevice)
        if (!bleLruHashMap.containsKey(bleDeviceController.getKey())) {
            bleLruHashMap[bleDeviceController.getKey()] = bleDeviceController
        }
        return bleDeviceController
    }

    /**
     * 移除设备控制器
     */
    @Synchronized
    fun removeBleDeviceController(key: String) {
        if (bleLruHashMap.containsKey(key)) {
            bleLruHashMap.remove(key)
        }
    }

}