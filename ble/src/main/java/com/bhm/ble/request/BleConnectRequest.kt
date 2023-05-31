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
import android.bluetooth.BluetoothProfile
import com.bhm.ble.attribute.BleOptions
import com.bhm.ble.callback.*
import com.bhm.ble.control.ActiveDisConnectedThrowable
import com.bhm.ble.control.CompleteThrowable
import com.bhm.ble.data.BleConnectFailType
import com.bhm.ble.data.BleConnectLastState
import com.bhm.ble.data.BleDevice
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

    private var bleRssiCallback: BleRssiCallback? = null

    private var bleMtuChangedCallback: BleMtuChangedCallback? = null

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

    private val autoConnect = getBleOptions()?.autoConnect?: BleOptions.AUTO_CONNECT

    /**
     * 连接设备
     */
    @Synchronized
    fun connect(bleConnectCallback: BleConnectCallback) {
        this.bleConnectCallback = bleConnectCallback
        if (bleDevice.deviceInfo == null) {
            BleLogger.e("连接失败：BluetoothDevice为空")
            BleConnectRequestManager.get().removeBleConnectRequest(bleDevice.getKey())
            bleConnectCallback.callConnectFail(bleDevice, BleConnectFailType.NullableBluetoothDevice)
            return
        }
        val bleManager = getBleManager()
        if (!BleUtil.isPermission(bleManager.getContext()?.applicationContext)) {
            BleLogger.e("权限不足，请检查")
            BleConnectRequestManager.get().removeBleConnectRequest(bleDevice.getKey())
            bleConnectCallback.callConnectFail(bleDevice, BleConnectFailType.NoBlePermissionType)
            return
        }
        if (!bleManager.isBleSupport()) {
            BleLogger.e("设备不支持蓝牙")
            BleConnectRequestManager.get().removeBleConnectRequest(bleDevice.getKey())
            bleConnectCallback.callConnectFail(bleDevice, BleConnectFailType.UnTypeSupportBle)
            return
        }
        if (!bleManager.isBleEnable()) {
            BleLogger.e("蓝牙未打开")
            BleConnectRequestManager.get().removeBleConnectRequest(bleDevice.getKey())
            bleConnectCallback.callConnectFail(bleDevice, BleConnectFailType.BleDisable)
            return
        }
        if (lastState == BleConnectLastState.Connecting) {
            BleLogger.e("连接中")
            BleConnectRequestManager.get().removeBleConnectRequest(bleDevice.getKey())
            bleConnectCallback.callConnectFail(bleDevice, BleConnectFailType.AlreadyConnecting)
            return
        }
        if (bleManager.isConnected(bleDevice)) {
            lastState =  BleConnectLastState.Connected
            BleLogger.e("已连接")
            bleConnectCallback.callConnectSuccess(bleDevice, bluetoothGatt)
            return
        }
        bleConnectCallback.callConnectStart()
        startConnectJob()
    }

    /**
     * 开始连接
     */
    private fun startConnectJob() {
        //初始化，待coreGattCallback回调再设置为连接中
        lastState = BleConnectLastState.ConnectIdle
        isActiveDisconnect.set(false)
        var connectTime = getBleOptions()?.connectMillisTimeOut?: BleOptions.DEFAULT_CONNECT_MILLIS_TIMEOUT
        if (connectTime <= 0) {
            connectTime = BleOptions.DEFAULT_CONNECT_MILLIS_TIMEOUT
        }
        connectJob = bleConnectCallback?.launchInMainThread {
            withTimeout(connectTime) {
                //每次连接之前确保和上一次操作间隔一定时间
                delay(100)
                bluetoothGatt = bleDevice.deviceInfo?.connectGatt(getBleManager().getContext(),
                    autoConnect, coreGattCallback, BluetoothDevice.TRANSPORT_LE)
                BleLogger.d("${bleDevice.deviceAddress} -> 开始第${currentConnectRetryCount + 1}次连接")
                if (bluetoothGatt == null) {
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

    /**
     * 处理连接结果，是否重连、或者显示结果
     */
    private fun onCompletion(throwable: Throwable?) {
        if (isContinueConnect()) {
            val retryInterval = getBleOptions()?.connectRetryInterval?: BleOptions.DEFAULT_CONNECT_RETRY_INTERVAL
            waitConnectJob = bleConnectCallback?.launchInMainThread {
                delay(retryInterval)
                currentConnectRetryCount ++
                startConnectJob()
            }
            waitConnectJob?.invokeOnCompletion {
                if (it is ActiveDisConnectedThrowable || it is CompleteThrowable) {
                    onCompletion(it)
                }
            }
        } else {
            throwable?.let {
                BleLogger.e(it.message)
                when (it) {
                    //连接超时
                    is TimeoutCancellationException -> {
                        lastState = BleConnectLastState.ConnectFailure
                        connectFail()
                        bleConnectCallback?.callConnectFail(
                            bleDevice,
                            BleConnectFailType.ConnectTimeOut
                        )
                    }
                    //连接成功
                    is CompleteThrowable -> {
                        //连接完成
                        bleConnectCallback?.callConnectSuccess(bleDevice, bluetoothGatt)
                    }
                    //主动断开
                    is ActiveDisConnectedThrowable -> {
                        disConnectGatt()
                    }
                    //连接失败
                    else -> {
                        lastState = BleConnectLastState.ConnectFailure
                        connectFail()
                        bleConnectCallback?.callConnectFail(
                            bleDevice,
                            BleConnectFailType.ConnectException(it)
                        )
                        connectJob?.cancel(null)
                        waitConnectJob?.cancel(null)
                    }
                }
            }
        }
    }

    /**
     * 主动断开连接，上层API调用
     */
    @Synchronized
    fun disConnect() {
        if (bleDevice.deviceInfo == null) {
            BleLogger.e("断开失败：BluetoothDevice为空")
            bleConnectCallback?.callConnectFail(bleDevice, BleConnectFailType.NullableBluetoothDevice)
            return
        }
        val bleManager = getBleManager()
        if (!BleUtil.isPermission(bleManager.getContext()?.applicationContext)) {
            BleLogger.e("权限不足，请检查")
            bleConnectCallback?.callConnectFail(bleDevice, BleConnectFailType.NoBlePermissionType)
            return
        }
        if (!bleManager.isBleSupport()) {
            BleLogger.e("设备不支持蓝牙")
            bleConnectCallback?.callConnectFail(bleDevice, BleConnectFailType.UnTypeSupportBle)
            return
        }
        if (!bleManager.isBleEnable()) {
            BleLogger.e("蓝牙未打开")
            bleConnectCallback?.callConnectFail(bleDevice, BleConnectFailType.BleDisable)
            return
        }
        isActiveDisconnect.set(true)
        if (lastState == BleConnectLastState.ConnectIdle ||
            lastState == BleConnectLastState.Connecting) {
            val throwable = ActiveDisConnectedThrowable()
            connectJob?.cancel(throwable)
            waitConnectJob?.cancel(throwable)
            //连接过程中断开
            //进入判断是否重连
            onCompletion(Throwable("连接过程中断开"))
        } else {
            lastState = BleConnectLastState.Disconnect
            disConnectGatt()
            refreshDeviceCache()
            closeBluetoothGatt()
            BleConnectRequestManager.get()
                .removeBleConnectRequest(bleDevice.getKey())
            removeRssiCallback()
            removeMtuChangedCallback()
            clearCharacterCallback()
            bleConnectCallback?.callDisConnected(
                isActiveDisconnect.get(),
                bleDevice, bluetoothGatt, BluetoothGatt.GATT_SUCCESS
            )
        }
    }

    private val coreGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            BleLogger.i(
                """
                    BluetoothGattCallback：onConnectionStateChange 
                    status: $status
                    newState: $newState
                    currentThread: ${Thread.currentThread().name}
                    lastState: $lastState
                    """.trimIndent()
            )
            bluetoothGatt = gatt

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                //连接成功
                currentConnectRetryCount = 0
                lastState = BleConnectLastState.Connected
                isActiveDisconnect.set(false)
                val throwable = CompleteThrowable()
                connectJob?.cancel(throwable)
                waitConnectJob?.cancel(throwable)
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                when (lastState) {
                    BleConnectLastState.ConnectIdle -> {
                        //触发connectGatt之后，将开始连接的初始状态改成连接中
                        lastState = BleConnectLastState.Connecting
                    }
                    BleConnectLastState.Connecting -> {
                        //连接过程中断开，进入判断是否重连
                        checkIfContinueConnect(Throwable("连接过程中断开"))
                    }
                    //所有断开连接的情况
                    BleConnectLastState.Connected -> {
                        if (!isActiveDisconnect.get()) {
                            lastState = BleConnectLastState.Disconnect
                            refreshDeviceCache()
                            closeBluetoothGatt()
                            BleConnectRequestManager.get()
                                .removeBleConnectRequest(bleDevice.getKey())
                            removeRssiCallback()
                            removeMtuChangedCallback()
                            clearCharacterCallback()
                            bleConnectCallback?.callDisConnected(
                                isActiveDisconnect.get(),
                                bleDevice, gatt, status
                            )
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    /**
     * 判断是否进入重连机制
     */
    private fun checkIfContinueConnect(throwable: Throwable?) {
        refreshDeviceCache()
        closeBluetoothGatt()
        onCompletion(Throwable(throwable))
    }

    /**
     * 是否继续连接
     */
    private fun isContinueConnect(): Boolean {
        if (!isActiveDisconnect.get() && lastState != BleConnectLastState.Connected) {
            var retryCount = getBleOptions()?.connectRetryCount?: 0
            if (retryCount < 0) {
                retryCount = 0
            }
            if (retryCount > 0 && currentConnectRetryCount < retryCount) {
                BleLogger.i("满足重连条件：currentConnectRetryCount = $currentConnectRetryCount")
                return true
            }
        }
        return false
    }

    /**
     * 连接失败 更改状态和释放资源
     */
    private fun connectFail() {
        refreshDeviceCache()
        closeBluetoothGatt()
        BleConnectRequestManager.get().removeBleConnectRequest(bleDevice.getKey())
    }

    /**
     * Gatt断开连接，需要一段时间才会触发onConnectionStateChange
     */
    @Synchronized
    private fun disConnectGatt() {
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

    @Synchronized
    fun removeRssiCallback() {
        bleRssiCallback = null
    }

    @Synchronized
    fun removeMtuChangedCallback() {
        bleMtuChangedCallback = null
    }

    @Synchronized
    fun removeBleConnectCallback() {
        bleConnectCallback = null
    }

    @Synchronized
    fun clearCharacterCallback() {
        bleNotifyCallbackHashMap.clear()
        bleIndicateCallbackHashMap.clear()
        bleWriteCallbackHashMap.clear()
        bleReadCallbackHashMap.clear()
    }

    /**
     * 断开所有连接 释放资源
     */
    @Synchronized
    fun release() {
        lastState = BleConnectLastState.ConnectIdle
        disConnectGatt()
        refreshDeviceCache()
        closeBluetoothGatt()
        removeBleConnectCallback()
        removeRssiCallback()
        removeMtuChangedCallback()
        clearCharacterCallback()
    }
}