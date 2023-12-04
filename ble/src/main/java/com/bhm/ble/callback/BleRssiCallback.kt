/*
 * Copyright (c) 2022-2032 buhuiming
 * 不能修改和删除上面的版权声明
 * 此代码属于buhuiming编写，在未经允许的情况下不得传播复制
 */
package com.bhm.ble.callback

import com.bhm.ble.device.BleDevice


/**
 * Rssi信号值回调
 *
 * @author Buhuiming
 * @date 2023年05月26日 15时54分
 */
open class BleRssiCallback : BleBaseCallback() {

    private var success: ((bleDevice: BleDevice, rssi: Int) -> Unit)? = null

    private var fail: ((bleDevice: BleDevice, throwable: Throwable) -> Unit)? = null

    fun onRssiFail(value: ((bleDevice: BleDevice, throwable: Throwable) -> Unit)) {
        fail = value
    }

    fun onRssiSuccess(value: ((bleDevice: BleDevice, rssi: Int) -> Unit)) {
        success = value
    }

    open fun callRssiFail(bleDevice: BleDevice, throwable: Throwable) {
        launchInMainThread {
            fail?.invoke(bleDevice, throwable)
        }
    }

    open fun callRssiSuccess(bleDevice: BleDevice, rssi: Int) {
        launchInMainThread {
            success?.invoke(bleDevice, rssi)
        }
    }
}