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
import com.bhm.ble.callback.BleConnectCallback
import com.bhm.ble.callback.BleMtuChangedCallback
import com.bhm.ble.data.ActiveDisConnectedException
import com.bhm.ble.data.ActiveStopConnectedException
import com.bhm.ble.data.BleConnectFailType
import com.bhm.ble.data.BleConnectLastState
import com.bhm.ble.data.CompleteException
import com.bhm.ble.data.Constants.AUTO_CONNECT
import com.bhm.ble.data.Constants.DEFAULT_CONNECT_MILLIS_TIMEOUT
import com.bhm.ble.data.Constants.DEFAULT_CONNECT_RETRY_INTERVAL
import com.bhm.ble.data.Constants.DEFAULT_MTU
import com.bhm.ble.data.Constants.DEFAULT_OPERATE_INTERVAL
import com.bhm.ble.data.UnDefinedException
import com.bhm.ble.device.BleConnectedDeviceManager
import com.bhm.ble.device.BleDevice
import com.bhm.ble.log.BleLogger
import com.bhm.ble.request.base.BleRequestImp
import com.bhm.ble.request.base.Request
import com.bhm.ble.utils.BleUtil
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
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

    private val waitTime = getBleOptions()?.operateInterval?: DEFAULT_OPERATE_INTERVAL

    private var connectMillisTimeOut: Long? = null

    private var connectRetryCount: Int? = null

    private var connectRetryInterval: Long? = null

    /**
     * 连接设备
     */
    fun connect(
        connectMillisTimeOut: Long?,
        connectRetryCount: Int?,
        connectRetryInterval: Long?,
        isForceConnect: Boolean = false,
        bleConnectCallback: BleConnectCallback
    ) {
        addBleConnectCallback(bleConnectCallback)
        if (bleDevice.deviceInfo == null) {
            BleLogger.e("连接失败：BluetoothDevice为空")
            removeBleConnectedDevice()
            bleConnectCallback.callConnectFail(createNewDeviceInfo(), BleConnectFailType.NullableBluetoothDevice)
            getBleConnectedDevice(bleDevice)?.getBleEventCallback()?.callConnectFail(
                createNewDeviceInfo(), BleConnectFailType.NullableBluetoothDevice
            )
            return
        }
        val bleManager = getBleManager()
        if (!BleUtil.isPermission(bleManager.getContext()?.applicationContext)) {
            BleLogger.e("权限不足，请检查")
            removeBleConnectedDevice()
            bleConnectCallback.callConnectFail(createNewDeviceInfo(), BleConnectFailType.NoBlePermission)
            getBleConnectedDevice(bleDevice)?.getBleEventCallback()?.callConnectFail(
                createNewDeviceInfo(), BleConnectFailType.NoBlePermission
            )
            return
        }
        if (!bleManager.isBleSupport()) {
            BleLogger.e("设备不支持蓝牙")
            removeBleConnectedDevice()
            bleConnectCallback.callConnectFail(createNewDeviceInfo(), BleConnectFailType.UnSupportBle)
            getBleConnectedDevice(bleDevice)?.getBleEventCallback()?.callConnectFail(
                createNewDeviceInfo(), BleConnectFailType.UnSupportBle
            )
            return
        }
        if (!bleManager.isBleEnable()) {
            BleLogger.e("蓝牙未打开")
            removeBleConnectedDevice()
            bleConnectCallback.callConnectFail(createNewDeviceInfo(), BleConnectFailType.BleDisable)
            getBleConnectedDevice(bleDevice)?.getBleEventCallback()?.callConnectFail(
                createNewDeviceInfo(), BleConnectFailType.BleDisable
            )
            return
        }
        if (lastState == BleConnectLastState.Connecting || lastState == BleConnectLastState.ConnectIdle) {
            BleLogger.e("${bleDevice.deviceAddress}连接中")
//            removeBleConnectedDevice()
            bleConnectCallback.callConnectFail(createNewDeviceInfo(), BleConnectFailType.AlreadyConnecting)
            getBleConnectedDevice(bleDevice)?.getBleEventCallback()?.callConnectFail(
                createNewDeviceInfo(), BleConnectFailType.AlreadyConnecting
            )
            return
        }
        this.connectMillisTimeOut = connectMillisTimeOut
        this.connectRetryCount = connectRetryCount
        this.connectRetryInterval = connectRetryInterval
        //主要针对某些机型，当触发连接超时回调连接失败并释放资源之后，此时外设开启触发手机系统已连接，但BleCore资源被释放
        // (bluetoothGatt是null)，或BleCore和系统的连接状态不一致，而导致setMtu和Notify/Indicate都失败。
        val systemConnectStatus = bleManager.isConnected(bleDevice, true)
        val bleCoreConnectStatus = bleManager.isConnected(bleDevice, false)
        BleLogger.e("设备[${bleDevice.deviceAddress}]当前连接状态，系统已连接$systemConnectStatus，" +
                "BleCore连接状态$bleCoreConnectStatus，" +
                " 是否强制连接$isForceConnect， " +
                "bluetoothGatt是否为空${bluetoothGatt == null}")
        //如果BleCore或者系统对应的状态是未连接、或者强制连接的情况
        if (!systemConnectStatus || !bleCoreConnectStatus || isForceConnect || bluetoothGatt == null) {
            bleConnectCallback.callConnectStart()
            getBleConnectedDevice(bleDevice)?.getBleEventCallback()?.callConnectStart()
            startConnectJob()
        } else {
            //如果BleCore和系统对应的状态都是已连接，则直接返回状态
            lastState = BleConnectLastState.Connected
            val deviceInfo = createNewDeviceInfo()
            addBleConnectedDevice()
            bleConnectCallback.callConnectSuccess(deviceInfo, bluetoothGatt)
            getBleConnectedDevice(bleDevice)?.getBleEventCallback()
                ?.callConnected(deviceInfo, bluetoothGatt)
            autoSetMtu()
        }
    }

    /**
     * 主动断开连接，上层API调用
     */
    fun disConnect() {
        if (bleDevice.deviceInfo == null) {
            BleLogger.e("[${bleDevice.deviceAddress}]断开失败：BluetoothDevice为空")
            bleConnectCallback?.callConnectFail(createNewDeviceInfo(), BleConnectFailType.NullableBluetoothDevice)
            getBleConnectedDevice(bleDevice)?.getBleEventCallback()?.callConnectFail(
                createNewDeviceInfo(), BleConnectFailType.NullableBluetoothDevice
            )
            return
        }
        val bleManager = getBleManager()
        if (!BleUtil.isPermission(bleManager.getContext()?.applicationContext)) {
            BleLogger.e("权限不足，请检查")
            bleConnectCallback?.callConnectFail(createNewDeviceInfo(), BleConnectFailType.NoBlePermission)
            getBleConnectedDevice(bleDevice)?.getBleEventCallback()?.callConnectFail(
                createNewDeviceInfo(), BleConnectFailType.NoBlePermission
            )
            return
        }
        if (!bleManager.isBleSupport()) {
            BleLogger.e("设备不支持蓝牙")
            bleConnectCallback?.callConnectFail(createNewDeviceInfo(), BleConnectFailType.UnSupportBle)
            getBleConnectedDevice(bleDevice)?.getBleEventCallback()?.callConnectFail(
                createNewDeviceInfo(), BleConnectFailType.UnSupportBle
            )
            return
        }
        if (!bleManager.isBleEnable()) {
            BleLogger.e("蓝牙未打开")
            bleConnectCallback?.callConnectFail(createNewDeviceInfo(), BleConnectFailType.BleDisable)
            getBleConnectedDevice(bleDevice)?.getBleEventCallback()?.callConnectFail(
                createNewDeviceInfo(), BleConnectFailType.BleDisable
            )
            return
        }
        isActiveDisconnect.set(true)
        lastState = BleConnectLastState.Disconnect
        if (lastState == BleConnectLastState.ConnectIdle ||
            lastState == BleConnectLastState.Connecting) {
            val throwable = ActiveDisConnectedException("连接过程中断开")
            connectJob?.cancel(throwable)
            waitConnectJob?.cancel(throwable)
        } else {
            disConnectGatt()
            BleLogger.e("${bleDevice.deviceAddress} -> 主动断开连接")
            val deviceInfo = createNewDeviceInfo()
            bleConnectCallback?.callDisConnecting(
                isActiveDisconnect.get(),
                deviceInfo, bluetoothGatt, BluetoothGatt.GATT_SUCCESS
            )
            getBleConnectedDevice(bleDevice)?.getBleEventCallback()?.callDisConnecting(
                isActiveDisconnect.get(),
                deviceInfo, bluetoothGatt, BluetoothGatt.GATT_SUCCESS
            )
            BleRequestImp.get().getMainScope().launch {
                delay(600)
                removeAllCallback()
            }
        }
    }

    /**
     * 取消/停止连接
     */
    fun stopConnect() {
        if (lastState == BleConnectLastState.ConnectIdle ||
            lastState == BleConnectLastState.Connecting) {
            val throwable = ActiveStopConnectedException("[${bleDevice.deviceAddress}]连接过程中取消/停止连接")
            connectJob?.cancel(throwable)
            waitConnectJob?.cancel(throwable)
        } else {
            BleLogger.i("[${bleDevice.deviceAddress}]非连接过程中，取消/停止连接无效")
        }
    }

    /**
     * 当连接上设备或者失去连接时会触发
     */
    fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
        BleLogger.i("onConnectionStateChange： status = $status " +
                ", newState = $newState , currentThread = ${Thread.currentThread().name} " +
                ", bleAddress = ${bleDevice.deviceAddress} , lastState = $lastState")
        if (lastState == BleConnectLastState.ConnectFailure) {
            //上一个状态为null即调用了close，就不再处理，这里出现有些手机，调用了close之后，还会触发onConnectionStateChange
            disConnectGatt()
            refreshDeviceCache()
            closeBluetoothGatt()
            return
        }
        BleLogger.i("[${bleDevice.deviceAddress}]连接状态变化BluetoothGatt是否为空：${gatt == null}")
        gatt?.let {
            bluetoothGatt = it
        }
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
                BleConnectLastState.ConnectIdle, BleConnectLastState.Connecting -> {
                    //连接过程中断开，进入判断是否重连
                    refreshDeviceCache()
                    closeBluetoothGatt()
                    lastState = BleConnectLastState.Disconnect
                    checkIfContinueConnect(UnDefinedException("[${bleDevice.deviceAddress}]连接过程中断开"))
                }
                BleConnectLastState.ConnectFailure -> {
                    BleLogger.i("连接失败后，设备[${bleDevice.deviceAddress}]触发断开连接")
                    refreshDeviceCache()
                    closeBluetoothGatt()
                }
                //所有断开连接的情况
                else -> {
                    refreshDeviceCache()
                    closeBluetoothGatt()
                    lastState = BleConnectLastState.Disconnect
                    if (!isActiveDisconnect.get()) {
                        BleLogger.e("${bleDevice.deviceAddress} -> 自动断开连接")
                        val deviceInfo = createNewDeviceInfo()
                        bleConnectCallback?.callDisConnecting(
                            isActiveDisconnect.get(),
                            deviceInfo, gatt, status
                        )
                        getBleConnectedDevice(bleDevice)?.getBleEventCallback()?.callDisConnecting(
                            isActiveDisconnect.get(),
                            deviceInfo, gatt, status
                        )
                        BleRequestImp.get().getMainScope().launch {
                            delay(600)
                            removeAllCallback()
                        }
                    }
                }
            }
        }
    }

    /**
     * 当设备是否找到服务[bluetoothGatt?.discoverServices()]时会触发
     */
    fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
        gatt?.let {
            bluetoothGatt = it
        }
        BleLogger.i("[${bleDevice.deviceAddress}]发现服务BluetoothGatt是否为空：${gatt == null}")
        if (status == BluetoothGatt.GATT_SUCCESS) {
            BleLogger.i("${bleDevice.deviceAddress} -> 连接成功，发现服务")
            currentConnectRetryCount = 0
            lastState = BleConnectLastState.Connected
            isActiveDisconnect.set(false)
            val deviceInfo = createNewDeviceInfo()
            addBleConnectedDevice()
            bleConnectCallback?.callConnectSuccess(deviceInfo, bluetoothGatt)
            getBleConnectedDevice(bleDevice)?.getBleEventCallback()?.callConnected(deviceInfo, bluetoothGatt)
            autoSetMtu()
        } else {
            connectFail()
            BleLogger.e("${bleDevice.deviceAddress} -> 连接失败：未发现服务")
            bleConnectCallback?.callConnectFail(
                createNewDeviceInfo(),
                BleConnectFailType.ConnectException(UnDefinedException("发现服务失败"))
            )
            getBleConnectedDevice(bleDevice)?.getBleEventCallback()?.callConnectFail(
                createNewDeviceInfo(),
                BleConnectFailType.ConnectException(UnDefinedException("发现服务失败"))
            )
        }
    }

    fun addBleConnectCallback(bleConnectCallback: BleConnectCallback) {
        this.bleConnectCallback = bleConnectCallback
    }

    fun removeBleConnectCallback() {
        bleConnectCallback = null
    }

    /**
     * 获取设备的BluetoothGatt对象
     */
    fun getBluetoothGatt(): BluetoothGatt? {
        return bluetoothGatt
    }

    /**
     * 断开所有连接 释放资源
     */
    fun close() {
        val deviceInfo = createNewDeviceInfo()
        bleConnectCallback?.callDisConnecting(
            isActiveDisconnect.get(),
            deviceInfo, bluetoothGatt, BluetoothGatt.GATT_SUCCESS
        )
        getBleConnectedDevice(bleDevice)?.getBleEventCallback()?.callDisConnecting(
            isActiveDisconnect.get(),
            deviceInfo, bluetoothGatt, BluetoothGatt.GATT_SUCCESS
        )
        lastState = null
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
        lastState = BleConnectLastState.ConnectIdle
        isActiveDisconnect.set(false)
        var connectTime = connectMillisTimeOut?: (getBleOptions()?.connectMillisTimeOut?: DEFAULT_CONNECT_MILLIS_TIMEOUT)
        if (connectTime <= 0) {
            connectTime = DEFAULT_CONNECT_MILLIS_TIMEOUT
        }
        connectJob = bleConnectCallback?.launchInMainThread {
            withTimeout(connectTime) {
                //每次连接之前确保和上一次操作间隔一定时间
                delay(waitTime)
                lastState = BleConnectLastState.Connecting
                bluetoothGatt = bleDevice.deviceInfo?.connectGatt(getBleManager().getContext(),
                    autoConnect, coreGattCallback, BluetoothDevice.TRANSPORT_LE)
                BleLogger.i("${bleDevice.deviceAddress} -> 开始第${currentConnectRetryCount + 1}次连接")
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
            val retryInterval = connectRetryInterval?: (getBleOptions()?.connectRetryInterval?: DEFAULT_CONNECT_RETRY_INTERVAL)
            waitConnectJob = bleConnectCallback?.launchInMainThread {
                delay(retryInterval)
                currentConnectRetryCount ++
                startConnectJob()
            }
            waitConnectJob?.invokeOnCompletion {
                if (it is ActiveDisConnectedException || it is CompleteException || it is ActiveStopConnectedException) {
                    onCompletion(it)
                }
            }
        } else {
            throwable?.let {
                when (it) {
                    //连接超时
                    is TimeoutCancellationException -> {
                        connectFail()
                        val connectTime = connectMillisTimeOut?: (getBleOptions()?.connectMillisTimeOut?: DEFAULT_CONNECT_MILLIS_TIMEOUT)
                        val retryCount = connectRetryCount?: (getBleOptions()?.connectRetryCount?: 0)
                        BleLogger.e("${bleDevice.deviceAddress} -> 连接失败：超时${connectTime * (retryCount + 1)}ms")
                        bleConnectCallback?.callConnectFail(
                            createNewDeviceInfo(),
                            BleConnectFailType.ConnectTimeOut
                        )
                        getBleConnectedDevice(bleDevice)?.getBleEventCallback()?.callConnectFail(
                            createNewDeviceInfo(),
                            BleConnectFailType.ConnectTimeOut
                        )
                    }
                    //连接成功
                    is CompleteException -> {
                        //发现服务
                        BleLogger.e(it.message)
                        findService()
                    }
                    //主动断开
                    is ActiveDisConnectedException -> {
                        BleLogger.e(it.message)
                        disConnectGatt()
                    }
                    is ActiveStopConnectedException -> {
                        connectFail()
                        BleLogger.e("${bleDevice.deviceAddress} -> 连接失败：${it.message}")
                        bleConnectCallback?.callConnectFail(
                            createNewDeviceInfo(),
                            BleConnectFailType.ConnectException(it)
                        )
                        getBleConnectedDevice(bleDevice)?.getBleEventCallback()?.callConnectFail(
                            createNewDeviceInfo(),
                            BleConnectFailType.ConnectException(it)
                        )
                    }
                    //连接失败
                    else -> {
                        connectFail()
                        BleLogger.e("${bleDevice.deviceAddress} -> 连接失败：${it.message}")
                        bleConnectCallback?.callConnectFail(
                            createNewDeviceInfo(),
                            BleConnectFailType.ConnectException(it)
                        )
                        getBleConnectedDevice(bleDevice)?.getBleEventCallback()?.callConnectFail(
                            createNewDeviceInfo(),
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
     * 判断是否进入重连机制，如果当前没有进入重新机制，则进入重连机制；如果进入了重连机制，则不打断重连的顺序
     */
    private fun checkIfContinueConnect(throwable: Throwable?) {
        if (currentConnectRetryCount != 0) {
            return
        }
        onCompletion(throwable)
    }

    /**
     * 是否继续连接
     */
    private fun isContinueConnect(throwable: Throwable?): Boolean {
        if (throwable is ActiveDisConnectedException || throwable is CompleteException || throwable is ActiveStopConnectedException) {
            return false
        }
        if (!isActiveDisconnect.get() && lastState != BleConnectLastState.Connected) {
            var retryCount = connectRetryCount?: (getBleOptions()?.connectRetryCount?: 0)
            if (retryCount < 0) {
                retryCount = 0
            }
            //满足重连条件，则先把上一次连接的状态close
            closeBluetoothGatt()
            if (retryCount > 0 && currentConnectRetryCount < retryCount) {
                BleLogger.i("${bleDevice.deviceAddress} -> 满足重连条件：" +
                        "当前连接失败次数：${currentConnectRetryCount + 1}次")
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
                val exception = UnDefinedException("${bleDevice.deviceAddress} -> 发现服务失败")
                BleLogger.e(exception.message)
                bleConnectCallback?.callConnectFail(
                    createNewDeviceInfo(),
                    BleConnectFailType.ConnectException(exception)
                )
                getBleConnectedDevice(bleDevice)?.getBleEventCallback()?.callConnectFail(
                    createNewDeviceInfo(),
                    BleConnectFailType.ConnectException(exception)
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
     * 连接成功，保证管理器中存在该对象。
     * 因为连接失败后，会删除该对象，然后满足重连条件的时候该对象不存在
     */
    private fun addBleConnectedDevice() {
        BleConnectedDeviceManager.get().buildBleConnectedDevice(bleDevice)
    }

    /**
     * 移除设备管理池中的设备
     */
    private fun removeAllCallback() {
        getBleConnectedDevice(bleDevice)?.removeAllCharacterCallback()
        removeBleConnectedDevice()
    }

    /**
     * 自动设置mtu
     */
    private fun autoSetMtu() {
        if (getBleOptions()?.autoSetMtu == true) {
            getBleConnectedDevice(bleDevice)?.setMtu(getBleOptions()?.mtu?: DEFAULT_MTU,
                object : BleMtuChangedCallback() {
                    override fun callMtuChanged(bleDevice: BleDevice, mtu: Int) {
                        super.callMtuChanged(bleDevice, mtu)
                        BleLogger.d("${bleDevice.deviceAddress} -> 自动设置Mtu成功: $mtu")
                    }

                    override fun callSetMtuFail(bleDevice: BleDevice, throwable: Throwable) {
                        super.callSetMtuFail(bleDevice, throwable)
                        BleLogger.e("${bleDevice.deviceAddress} -> " +
                                "自动设置Mtu失败: ${throwable.message}")
                    }
                })
        }
    }

    private fun createNewDeviceInfo(): BleDevice {
        if (bluetoothGatt == null || bluetoothGatt?.device == null) {
            return bleDevice
        }
        return BleDevice(
            deviceInfo = bluetoothGatt?.device?: bleDevice.deviceInfo,
            deviceName = bluetoothGatt?.device?.name?: bleDevice.deviceName,
            deviceAddress = bluetoothGatt?.device?.address?: bleDevice.deviceAddress,
            rssi = bleDevice.rssi,
            timestampNanos = bleDevice.timestampNanos,
            scanRecord = bleDevice.scanRecord,
            tag = bleDevice.tag,
        )
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
    private fun disConnectGatt() {
        if (getBleManager().isConnected(bleDevice, true)) {
            bluetoothGatt?.disconnect()
        }
    }

    /**
     * 刷新缓存
     */
    private fun refreshDeviceCache() {
        try {
            val refresh = BluetoothGatt::class.java.getMethod("refresh")
            if (bluetoothGatt != null) {
                val success = refresh.invoke(bluetoothGatt) as Boolean
                BleLogger.d("refreshDeviceCache, is success:  $success")
            }
        } catch (e: Exception) {
            BleLogger.e("exception occur while refreshing device: " + e.message)
            e.printStackTrace()
        }
    }

    /**
     * 关闭Gatt
     */
    private fun closeBluetoothGatt() {
        bluetoothGatt?.close()
    }
}