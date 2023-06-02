/*
 * Copyright (c) 2022-2032 buhuiming
 * 不能修改和删除上面的版权声明
 * 此代码属于buhuiming编写，在未经允许的情况下不得传播复制
 */
package com.bhm.ble.callback


/**
 * 订阅通知回调
 * Notify方式
 *
 * @author Buhuiming
 * @date 2023年05月29日 08时47分
 */
open class BleNotifyCallback : BleBaseCallback(){

    private var notifySuccess: (() -> Unit)? = null

    private var notifyFail: ((throwable: Throwable) -> Unit)? = null

    private var characteristicChanged: ((data: ByteArray) -> Unit)? = null

    fun onNotifyFail(value: ((throwable: Throwable) -> Unit)) {
        notifyFail = value
    }

    fun onNotifySuccess(value: (() -> Unit)) {
        notifySuccess = value
    }

    fun onCharacteristicChanged(value: ((data: ByteArray) -> Unit)) {
        characteristicChanged = value
    }

    open fun callNotifyFail(throwable: Throwable) {
        launchInMainThread {
            notifyFail?.invoke(throwable)
        }
    }

    open fun callNotifySuccess() {
        launchInMainThread {
            notifySuccess?.invoke()
        }
    }

    open fun callCharacteristicChanged(data: ByteArray) {
        //数据处理放在非主线程
        characteristicChanged?.invoke(data)
    }
}