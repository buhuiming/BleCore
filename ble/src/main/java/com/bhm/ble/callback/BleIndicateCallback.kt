/*
 * Copyright (c) 2022-2032 buhuiming
 * 不能修改和删除上面的版权声明
 * 此代码属于buhuiming编写，在未经允许的情况下不得传播复制
 */
package com.bhm.ble.callback

import com.bhm.ble.device.BleDevice


/**
 * 订阅通知回调
 * Indicate方式
 *
 * @author Buhuiming
 * @date 2023年05月29日 08时47分
 */
open class BleIndicateCallback : BleBaseCallback() {

    private var indicateSuccess: ((bleDevice: BleDevice, indicateUUID: String) -> Unit)? = null

    private var indicateFail: ((bleDevice: BleDevice, indicateUUID: String, throwable: Throwable) -> Unit)? = null

    private var characteristicChanged: ((bleDevice: BleDevice, indicateUUID: String, data: ByteArray) -> Unit)? = null

    fun onIndicateFail(value: ((bleDevice: BleDevice, indicateUUID: String, throwable: Throwable) -> Unit)) {
        indicateFail = value
    }

    fun onIndicateSuccess(value: ((bleDevice: BleDevice, indicateUUID: String) -> Unit)) {
        indicateSuccess = value
    }

    fun onCharacteristicChanged(value: ((bleDevice: BleDevice, indicateUUID: String, data: ByteArray) -> Unit)) {
        characteristicChanged = value
    }

    open fun callIndicateFail(bleDevice: BleDevice, indicateUUID: String, throwable: Throwable) {
        launchInMainThread {
            indicateFail?.invoke(bleDevice, indicateUUID, throwable)
        }
    }

    open fun callIndicateSuccess(bleDevice: BleDevice, indicateUUID: String) {
        launchInMainThread {
            indicateSuccess?.invoke(bleDevice, indicateUUID)
        }
    }

    open fun callCharacteristicChanged(bleDevice: BleDevice, indicateUUID: String, data: ByteArray) {
        //数据处理的线程需要自行切换
        characteristicChanged?.invoke(bleDevice, indicateUUID, data)
    }
}