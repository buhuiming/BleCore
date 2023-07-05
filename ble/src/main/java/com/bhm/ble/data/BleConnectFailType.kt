/*
 * Copyright (c) 2022-2032 buhuiming
 * 不能修改和删除上面的版权声明
 * 此代码属于buhuiming编写，在未经允许的情况下不得传播复制
 */
package com.bhm.ble.data


/**
 * 连接失败类型
 *
 * @author Buhuiming
 * @date 2023年05月26日 11时10分
 */
sealed class BleConnectFailType {

    /**
     * BluetoothDevice为空
     */
    object NullableBluetoothDevice: BleConnectFailType()

    /**
     * 设备不支持Ble
     */
    object UnSupportBle: BleConnectFailType()

    /**
     * 设备未打开蓝牙
     */
    object BleDisable: BleConnectFailType()

    /**
     * 未申请权限
     */
    object NoBlePermission: BleConnectFailType()

    /**
     * 连接异常
     */
    class ConnectException(val throwable: Throwable): BleConnectFailType()

    /**
     * 连接超时
     */
    object ConnectTimeOut: BleConnectFailType()

    /**
     * 连接中
     */
    object AlreadyConnecting: BleConnectFailType()

    /**
     * 扫描并连接时，扫描到的BluetoothDevice为空
     */
    object ScanNullableBluetoothDevice: BleConnectFailType()
}