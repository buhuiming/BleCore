/*
 * Copyright (c) 2022-2032 buhuiming
 * 不能修改和删除上面的版权声明
 * 此代码属于buhuiming编写，在未经允许的情况下不得传播复制
 */
package com.bhm.ble.callback

import com.bhm.ble.device.BleDevice


/**
 * 写回调
 *
 * @author Buhuiming
 * @date 2023年05月26日 15时54分
 */
open class BleWriteCallback : BleBaseCallback() {

    private var writeSuccess: ((bleDevice: BleDevice, current: Int, total: Int, justWrite: ByteArray) -> Unit)? = null

    private var writeFail: ((bleDevice: BleDevice, current: Int, total: Int, throwable: Throwable) -> Unit)? = null

    private var writeComplete: ((bleDevice: BleDevice, allSuccess: Boolean) -> Unit)? = null

    fun onWriteFail(value: ((bleDevice: BleDevice, current: Int, total: Int, throwable: Throwable) -> Unit)) {
        writeFail = value
    }

    fun onWriteSuccess(value: ((bleDevice: BleDevice, current: Int, total: Int, justWrite: ByteArray) -> Unit)) {
        writeSuccess = value
    }

    fun onWriteComplete(value: ((bleDevice: BleDevice, allSuccess: Boolean) -> Unit)) {
        writeComplete = value
    }

    open fun callWriteFail(bleDevice: BleDevice, current: Int, total: Int, throwable: Throwable) {
        launchInMainThread {
            writeFail?.invoke(bleDevice, current, total, throwable)
        }
    }

    open fun callWriteSuccess(bleDevice: BleDevice, current: Int, total: Int, justWrite: ByteArray) {
        launchInMainThread {
            writeSuccess?.invoke(bleDevice, current, total, justWrite)
        }
    }

    open fun callWriteComplete(bleDevice: BleDevice, allSuccess: Boolean) {
        launchInMainThread {
            writeComplete?.invoke(bleDevice, allSuccess)
        }
    }
}