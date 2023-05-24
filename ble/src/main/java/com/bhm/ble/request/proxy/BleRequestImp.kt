/*
 * Copyright (c) 2022-2032 buhuiming
 * 不能修改和删除上面的版权声明
 * 此代码属于buhuiming编写，在未经允许的情况下不得传播复制
 */
@file:Suppress("SENSELESS_COMPARISON")

package com.bhm.ble.request.proxy

import com.bhm.ble.callback.BleBaseRequest
import com.bhm.ble.callback.BleConnectCallback
import com.bhm.ble.callback.BleScanCallback
import com.bhm.ble.data.BleDevice
import com.bhm.ble.request.BleConnectRequest
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
        BleRequestManager.get().getRequest(BleScanRequest::class.java).startScan(bleScanCallback)
    }

    /**
     * 是否扫描中
     */
    override fun isScanning(): Boolean {
        return BleRequestManager.get().getRequest(BleScanRequest::class.java).isScanning()
    }

    /**
     * 停止扫描
     */
    override fun stopScan() {
        BleRequestManager.get().getRequest(BleScanRequest::class.java).stopScan()
    }

    /**
     * 开始连接
     */
    override fun connect(bleDevice: BleDevice, bleConnectCallback: BleConnectCallback) {
        BleRequestManager.get().getRequest(BleConnectRequest::class.java).connect(bleDevice, bleConnectCallback)
    }

    /**
     * 断开连接
     */
    override fun disConnect(bleDevice: BleDevice) {
        BleRequestManager.get().getRequest(BleConnectRequest::class.java).disConnect(bleDevice)
    }

    /**
     * 断开所有连接 释放资源
     */
    override fun release() {

    }
}