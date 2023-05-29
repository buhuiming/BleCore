/*
 * Copyright (c) 2022-2032 buhuiming
 * 不能修改和删除上面的版权声明
 * 此代码属于buhuiming编写，在未经允许的情况下不得传播复制
 */
package com.bhm.ble.data


/**
 * 连接的最后状态
 *
 * @author Buhuiming
 * @date 2023年05月29日 09时02分
 */
internal sealed class BleConnectLastState {

    /**
     * 初始状态
     */
    object ConnectIdle : BleConnectLastState()

    /**
     * 连接中状态
     */
    object Connecting : BleConnectLastState()

    /**
     * 已连接状态
     */
    object Connected : BleConnectLastState()

    /**
     * 连接失败状态
     */
    object ConnectFailure : BleConnectLastState()

    /**
     * 断开状态
     */
    object Disconnect : BleConnectLastState()
}