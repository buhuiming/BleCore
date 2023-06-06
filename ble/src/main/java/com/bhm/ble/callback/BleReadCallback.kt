/*
 * Copyright (c) 2022-2032 buhuiming
 * 不能修改和删除上面的版权声明
 * 此代码属于buhuiming编写，在未经允许的情况下不得传播复制
 */
package com.bhm.ble.callback


/**
 * 读回调
 *
 * @author Buhuiming
 * @date 2023年05月26日 15时54分
 */
open class BleReadCallback : BleBaseCallback(){

    private var readSuccess: ((data: ByteArray) -> Unit)? = null

    private var readFail: ((throwable: Throwable) -> Unit)? = null

    fun onReadFail(value: ((throwable: Throwable) -> Unit)) {
        readFail = value
    }

    fun onReadSuccess(value: ((data: ByteArray) -> Unit)) {
        readSuccess = value
    }

    open fun callReadFail(throwable: Throwable) {
        launchInMainThread {
            readFail?.invoke(throwable)
        }
    }

    open fun callReadSuccess(data: ByteArray) {
        launchInMainThread {
            readSuccess?.invoke(data)
        }
    }
}