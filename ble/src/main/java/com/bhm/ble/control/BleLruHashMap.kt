/*
 * Copyright (c) 2022-2032 buhuiming
 * 不能修改和删除上面的版权声明
 * 此代码属于buhuiming编写，在未经允许的情况下不得传播复制
 */
package com.bhm.ble.control

import com.bhm.ble.device.BleConnectedDevice
import com.bhm.ble.log.BleLogger
import java.util.*
import java.util.concurrent.ConcurrentHashMap


/**
 * 存放BleConnectRequest的容器，控制连接设备数量
 *
 * @author Buhuiming
 * @date 2023年05月26日 08时59分
 */
internal class BleLruHashMap(
    private val maxSize: Int
) : ConcurrentHashMap<String, BleConnectedDevice?>(maxSize) {

    private val keyLists = Collections.synchronizedList(LinkedList<String>())

    override fun put(key: String, value: BleConnectedDevice): BleConnectedDevice? {
        if (size == maxSize) {
            BleLogger.w("超出最大连接设备数：${maxSize}，断开第一个设备的连接")
            get(keyLists.firstOrNull())?.disConnect()
            remove(keyLists.firstOrNull())
            keyLists.removeFirstOrNull()
        }
        keyLists.add(key)
        return super.put(key, value)
    }

    override fun remove(key: String): BleConnectedDevice? {
        keyLists.remove(key)
        return super.remove(key)
    }

    override fun clear() {
        super.clear()
        keyLists.clear()
    }

    override fun toString(): String {
        val sb = StringBuilder()
        for ((key, value) in entries) {
            sb.append(String.format("%s:%s ", key, value))
        }
        return sb.toString()
    }

}