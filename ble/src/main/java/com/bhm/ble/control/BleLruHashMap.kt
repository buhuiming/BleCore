/*
 * Copyright (c) 2022-2032 buhuiming
 * 不能修改和删除上面的版权声明
 * 此代码属于buhuiming编写，在未经允许的情况下不得传播复制
 */
package com.bhm.ble.control

import com.bhm.ble.request.BleConnectRequest
import com.bhm.ble.utils.BleLogger
import kotlin.math.ceil


/**
 * 存放BleConnectRequest的容器，控制连接设备数量
 *
 * @author Buhuiming
 * @date 2023年05月26日 08时59分
 */
internal class BleLruHashMap<K, V>(saveSize: Int) : LinkedHashMap<K, V>(
    ceil(saveSize / 0.75).toInt() + 1, 0.75f, true) {

    private var maxSize = saveSize

    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>?): Boolean {
        eldest?.let {
            if (size > maxSize && it.value is BleConnectRequest) {
                BleLogger.e("超出最大连接设备数：${maxSize}，断开第一个设备的连接")
                (it.value as BleConnectRequest).disConnect()
            }
        }
        return size > maxSize
    }

    override fun toString(): String {
        val sb = StringBuilder()
        for ((key, value) in entries) {
            sb.append(String.format("%s:%s ", key, value))
        }
        return sb.toString()
    }

}