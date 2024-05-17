/*
 * Copyright (c) 2022-2032 buhuiming
 * 不能修改和删除上面的版权声明
 * 此代码属于buhuiming编写，在未经允许的情况下不得传播复制
 */
package com.bhm.ble.receiver

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.bhm.ble.callback.BluetoothCallback
import com.bhm.ble.log.BleLogger

/**
 * @description 注册蓝牙广播
 * @author Buhuiming
 * @date 2023/12/29/ 15:07
 */
open class BluetoothReceiver : BroadcastReceiver() {

    private var bluetoothCallback: BluetoothCallback? = null

    override fun onReceive(context: Context?, intent: Intent?) {
        val action = intent?.action
        if (action == BluetoothAdapter.ACTION_STATE_CHANGED) {
            when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
                BluetoothAdapter.STATE_OFF -> {
                    BleLogger.i("系统蓝牙关闭了!")
                    bluetoothCallback?.callStateOff()
                }
                BluetoothAdapter.STATE_TURNING_OFF -> {
                    BleLogger.i("系统蓝牙关闭中...")
                    bluetoothCallback?.callStateTurningOff()
                }
                BluetoothAdapter.STATE_ON -> {
                    BleLogger.i("系统蓝牙打开了")
                    bluetoothCallback?.callStateOn()
                }
                BluetoothAdapter.STATE_TURNING_ON -> {
                    BleLogger.i("系统蓝牙打开中...")
                    bluetoothCallback?.callStateTurningOn()
                }
            }
        }
    }

    fun setBluetoothCallback(bluetoothCallback: BluetoothCallback?) {
        this.bluetoothCallback = bluetoothCallback
    }
}