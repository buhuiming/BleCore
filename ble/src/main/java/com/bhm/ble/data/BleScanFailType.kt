/*
 * Copyright (c) 2022-2032 buhuiming
 * 不能修改和删除上面的版权声明
 * 此代码属于buhuiming编写，在未经允许的情况下不得传播复制
 */
package com.bhm.ble.data


/**
 * 扫描失败类型
 *
 * @author Buhuiming
 * @date 2023年05月22日 09时59分
 */
sealed class BleScanFailType {

    /**
     * 设备不支持Ble
     */
    object UnTypeSupportBle: BleScanFailType()

    /**
     * 设备未打开蓝牙
     */
    object BleEnableType: BleScanFailType()

    /**
     * 未申请权限
     */
    object NoBlePermissionType: BleScanFailType()

    /**
     * 已开启扫描，不能再次开启
     */
    object AlReadyScanning: BleScanFailType()

    /**
     * 扫描超时
     */
    object ScanOverTimeType: BleScanFailType()

    /**
     * 未知错误
     */
    object UnKnowError: BleScanFailType()
}