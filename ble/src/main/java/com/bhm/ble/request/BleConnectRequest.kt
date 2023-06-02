/*
 * Copyright (c) 2022-2032 buhuiming
 * 不能修改和删除上面的版权声明
 * 此代码属于buhuiming编写，在未经允许的情况下不得传播复制
 */
package com.bhm.ble.request

import android.annotation.SuppressLint
import android.bluetooth.*
import android.os.Build
import com.bhm.ble.attribute.BleOptions
import com.bhm.ble.callback.*
import com.bhm.ble.control.ActiveDisConnectedThrowable
import com.bhm.ble.control.CompleteThrowable
import com.bhm.ble.control.NotifyFailException
import com.bhm.ble.data.BleConnectFailType
import com.bhm.ble.data.BleConnectLastState
import com.bhm.ble.data.BleDevice
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

    private val waitTime = 100L

    private var notifyJob: Job? = null

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
        if (lastState == BleConnectLastState.Connecting || lastState == BleConnectLastState.ConnectIdle) {
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
                        connectFail()
                        bleConnectCallback?.callConnectFail(
                            bleDevice,
                            BleConnectFailType.ConnectTimeOut
                        )
                    }
                    //连接成功
                    is CompleteThrowable -> {
                        //发现服务
                        findService()
                    }
                    //主动断开
                    is ActiveDisConnectedThrowable -> {
                        disConnectGatt()
                    }
                    //连接失败
                    else -> {
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
            val throwable = ActiveDisConnectedThrowable("连接过程中断开")
            connectJob?.cancel(throwable)
            waitConnectJob?.cancel(throwable)
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
        /**
         * 当连接上设备或者失去连接时会触发
         */
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            BleLogger.i(
                """
                    BluetoothGattCallback：onConnectionStateChange 
                    status: $status
                    newState: $newState
                    currentThread: ${Thread.currentThread().name}
                    bleAddress: ${bleDevice.deviceAddress}
                    lastState: $lastState
                    """.trimIndent()
            )
            bluetoothGatt = gatt

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                //连接成功
                if (connectJob?.isActive == true || waitConnectJob?.isActive == true) {
                    val throwable = CompleteThrowable()
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
                }
            }
        }

        /**
         * 当设备是否找到服务[bluetoothGatt?.discoverServices()]时会触发
         */
        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)
            bluetoothGatt = gatt
            if (status == BluetoothGatt.GATT_SUCCESS) {
                BleLogger.e("2---------${System.currentTimeMillis()}")
                BleLogger.i("连接成功，发现服务")
                currentConnectRetryCount = 0
                lastState = BleConnectLastState.Connected
                isActiveDisconnect.set(false)
                bleConnectCallback?.callConnectSuccess(bleDevice, bluetoothGatt)
            } else {
                connectFail()
                bleConnectCallback?.callConnectFail(
                    bleDevice,
                    BleConnectFailType.ConnectException(Throwable("发现服务失败"))
                )
            }
        }

        /**
         * 设备发出通知时会时会触发
         */
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            super.onCharacteristicChanged(gatt, characteristic, value)
            bleNotifyCallbackHashMap.values.forEach {
                if (characteristic.uuid?.toString().equals(it.getKey(), ignoreCase = true)) {
                    it.callCharacteristicChanged(value)
                }
            }
            bleIndicateCallbackHashMap.values.forEach {
                if (characteristic.uuid?.toString().equals(it.getKey(), ignoreCase = true)) {
                    it.callCharacteristicChanged(value)
                }
            }
        }

        /**
         * 当向设备Descriptor中写数据时会触发
         */
        override fun onDescriptorWrite(
            gatt: BluetoothGatt?,
            descriptor: BluetoothGattDescriptor?,
            status: Int
        ) {
            super.onDescriptorWrite(gatt, descriptor, status)
            bleNotifyCallbackHashMap.values.forEach {
                if (descriptor?.characteristic?.uuid?.toString().equals(it.getKey(), ignoreCase = true)) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        cancelNotifyJob(CancellationException("Descriptor写数据成功"))
                        it.callNotifySuccess()
                    } else {
                        val exception = NotifyFailException.DescriptorException
                        cancelNotifyJob(exception)
                        it.callNotifyFail(exception)
                    }
                }
            }
            bleIndicateCallbackHashMap.values.forEach {
                if (descriptor?.characteristic?.uuid.toString().equals(it.getKey(), ignoreCase = true)) {

                }
            }
        }

        /**
         * 当读取设备时会触发
         */
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int
        ) {
            super.onCharacteristicRead(gatt, characteristic, value, status)
        }

        /**
         * 当向Characteristic写数据时会触发
         */
        override fun onCharacteristicWrite(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            super.onCharacteristicWrite(gatt, characteristic, status)
        }
    }

    /**
     * notify
     */
    @Synchronized
    fun enableCharacteristicNotify(serviceUUID: String,
                                   notifyUUID: String,
                                   userCharacteristicDescriptor: Boolean,
                                   bleNotifyCallback: BleNotifyCallback) {
        val gattService = bluetoothGatt?.getService(UUID.fromString(serviceUUID))
        val characteristic = gattService?.getCharacteristic(UUID.fromString(notifyUUID))
        if (bluetoothGatt != null && gattService != null && characteristic != null &&
            (characteristic.properties or BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0
        ) {
            cancelNotifyJob(null)
            bleNotifyCallback.setKey(notifyUUID)
            addNotifyCallback(notifyUUID, bleNotifyCallback)
            var operateTime = getBleOptions()?.operateMillisTimeOut?: BleOptions.DEFAULT_OPERATE_MILLIS_TIMEOUT
            if (operateTime <= 0) {
                operateTime = BleOptions.DEFAULT_OPERATE_MILLIS_TIMEOUT
            }
            notifyJob = CoroutineScope(Dispatchers.IO).launch {
                withTimeout(operateTime) {
                    delay(operateTime)
                }
            }
            notifyJob?.invokeOnCompletion {
                it?.let {
                    BleLogger.e(it.message)
                    if (it is TimeoutCancellationException) {
                        bleNotifyCallback.callNotifyFail(it)
                    }
                }
            }
            setCharacteristicNotify(characteristic, userCharacteristicDescriptor, true, bleNotifyCallback)
        } else {
            bleNotifyCallback.callNotifyFail(NotifyFailException.UnSupportNotifyException)
        }
    }

    /**
     * stop notify
     */
    @Synchronized
    fun disableCharacteristicNotify(serviceUUID: String,
                                   notifyUUID: String,
                                   userCharacteristicDescriptor: Boolean): Boolean {
        val gattService = bluetoothGatt?.getService(UUID.fromString(serviceUUID))
        val characteristic = gattService?.getCharacteristic(UUID.fromString(notifyUUID))
        if (bluetoothGatt != null && gattService != null && characteristic != null &&
            (characteristic.properties or BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0
        ) {
            cancelNotifyJob(null)
            return setCharacteristicNotify(characteristic, userCharacteristicDescriptor, false, null)
        }
        return false
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

    @Synchronized
    fun addNotifyCallback(uuid: String?, bleNotifyCallback: BleNotifyCallback?) {
        bleNotifyCallbackHashMap[uuid!!] = bleNotifyCallback!!
    }

    @Synchronized
    fun addIndicateCallback(uuid: String?, bleIndicateCallback: BleIndicateCallback?) {
        bleIndicateCallbackHashMap[uuid!!] = bleIndicateCallback!!
    }

    @Synchronized
    fun addWriteCallback(uuid: String?, bleWriteCallback: BleWriteCallback?) {
        bleWriteCallbackHashMap[uuid!!] = bleWriteCallback!!
    }

    @Synchronized
    fun addReadCallback(uuid: String?, bleReadCallback: BleReadCallback?) {
        bleReadCallbackHashMap[uuid!!] = bleReadCallback!!
    }

    @Synchronized
    fun removeNotifyCallback(uuid: String?) {
        if (bleNotifyCallbackHashMap.containsKey(uuid)) bleNotifyCallbackHashMap.remove(uuid)
    }

    @Synchronized
    fun removeIndicateCallback(uuid: String?) {
        if (bleIndicateCallbackHashMap.containsKey(uuid)) bleIndicateCallbackHashMap.remove(uuid)
    }

    @Synchronized
    fun removeWriteCallback(uuid: String?) {
        if (bleWriteCallbackHashMap.containsKey(uuid)) bleWriteCallbackHashMap.remove(uuid)
    }

    @Synchronized
    fun removeReadCallback(uuid: String?) {
        if (bleReadCallbackHashMap.containsKey(uuid)) bleReadCallbackHashMap.remove(uuid)
    }

    @Synchronized
    fun addRssiCallback(callback: BleRssiCallback) {
        bleRssiCallback = callback
    }

    @Synchronized
    fun removeRssiCallback() {
        bleRssiCallback = null
    }

    @Synchronized
    fun addMtuChangedCallback(callback: BleMtuChangedCallback) {
        bleMtuChangedCallback = callback
    }

    @Synchronized
    fun removeMtuChangedCallback() {
        bleMtuChangedCallback = null
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
        cancelNotifyJob(null)
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
        if (throwable is ActiveDisConnectedThrowable || throwable is CompleteThrowable) {
            return false
        }
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
     * 发现服务
     */
    private fun findService() {
        bleConnectCallback?.launchInMainThread {
            delay(waitTime * 5)
            BleLogger.e("1---------${System.currentTimeMillis()}")
            if (bluetoothGatt == null || bluetoothGatt?.discoverServices() == false) {
                connectFail()
                bleConnectCallback?.callConnectFail(
                    bleDevice,
                    BleConnectFailType.ConnectException(Throwable("发现服务失败"))
                )
            }
        }
    }

    /**
     * 连接失败 更改状态和释放资源
     */
    private fun connectFail() {
        lastState = BleConnectLastState.ConnectFailure
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


    /**
     * 取消设置notify
     */
    private fun cancelNotifyJob(exception: CancellationException?) {
        notifyJob?.cancel(exception)
    }

    /**
     * 配置Notify
     */
    private fun setCharacteristicNotify(characteristic: BluetoothGattCharacteristic,
                                        useCharacteristicDescriptor: Boolean,
                                        enable: Boolean,
                                        bleNotifyCallback: BleNotifyCallback?): Boolean {
        val setSuccess = bluetoothGatt?.setCharacteristicNotification(characteristic, enable)
        if (setSuccess != true) {
            val exception = NotifyFailException.SetCharacteristicNotificationFailException
            cancelNotifyJob(exception)
            bleNotifyCallback?.callNotifyFail(exception)
            return false
        }
        val descriptor = if (useCharacteristicDescriptor) {
            characteristic.getDescriptor(characteristic.uuid)
        } else {
            characteristic.getDescriptor(UUID.fromString(BleOptions.UUID_CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR))
        }
        if (descriptor == null) {
            val exception = NotifyFailException.SetCharacteristicNotificationFailException
            cancelNotifyJob(exception)
            bleNotifyCallback?.callNotifyFail(exception)
            return false
        }
        val success: Boolean
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val writeDescriptor: Int? = if (enable) {
                bluetoothGatt?.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
            } else {
                bluetoothGatt?.writeDescriptor(descriptor, BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE)
            }
            success = writeDescriptor == BluetoothStatusCodes.SUCCESS
        } else {
            @Suppress("DEPRECATION")
            descriptor.value = if (enable) {
                BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            } else {
                BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
            }
            @Suppress("DEPRECATION")
            val writeDescriptor: Boolean? = bluetoothGatt?.writeDescriptor(descriptor)
            success = writeDescriptor == true
        }
        if (!success) {
            val exception = NotifyFailException.DescriptorException
            cancelNotifyJob(exception)
            bleNotifyCallback?.callNotifyFail(exception)
            return false
        }
        return true
    }
}