/*
 * Copyright (c) 2022-2032 buhuiming
 * 不能修改和删除上面的版权声明
 * 此代码属于buhuiming编写，在未经允许的情况下不得传播复制
 */
package com.bhm.ble.callback


/**
 * 订阅通知回调
 * Indicate方式
 *
 * @author Buhuiming
 * @date 2023年05月29日 08时47分
 */
open class BleIndicateCallback : BleBaseCallback() {

    private var indicateSuccess: (() -> Unit)? = null

    private var indicateFail: ((throwable: Throwable) -> Unit)? = null

    private var characteristicChanged: ((data: ByteArray) -> Unit)? = null

    fun onIndicateFail(value: ((throwable: Throwable) -> Unit)) {
        indicateFail = value
    }

    fun onIndicateSuccess(value: (() -> Unit)) {
        indicateSuccess = value
    }

    fun onCharacteristicChanged(value: ((data: ByteArray) -> Unit)) {
        characteristicChanged = value
    }

    open fun callIndicateFail(throwable: Throwable) {
        launchInMainThread {
            indicateFail?.invoke(throwable)
        }
    }

    open fun callIndicateSuccess() {
        launchInMainThread {
            indicateSuccess?.invoke()
        }
    }

    open fun callCharacteristicChanged(data: ByteArray) {
        //数据处理如果需要在非主线程，则需要自行切换
        launchInMainThread {
            characteristicChanged?.invoke(data)
        }
    }
}