/*
 * Copyright (c) 2022-2032 buhuiming
 * 不能修改和删除上面的版权声明
 * 此代码属于buhuiming编写，在未经允许的情况下不得传播复制
 */
package com.bhm.ble.callback

import com.bhm.ble.device.BleDevice


/**
 * 订阅通知回调
 * Notify方式
 *
 * @author Buhuiming
 * @date 2023年05月29日 08时47分
 */
open class BleNotifyCallback : BleBaseCallback() {

    private var notifySuccess: ((bleDevice: BleDevice, notifyUUID: String) -> Unit)? = null

    private var notifyFail: ((bleDevice: BleDevice, notifyUUID: String, throwable: Throwable) -> Unit)? = null

    private var characteristicChanged: ((bleDevice: BleDevice, notifyUUID: String, data: ByteArray) -> Unit)? = null

    fun onNotifyFail(value: ((bleDevice: BleDevice, notifyUUID: String, throwable: Throwable) -> Unit)) {
        notifyFail = value
    }

    fun onNotifySuccess(value: ((bleDevice: BleDevice, notifyUUID: String) -> Unit)) {
        notifySuccess = value
    }

    fun onCharacteristicChanged(value: ((bleDevice: BleDevice, notifyUUID: String, data: ByteArray) -> Unit)) {
        characteristicChanged = value
    }

    open fun callNotifyFail(bleDevice: BleDevice, notifyUUID: String, throwable: Throwable) {
        launchInMainThread {
            notifyFail?.invoke(bleDevice, notifyUUID, throwable)
        }
    }

    open fun callNotifySuccess(bleDevice: BleDevice, notifyUUID: String) {
        launchInMainThread {
            notifySuccess?.invoke(bleDevice, notifyUUID)
        }
    }

    open fun callCharacteristicChanged(bleDevice: BleDevice, notifyUUID: String, data: ByteArray) {
        //数据处理的线程需要自行切换
        characteristicChanged?.invoke(bleDevice, notifyUUID, data)
    }
}