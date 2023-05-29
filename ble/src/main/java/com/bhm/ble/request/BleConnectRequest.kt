/*
 * Copyright (c) 2022-2032 buhuiming
 * 不能修改和删除上面的版权声明
 * 此代码属于buhuiming编写，在未经允许的情况下不得传播复制
 */
package com.bhm.ble.request

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import com.bhm.ble.attribute.BleOptions
import com.bhm.ble.callback.*
import com.bhm.ble.control.CompleteThrowable
import com.bhm.ble.data.BleConnectFailType
import com.bhm.ble.data.BleConnectLastState
import com.bhm.ble.data.BleDevice
import com.bhm.ble.data.BleScanFailType
import com.bhm.ble.utils.BleLogger
import com.bhm.ble.utils.BleUtil
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean


/**
 * ble连接、断开连接请求
 *
 * @author Buhuiming
 * @date 2023年05月24日 14时10分
 */
@SuppressLint("MissingPermission")
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

    private var currentConnectRetryCount = 0

    private var connectJob: Job? = null

    private var waitConnectJob: Job? = null

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
        if (lastState == BleConnectLastState.Connecting) {
            BleLogger.e("连接中")
            bleConnectCallback.callConnectFail(bleDevice, BleConnectFailType.AlreadyConnecting)
            return
        }
        if (lastState ==  BleConnectLastState.Connected) {
            BleLogger.e("已连接")
            bleConnectCallback.callConnectSuccess(bleDevice, bluetoothGatt)
            return
        }
        this.bleConnectCallback = bleConnectCallback
        startConnect()
    }

    private fun startConnect() {
        isActiveDisconnect.set(false)
        lastState = BleConnectLastState.Connecting
        var connectTime = getBleOptions()?.connectMillisTimeOut?: BleOptions.DEFAULT_CONNECT_MILLIS_TIMEOUT
        if (connectTime <= 0) {
            connectTime = BleOptions.DEFAULT_CONNECT_MILLIS_TIMEOUT
        }
        connectJob = bleConnectCallback?.launchInMainThread {
            withTimeout(connectTime) {
                bluetoothGatt = bleDevice.deviceInfo?.connectGatt(getBleManager().getContext(),
                    getBleOptions()?.autoConnect?: false, coreGattCallback, BluetoothDevice.TRANSPORT_LE)
                BleLogger.d("${bleDevice.deviceAddress} -> 开始第${currentConnectRetryCount + 1}次连接")
                bleConnectCallback?.callConnectStart()
                if (bluetoothGatt == null) {
                    lastState = BleConnectLastState.ConnectFailure
                    cancel(CancellationException("连接异常：bluetoothGatt == null"))
                } else {
                    delay(connectTime)
                }
            }
        }
        connectJob?.invokeOnCompletion {
            onCompletion(it)
        }
    }

    private fun onCompletion(throwable: Throwable?) {
        if (ifContinueConnect()) {
            val retryInterval = getBleOptions()?.connectRetryInterval?: BleOptions.DEFAULT_CONNECT_RETRY_INTERVAL
            waitConnectJob = bleConnectCallback?.launchInMainThread {
                delay(retryInterval)
                currentConnectRetryCount ++
                startConnect()
            }
            waitConnectJob?.invokeOnCompletion {
                if (CANCEL_WAIT_JOB_MESSAGE == it?.message) {
                    onCompletion(it)
                }
            }
        } else {
            throwable?.let {
                BleLogger.e(it.message)
                when (it) {
                    is TimeoutCancellationException -> {
                        connectFail()
                        bleConnectCallback?.callConnectFail(
                            bleDevice,
                            BleConnectFailType.ConnectTimeOut
                        )
                    }
                    is CompleteThrowable -> {
                        //连接完成
                        bleConnectCallback?.callConnectSuccess(bleDevice, bluetoothGatt)
                    }
                    else -> {
                        connectFail()
                        bleConnectCallback?.callConnectFail(
                            bleDevice,
                            BleConnectFailType.ConnectException
                        )
                    }
                }
            }
        }
    }

    /**
     * 主动断开连接
     */
    @Synchronized
    fun disConnect() {
        lastState = BleConnectLastState.Disconnect
        isActiveDisconnect.set(true)
        disconnectGatt()
        connectJob?.cancel()
        waitConnectJob?.cancel(CancellationException(CANCEL_WAIT_JOB_MESSAGE))
    }

    private val coreGattCallback = object : BluetoothGattCallback() {
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            super.onCharacteristicChanged(gatt, characteristic, value)

        }
    }

    /**
     * 是否继续连接
     */
    private fun ifContinueConnect(): Boolean {
        if (!isActiveDisconnect.get()) {
            var retryCount = getBleOptions()?.connectRetryCount?: 0
            if (retryCount < 0) {
                retryCount = 0
            }
            if (retryCount > 0 && currentConnectRetryCount < retryCount) {
                return true
            }
        }
        return false
    }

    /**
     * 连接失败 更改状态和释放资源
     */
    private fun connectFail() {
        disconnectGatt()
        refreshDeviceCache()
        closeBluetoothGatt()
        lastState = BleConnectLastState.ConnectFailure
        BleConnectRequestManager.get().removeBleConnectRequest(bleDevice.getKey())
    }

    /**
     * 连接失败 更改状态和释放资源
     */
    @Synchronized
    private fun disconnectGatt() {
        bluetoothGatt?.disconnect()
    }

    /**
     * 刷新缓存
     */
    @Synchronized
    private fun refreshDeviceCache() {
        try {
            val refresh = BluetoothGatt::class.java.getMethod("refresh")
            if (bluetoothGatt != null) {
                val success = refresh.invoke(bluetoothGatt) as Boolean
                BleLogger.i("refreshDeviceCache, is success:  $success")
            }
        } catch (e: Exception) {
            BleLogger.e("exception occur while refreshing device: " + e.message)
            e.printStackTrace()
        }
    }

    /**
     * 关闭Gatt
     */
    @Synchronized
    private fun closeBluetoothGatt() {
        bluetoothGatt?.close()
    }
}