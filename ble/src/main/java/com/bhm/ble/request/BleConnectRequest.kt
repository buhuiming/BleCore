/*
 * Copyright (c) 2022-2032 buhuiming
 * 不能修改和删除上面的版权声明
 * 此代码属于buhuiming编写，在未经允许的情况下不得传播复制
 */
package com.bhm.ble.request

import android.bluetooth.BluetoothGatt
import com.bhm.ble.callback.*
import com.bhm.ble.data.BleConnectFailType
import com.bhm.ble.data.BleConnectLastState
import com.bhm.ble.data.BleDevice
import com.bhm.ble.utils.BleLogger
import com.bhm.ble.utils.BleUtil
import java.util.concurrent.atomic.AtomicBoolean


/**
 * ble连接、断开连接请求
 *
 * @author Buhuiming
 * @date 2023年05月24日 14时10分
 */
internal class BleConnectRequest(val bleDevice: BleDevice) : Request(){

    private var bleConnectCallback: BleConnectCallback? = null

    private val bleRssiCallback: BleRssiCallback? = null

    private val bleMtuChangedCallback: BleMtuChangedCallback? = null

    private val bleNotifyCallbackHashMap: HashMap<String, BleNotifyCallback> = HashMap()

    private val bleIndicateCallbackHashMap: HashMap<String, BleIndicateCallback> = HashMap()

    private val bleWriteCallbackHashMap: HashMap<String, BleWriteCallback> = HashMap()

    private val bleReadCallbackHashMap: HashMap<String, BleReadCallback> = HashMap()

    private var lastState: BleConnectLastState? = null

    private var isActiveDisconnect = AtomicBoolean(false)

    private var bluetoothGatt: BluetoothGatt? = null

    private var connectRetryCount = 0

    /**
     * 连接设备
     */
    @Synchronized
    fun connect(bleConnectCallback: BleConnectCallback) {
        if (bleDevice.deviceInfo == null) {
            BleLogger.e("连接失败：BluetoothDevice为空")
            bleConnectCallback.callConnectFail(bleDevice, BleConnectFailType.NullableBluetoothDevice)
            return
        }
        val bleManager = getBleManager()
        if (!BleUtil.isPermission(bleManager.getContext()?.applicationContext)) {
            BleLogger.e("权限不足，请检查")
            bleConnectCallback.callConnectFail(bleDevice, BleConnectFailType.NoBlePermissionType)
            return
        }
        if (!bleManager.isBleSupport()) {
            BleLogger.e("设备不支持蓝牙")
            bleConnectCallback.callConnectFail(bleDevice, BleConnectFailType.UnTypeSupportBle)
            return
        }
        if (!bleManager.isBleEnable()) {
            BleLogger.e("蓝牙未打开")
            bleConnectCallback.callConnectFail(bleDevice, BleConnectFailType.BleDisable)
            return
        }
        this.bleConnectCallback = bleConnectCallback
        lastState = BleConnectLastState.Connecting

    }

    /**
     * 主动断开连接
     */
    fun disConnect() {
        BleLogger.e("断开连接：${bleDevice.deviceAddress}")
    }

    fun getKey() = bleDevice.getKey()
}