/*
 * Copyright (c) 2022-2032 buhuiming
 * 不能修改和删除上面的版权声明
 * 此代码属于buhuiming编写，在未经允许的情况下不得传播复制
 */
package com.bhm.ble.callback

import com.bhm.ble.request.BleRequestManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch


/**
 * 回调的基类
 *
 * @author Buhuiming
 * @date 2023年05月26日 16时04分
 */
open class BleBaseCallback {

    private val mainScope = BleRequestManager.get().getMainScope()

    fun launchInMainThread(block: suspend CoroutineScope.() -> Unit): Job {
        return mainScope.launch {
            block.invoke(this)
        }
    }
}