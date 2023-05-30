/*
 * Copyright (c) 2022-2032 buhuiming
 * 不能修改和删除上面的版权声明
 * 此代码属于buhuiming编写，在未经允许的情况下不得传播复制
 */
package com.bhm.ble.request

import com.bhm.ble.callback.BleConnectCallback
import com.bhm.ble.callback.BleScanCallback
import com.bhm.ble.data.BleDevice


/**
 * 抽象方法
 *
 * @author Buhuiming
 * @date 2023年05月22日 10时37分
 */
internal interface BleBaseRequest {

    /**
     * 开始扫描
     */
    fun startScan(bleScanCallback: BleScanCallback)

    /**
     * 是否扫描中
     */
    fun isScanning(): Boolean

    /**
     * 停止扫描
     */
    fun stopScan()

    /**
     * 开始连接
     */
    fun connect(bleDevice: BleDevice, bleConnectCallback: BleConnectCallback)

    /**
     * 断开连接
     */
    fun disConnect(bleDevice: BleDevice)

    /**
     * 是否已连接
     */
    fun isConnected(bleDevice: BleDevice): Boolean

    /**
     * 断开所有连接 释放资源
     */
    fun release()
}