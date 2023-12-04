/*
 * Copyright (c) 2022-2032 buhuiming
 * 不能修改和删除上面的版权声明
 * 此代码属于buhuiming编写，在未经允许的情况下不得传播复制
 */
package com.bhm.ble.callback

import com.bhm.ble.device.BleDevice


/**
 * 读回调
 *
 * @author Buhuiming
 * @date 2023年05月26日 15时54分
 */
open class BleReadCallback : BleBaseCallback() {

    private var readSuccess: ((bleDevice: BleDevice, data: ByteArray) -> Unit)? = null

    private var readFail: ((bleDevice: BleDevice, throwable: Throwable) -> Unit)? = null

    fun onReadFail(value: ((bleDevice: BleDevice, throwable: Throwable) -> Unit)) {
        readFail = value
    }

    fun onReadSuccess(value: ((bleDevice: BleDevice, data: ByteArray) -> Unit)) {
        readSuccess = value
    }

    open fun callReadFail(bleDevice: BleDevice, throwable: Throwable) {
        launchInMainThread {
            readFail?.invoke(bleDevice, throwable)
        }
    }

    open fun callReadSuccess(bleDevice: BleDevice, data: ByteArray) {
        launchInMainThread {
            readSuccess?.invoke(bleDevice, data)
        }
    }
}