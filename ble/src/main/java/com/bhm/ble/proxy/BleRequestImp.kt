/*
 * Copyright (c) 2022-2032 buhuiming
 * 不能修改和删除上面的版权声明
 * 此代码属于buhuiming编写，在未经允许的情况下不得传播复制
 */
@file:Suppress("SENSELESS_COMPARISON")

package com.bhm.ble.proxy

import com.bhm.ble.callback.BleBaseRequest
import com.bhm.ble.callback.BleScanCallback
import com.bhm.ble.request.BleRequestManager
import com.bhm.ble.request.BleScanRequest
import com.bhm.ble.utils.BleLogger


/**
 * 操作实现
 *
 * @author Buhuiming
 * @date 2023年05月22日 10时41分
 */
internal class BleRequestImp private constructor() : BleBaseRequest{

    companion object {

        private var instance: BleRequestImp = BleRequestImp()

        @Synchronized
        fun get(): BleRequestImp {
            if (instance == null) {
                instance = BleRequestImp()
            }
            return instance
        }
    }

    /**
     * 开始扫描
     */
    override fun startScan(bleScanCallback: BleScanCallback) {
        val request: BleScanRequest = BleRequestManager.get().getRequest(BleScanRequest::class.java)
        request.startScan(bleScanCallback)
    }

}