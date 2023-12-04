/*
 * Copyright (c) 2022-2032 buhuiming
 * 不能修改和删除上面的版权声明
 * 此代码属于buhuiming编写，在未经允许的情况下不得传播复制
 */
package com.bhm.ble.callback

import com.bhm.ble.device.BleDevice


/**
 * mtu值变化回调
 *
 * @author Buhuiming
 * @date 2023年05月26日 16时17分
 */
open class BleMtuChangedCallback : BleBaseCallback() {

    private var mtuChanged: ((bleDevice: BleDevice, mtu: Int) -> Unit)? = null

    private var fail: ((bleDevice: BleDevice, throwable: Throwable) -> Unit)? = null

    fun onSetMtuFail(value: ((bleDevice: BleDevice, throwable: Throwable) -> Unit)) {
        fail = value
    }

    fun onMtuChanged(value: ((bleDevice: BleDevice, mtu: Int) -> Unit)) {
        mtuChanged = value
    }

    open fun callSetMtuFail(bleDevice: BleDevice, throwable: Throwable) {
        launchInMainThread {
            fail?.invoke(bleDevice, throwable)
        }
    }

    open fun callMtuChanged(bleDevice: BleDevice, mtu: Int) {
        launchInMainThread {
            mtuChanged?.invoke(bleDevice, mtu)
        }
    }
}