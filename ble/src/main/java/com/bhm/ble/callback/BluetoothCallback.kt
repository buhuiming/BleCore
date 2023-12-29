/*
 * Copyright (c) 2022-2032 buhuiming
 * 不能修改和删除上面的版权声明
 * 此代码属于buhuiming编写，在未经允许的情况下不得传播复制
 */
package com.bhm.ble.callback


/**
 * 系统蓝牙状态回调
 * @author Buhuiming
 * @date 2023年12月15日 12时07分
 */
open class BluetoothCallback : BleBaseCallback() {

    private var stateOn: (() -> Unit)? = null

    private var stateOff: (() -> Unit)? = null

    private var stateTurningOn: (() -> Unit)? = null

    private var stateTurningOff: (() -> Unit)? = null

    /**
     * 蓝牙打开
     */
    fun onStateOn(value: () -> Unit) {
        stateOn = value
    }

    /**
     * 正在打开蓝牙
     */
    fun onStateTurningOn(value: () -> Unit) {
        stateTurningOn = value
    }

    /**
     * 蓝牙关闭
     */
    fun onStateOff(value: () -> Unit) {
        stateOff = value
    }

    /**
     * 正在关闭蓝牙
     */
    fun onStateTurningOff(value: () -> Unit) {
        stateTurningOff = value
    }


    open fun callStateOn() {
        stateOn?.invoke()
    }

    open fun callStateTurningOn() {
        stateTurningOn?.invoke()
    }

    open fun callStateOff() {
        stateOff?.invoke()
    }

    open fun callStateTurningOff() {
        stateTurningOff?.invoke()
    }
}