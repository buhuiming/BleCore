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
    object UnSupportBle: BleScanFailType()

    /**
     * 设备未打开蓝牙
     */
    object BleDisable: BleScanFailType()

    /**
     * 设备未打开GPS定位
     */
    object GPSDisable: BleScanFailType()

    /**
     * 未申请权限
     */
    object NoBlePermission: BleScanFailType()

    /**
     * 已开启扫描，不能再次开启
     */
    object AlReadyScanning: BleScanFailType()

    /**
     * 扫描错误(这里不再详细区分，具体错误码如下)
     * 1、errorCode = [android.bluetooth.le.ScanCallback.SCAN_FAILED_ALREADY_STARTED]
     * 无法启动扫描，因为应用程序已启动具有相同设置的 BLE 扫描。
     * 2、errorCode = [android.bluetooth.le.ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED]
     * 无法开始扫描，因为无法注册应用程序。
     * 3、errorCode = [android.bluetooth.le.ScanCallback.SCAN_FAILED_INTERNAL_ERROR]
     * 由于内部错误无法开始扫描。
     * 4、errorCode = [android.bluetooth.le.ScanCallback.SCAN_FAILED_FEATURE_UNSUPPORTED]
     * 无法启动电源优化扫描，因为不支持此功能。
     * 5、errorCode = [android.bluetooth.le.ScanCallback.SCAN_FAILED_OUT_OF_HARDWARE_RESOURCES]
     * 由于硬件资源不足，无法启动扫描。
     * 6、errorCode = [android.bluetooth.le.ScanCallback.SCAN_FAILED_SCANNING_TOO_FREQUENTLY]
     * 由于应用程序尝试扫描过于频繁，无法开始扫描。
     * 7、errorCode = -1，具体看throwable
     */
    data class ScanError(val errorCode: Int, val throwable: Throwable?): BleScanFailType()
}