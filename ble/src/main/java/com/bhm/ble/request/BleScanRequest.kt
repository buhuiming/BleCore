/*
 * Copyright (c) 2022-2032 buhuiming
 * 不能修改和删除上面的版权声明
 * 此代码属于buhuiming编写，在未经允许的情况下不得传播复制
 */
package com.bhm.ble.request

import com.bhm.ble.callback.BleScanCallback
import com.bhm.ble.utils.BleLogger
import java.util.concurrent.atomic.AtomicBoolean


/**
 * Ble扫描
 *
 * @author Buhuiming
 * @date 2023年05月22日 09时49分
 */

internal class BleScanRequest {

    private val isScanning = AtomicBoolean(false)

    /**
     * 开始扫描
     */
    fun startScan(bleScanCallback: BleScanCallback) {
        BleLogger.d("开始扫描")
        isScanning.set(true)
    }

    fun isScanning() = isScanning.get()
}