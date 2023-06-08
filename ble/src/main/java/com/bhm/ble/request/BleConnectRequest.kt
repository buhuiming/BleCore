/*
 * Copyright (c) 2022-2032 buhuiming
 * 不能修改和删除上面的版权声明
 * 此代码属于buhuiming编写，在未经允许的情况下不得传播复制
 */

package com.bhm.ble.request

import android.annotation.SuppressLint
import android.bluetooth.*
import com.bhm.ble.callback.*
import com.bhm.ble.control.*
import com.bhm.ble.data.*
import com.bhm.ble.data.Constants.AUTO_CONNECT
import com.bhm.ble.data.Constants.DEFAULT_CONNECT_MILLIS_TIMEOUT
import com.bhm.ble.data.Constants.DEFAULT_CONNECT_RETRY_INTERVAL
import com.bhm.ble.data.Constants.DEFAULT_MTU
import com.bhm.ble.device.BleConnectedDeviceManager
import com.bhm.ble.device.BleDevice
import com.bhm.ble.utils.BleLogger
import com.bhm.ble.utils.BleUtil
import kotlinx.coroutines.*
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean


/**
 * ble连接、断开连接请求
 *
 * @author Buhuiming
 * @date 2023年05月24日 14时10分
 */
@SuppressLint("MissingPermission")
internal class BleConnectRequest(
    private val bleDevice: BleDevice,
    private val coreGattCallback: BluetoothGattCallback,
) : Request() {

    private var bleConnectCallback: BleConnectCallback? = null

    private var lastState: BleConnectLastState? = null

    private var isActiveDisconnect = AtomicBoolean(false)

    private var bluetoothGatt: BluetoothGatt? = null

    private var currentConnectRetryCount = 0

    private var connectJob: Job? = null

    private var waitConnectJob: Job? = null

    private val autoConnect = getBleOptions()?.autoConnect?: AUTO_CONNECT

    private val waitTime = 100L

    /**
     * 连接设备
     */
    @Synchronized
    fun connect(bleConnectCallback: BleConnectCallback) {
        addBleConnectCallback(bleConnectCallback)
        if (bleDevice.deviceInfo == null) {
            BleLogger.e("连接失败：BluetoothDevice为空")
            removeBleConnectedDevice()
            bleConnectCallback.callConnectFail(bleDevice, BleConnectFailType.NullableBluetoothDevice)
            return
        }
        val bleManager = getBleManager()
        if (!BleUtil.isPermission(bleManager.getContext()?.applicationContext)) {
            BleLogger.e("权限不足，请检查")
            removeBleConnectedDevice()
            bleConnectCallback.callConnectFail(bleDevice, BleConnectFailType.NoBlePermissionType)
            return
        }
        if (!bleManager.isBleSupport()) {
            BleLogger.e("设备不支持蓝牙")
            removeBleConnectedDevice()
            bleConnectCallback.callConnectFail(bleDevice, BleConnectFailType.UnSupportBle)
            return
        }
        if (!bleManager.isBleEnable()) {
            BleLogger.e("蓝牙未打开")
            removeBleConnectedDevice()
            bleConnectCallback.callConnectFail(bleDevice, BleConnectFailType.BleDisable)
            return
        }
        if (lastState == BleConnectLastState.Connecting || lastState == BleConnectLastState.ConnectIdle) {
            BleLogger.e("连接中")
            removeBleConnectedDevice()
            bleConnectCallback.callConnectFail(bleDevice, BleConnectFailType.AlreadyConnecting)
            return
        }
        if (bleManager.isConnected(bleDevice)) {
            lastState =  BleConnectLastState.Connected
            BleLogger.e("已连接")
            bleConnectCallback.callConnectSuccess(bleDevice, bluetoothGatt)
            autoSetMtu()
            return
        }
        bleConnectCallback.callConnectStart()
        startConnectJob()
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
            bleConnectCallback?.callConnectFail(bleDevice, BleConnectFailType.UnSupportBle)
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
            val throwable = ActiveDisConnectedException("连接过程中断开")
            connectJob?.cancel(throwable)
            waitConnectJob?.cancel(throwable)
        } else {
            lastState = BleConnectLastState.Disconnect
            disConnectGatt()
            refreshDeviceCache()
            closeBluetoothGatt()
            removeAllCallback()
            BleLogger.e("${bleDevice.deviceAddress} -> 主动断开连接")
            bleConnectCallback?.callDisConnected(
                isActiveDisconnect.get(),
                bleDevice, bluetoothGatt, BluetoothGatt.GATT_SUCCESS
            )
        }
    }

    /**
     * 当连接上设备或者失去连接时会触发
     */
    fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
        BleLogger.i("BluetoothGattCallback：onConnectionStateChange status: $status " +
                "newState: $newState currentThread: ${Thread.currentThread().name} " +
                "bleAddress: ${bleDevice.deviceAddress} lastState: $lastState")
        bluetoothGatt = gatt
        if (newState == BluetoothProfile.STATE_CONNECTED) {
            //连接成功
            if (connectJob?.isActive == true || waitConnectJob?.isActive == true) {
                val throwable = CompleteException()
                connectJob?.cancel(throwable)
                waitConnectJob?.cancel(throwable)
            } else {
                findService()
            }
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
                else -> {
                    if (!isActiveDisconnect.get()) {
                        lastState = BleConnectLastState.Disconnect
                        refreshDeviceCache()
                        closeBluetoothGatt()
                        removeAllCallback()
                        BleLogger.e("${bleDevice.deviceAddress} -> 自动断开连接")
                        bleConnectCallback?.callDisConnected(
                            isActiveDisconnect.get(),
                            bleDevice, gatt, status
                        )
                    }
                }
            }
        }
    }

    /**
     * 当设备是否找到服务[bluetoothGatt?.discoverServices()]时会触发
     */
    fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
        bluetoothGatt = gatt
        if (status == BluetoothGatt.GATT_SUCCESS) {
            BleLogger.i("${bleDevice.deviceAddress} -> 连接成功，发现服务")
            currentConnectRetryCount = 0
            lastState = BleConnectLastState.Connected
            isActiveDisconnect.set(false)
            bleConnectCallback?.callConnectSuccess(bleDevice, bluetoothGatt)
            autoSetMtu()
        } else {
            connectFail()
            BleLogger.e("${bleDevice.deviceAddress} -> 连接失败：未发现服务")
            bleConnectCallback?.callConnectFail(
                bleDevice,
                BleConnectFailType.ConnectException(Throwable("发现服务失败"))
            )
        }
    }

    @Synchronized
    fun addBleConnectCallback(bleConnectCallback: BleConnectCallback) {
        this.bleConnectCallback = bleConnectCallback
    }

    @Synchronized
    fun removeBleConnectCallback() {
        bleConnectCallback = null
    }

    /**
     * 获取设备的BluetoothGatt对象
     */
    @Synchronized
    fun getBluetoothGatt(): BluetoothGatt? {
        return bluetoothGatt
    }

    /**
     * 断开所有连接 释放资源
     */
    @Synchronized
    fun release() {
        bleConnectCallback?.callDisConnected(
            isActiveDisconnect.get(),
            bleDevice, bluetoothGatt, BluetoothGatt.GATT_SUCCESS
        )
        lastState = BleConnectLastState.Disconnect
        disConnectGatt()
        refreshDeviceCache()
        closeBluetoothGatt()
        removeBleConnectCallback()
        removeAllCallback()
    }

    /**
     * 开始连接
     */
    private fun startConnectJob() {
        //初始化，待coreGattCallback回调再设置为连接中
        lastState = BleConnectLastState.ConnectIdle
        isActiveDisconnect.set(false)
        var connectTime = getBleOptions()?.connectMillisTimeOut?: DEFAULT_CONNECT_MILLIS_TIMEOUT
        if (connectTime <= 0) {
            connectTime = DEFAULT_CONNECT_MILLIS_TIMEOUT
        }
        connectJob = bleConnectCallback?.launchInMainThread {
            withTimeout(connectTime) {
                //每次连接之前确保和上一次操作间隔一定时间
                delay(waitTime)
                bluetoothGatt = bleDevice.deviceInfo?.connectGatt(getBleManager().getContext(),
                    autoConnect, coreGattCallback, BluetoothDevice.TRANSPORT_LE)
                BleLogger.d("${bleDevice.deviceAddress} -> 开始第${currentConnectRetryCount + 1}次连接")
                if (bluetoothGatt == null) {
                    cancel(CancellationException("连接异常：bluetoothGatt == null"))
                } else {
                    delay(connectTime + waitTime * 6) //需要加上等待发现服务的时间
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
        if (isContinueConnect(throwable)) {
            val retryInterval = getBleOptions()?.connectRetryInterval?: DEFAULT_CONNECT_RETRY_INTERVAL
            waitConnectJob = bleConnectCallback?.launchInMainThread {
                delay(retryInterval)
                currentConnectRetryCount ++
                startConnectJob()
            }
            waitConnectJob?.invokeOnCompletion {
                if (it is ActiveDisConnectedException || it is CompleteException) {
                    onCompletion(it)
                }
            }
        } else {
            throwable?.let {
                BleLogger.e(it.message)
                when (it) {
                    //连接超时
                    is TimeoutCancellationException -> {
                        connectFail()
                        BleLogger.e("${bleDevice.deviceAddress} -> 连接失败：超时")
                        bleConnectCallback?.callConnectFail(
                            bleDevice,
                            BleConnectFailType.ConnectTimeOut
                        )
                    }
                    //连接成功
                    is CompleteException -> {
                        //发现服务
                        findService()
                    }
                    //主动断开
                    is ActiveDisConnectedException -> {
                        disConnectGatt()
                    }
                    //连接失败
                    else -> {
                        connectFail()
                        BleLogger.e("${bleDevice.deviceAddress} -> 连接失败：${it.message}")
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
    private fun isContinueConnect(throwable: Throwable?): Boolean {
        if (throwable is ActiveDisConnectedException || throwable is CompleteException) {
            return false
        }
        if (!isActiveDisconnect.get() && lastState != BleConnectLastState.Connected) {
            var retryCount = getBleOptions()?.connectRetryCount?: 0
            if (retryCount < 0) {
                retryCount = 0
            }
            if (retryCount > 0 && currentConnectRetryCount < retryCount) {
                BleLogger.i("${bleDevice.deviceAddress} -> 满足重连条件：" +
                        "currentConnectRetryCount = $currentConnectRetryCount")
                return true
            }
        }
        return false
    }

    /**
     * 发现服务
     */
    private fun findService() {
        bleConnectCallback?.launchInMainThread {
            delay(waitTime * 5)
            if (bluetoothGatt == null || bluetoothGatt?.discoverServices() == false) {
                connectFail()
                val throwable = Throwable("${bleDevice.deviceAddress} -> 发现服务失败")
                BleLogger.e(throwable.message)
                bleConnectCallback?.callConnectFail(
                    bleDevice,
                    BleConnectFailType.ConnectException(throwable)
                )
            }
        }
    }

    /**
     * 移除设备管理池中的设备
     */
    private fun removeBleConnectedDevice() {
        BleConnectedDeviceManager.get().removeBleConnectedDevice(bleDevice.getKey())
    }

    /**
     * 移除设备管理池中的设备
     */
    private fun removeAllCallback() {
        removeBleConnectedDevice()
        getBleConnectedDevice(bleDevice)?.removeAllCallback()
    }

    /**
     * 自动设置mtu
     */
    private fun autoSetMtu() {
        if (getBleOptions()?.autoSetMtu == true) {
            getBleConnectedDevice(bleDevice)?.setMtu(getBleOptions()?.mtu?: DEFAULT_MTU,
                object : BleMtuChangedCallback() {
                    override fun callMtuChanged(mtu: Int) {
                        super.callMtuChanged(mtu)
                        BleLogger.d("${bleDevice.deviceAddress} -> 自动设置Mtu成功: $mtu")
                    }

                    override fun callSetMtuFail(throwable: Throwable) {
                        super.callSetMtuFail(throwable)
                        BleLogger.e("${bleDevice.deviceAddress} -> " +
                                "自动设置Mtu失败: ${throwable.message}")
                    }
                })
        }
    }

    /**
     * 连接失败 更改状态和释放资源
     */
    private fun connectFail() {
        lastState = BleConnectLastState.ConnectFailure
        refreshDeviceCache()
        closeBluetoothGatt()
        removeBleConnectedDevice()
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
}