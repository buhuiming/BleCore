/*
 * Copyright (c) 2022-2032 buhuiming
 * 不能修改和删除上面的版权声明
 * 此代码属于buhuiming编写，在未经允许的情况下不得传播复制
 */
package com.bhm.ble.callback


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
}