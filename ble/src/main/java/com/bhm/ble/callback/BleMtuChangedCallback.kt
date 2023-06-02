/*
 * Copyright (c) 2022-2032 buhuiming
 * 不能修改和删除上面的版权声明
 * 此代码属于buhuiming编写，在未经允许的情况下不得传播复制
 */
package com.bhm.ble.callback


/**
 * mtu值变化回调
 *
 * @author Buhuiming
 * @date 2023年05月26日 16时17分
 */
open class BleMtuChangedCallback : BleBaseCallback(){

    private var mtuChanged: ((mtu: Int) -> Unit)? = null

    private var fail: ((throwable: Throwable) -> Unit)? = null

    fun onSetMtuFail(value: ((throwable: Throwable) -> Unit)) {
        fail = value
    }

    fun onMtuChanged(value: ((mtu: Int) -> Unit)) {
        mtuChanged = value
    }

    open fun callSetMtuFail(throwable: Throwable) {
        launchInMainThread {
            fail?.invoke(throwable)
        }
    }

    open fun callMtuChanged(mtu: Int) {
        launchInMainThread {
            mtuChanged?.invoke(mtu)
        }
    }
}