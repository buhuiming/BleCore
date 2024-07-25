/*
 * Copyright (c) 2022-2032 buhuiming
 * 不能修改和删除上面的版权声明
 * 此代码属于buhuiming编写，在未经允许的情况下不得传播复制
 */
@file:Suppress("RemoveExplicitTypeArguments")

package com.bhm.ble.request

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.os.Build
import android.util.SparseArray
import com.bhm.ble.callback.BleWriteCallback
import com.bhm.ble.data.BleWriteData
import com.bhm.ble.data.Constants.DEFAULT_MTU
import com.bhm.ble.data.Constants.WRITE_TASK_ID
import com.bhm.ble.data.NoBlePermissionException
import com.bhm.ble.data.TimeoutCancelException
import com.bhm.ble.data.UnDefinedException
import com.bhm.ble.data.UnSupportException
import com.bhm.ble.device.BleDevice
import com.bhm.ble.log.BleLogger
import com.bhm.ble.request.base.BleTaskQueueRequest
import com.bhm.ble.utils.BleUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Collections
import java.util.LinkedList
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


/**
 * 设备写请求
 *
 * @author Buhuiming
 * @date 2023年06月07日 15时58分
 */
internal class BleWriteRequest(
    private val bleDevice: BleDevice,
) : BleTaskQueueRequest(bleDevice, "Write队列") {

    private val bleWriteDataHashMap:
            ConcurrentHashMap<String, MutableList<BleWriteData>> = ConcurrentHashMap()

    /**
     * 写数据队列。写成功才写下一包。Ble库目前没有这样处理
     */
    private val linkedBlockingQueue = LinkedBlockingQueue<ByteArray>()

    /**
     * 写数据临时队列
     */
    private val linkedBlockingTempQueue = LinkedBlockingQueue<ByteArray>()

    /**
     * 添加任务线程
     */
    private val addWriteJobScope = CoroutineScope(Dispatchers.IO)

    /**
     * 写队列线程
     */
    private val writeJobScope = CoroutineScope(Dispatchers.IO)

    /**
     * 当前重写次数
     */
    private var currentRetryWriteCount = AtomicInteger(0)

    private fun addBleWriteData(uuid: String, bleWriteData: BleWriteData) {
        if (bleWriteDataHashMap.containsKey(uuid) && bleWriteDataHashMap[uuid] != null) {
            bleWriteDataHashMap[uuid]?.add(bleWriteData)
        } else {
            val list = Collections.synchronizedList(LinkedList<BleWriteData>())
            list.add(bleWriteData)
            bleWriteDataHashMap[uuid] = list
        }
    }

    fun removeWriteCallback(uuid: String?, bleWriteCallback: BleWriteCallback? = null) {
        if (bleWriteDataHashMap.containsKey(uuid)) {
            if (bleWriteCallback != null) {
                bleWriteDataHashMap[uuid]?.let { list ->
                    synchronized(list) {
                        val iterator = list.iterator()
                        while (iterator.hasNext()) {
                            if (iterator.next().bleWriteCallback == bleWriteCallback) {
                                iterator.remove()
                            }
                        }
                    }
                }
            } else {
                bleWriteDataHashMap.remove(uuid)
            }
        }
    }

    fun removeAllWriteCallback() {
        bleWriteDataHashMap.clear()
    }

    /**
     * 放入一个写队列，写成功，则从队列中取下一个数据，写失败，则重试[retryWriteCount]次
     *  与[writeData]的区别在于，[writeData]写成功，则从队列中取下一个数据，写失败，则不再继续写后面的数据
     */
    fun writeQueueData(
        serviceUUID: String,
        writeUUID: String,
        operateRandomID: String,
        dataArray: SparseArray<ByteArray>,
        skipErrorPacketData: Boolean = false,
        retryWriteCount: Int = 0,
        retryDelayTime: Long = 0L,
        writeType: Int?,
        bleWriteCallback: BleWriteCallback
    ) {
        if (!BleUtil.isPermission(getBleManager().getContext())) {
            bleWriteCallback.callWriteFail(bleDevice, 0, dataArray.size(), NoBlePermissionException())
            bleWriteCallback.callWriteComplete(bleDevice, false)
            return
        }
        if (dataArray.size() == 0) {
            val exception = UnDefinedException(
                getTaskId(writeUUID, operateRandomID, 0) + " -> 写数据失败，数据为空"
            )
            BleLogger.e(exception.message)
            bleWriteCallback.callWriteFail(bleDevice, 0, 0, exception)
            bleWriteCallback.callWriteComplete(bleDevice, false)
            return
        }
        addWriteJobScope.launch {
            for (i in 0 until dataArray.size()) {
                val data = dataArray.valueAt(i)
                linkedBlockingTempQueue.put(data)
            }
            if (getWriteDataFromTemp()) {
                startWriteQueueJob(
                    serviceUUID,
                    writeUUID,
                    operateRandomID,
                    skipErrorPacketData,
                    retryWriteCount,
                    retryDelayTime,
                    writeType,
                    bleWriteCallback
                )
            }
        }
    }

    fun writeData(
        serviceUUID: String,
        writeUUID: String,
        operateRandomID: String,
        dataArray: SparseArray<ByteArray>,
        writeType: Int?,
        bleWriteCallback: BleWriteCallback
    ) {
        if (!BleUtil.isPermission(getBleManager().getContext())) {
            bleWriteCallback.callWriteFail(bleDevice, 0, dataArray.size(), NoBlePermissionException())
            bleWriteCallback.callWriteComplete(bleDevice, false)
            return
        }
        if (dataArray.size() == 0) {
            val exception = UnDefinedException(
                getTaskId(writeUUID, operateRandomID, 0) + " -> 写数据失败，数据为空"
            )
            BleLogger.e(exception.message)
            bleWriteCallback.callWriteFail(bleDevice, 0, 0, exception)
            bleWriteCallback.callWriteComplete(bleDevice, false)
            return
        }
        for (i in 0 until dataArray.size()) {
            val data = dataArray.valueAt(i)
            if (data == null || data.isEmpty()) {
                val exception = UnDefinedException(
                    getTaskId(writeUUID, operateRandomID, i + 1) +
                            " -> 写数据失败，第${i + 1}个数据包为空"
                )
                BleLogger.e(exception.message)
                bleWriteCallback.callWriteFail(bleDevice, i + 1, dataArray.size(), exception)
                bleWriteCallback.callWriteComplete(bleDevice, false)
                return
            }
            val mtu = getBleOptions()?.mtu?: DEFAULT_MTU
            //mtu长度包含了ATT的opcode一个字节以及ATT的handle2个字节
            val maxWriteLength = mtu - 3
            if (data.size > maxWriteLength) {
                val exception = UnDefinedException("${getTaskId(writeUUID, operateRandomID
                    , i + 1)} -> " + "写数据失败，第${i + 1}个数据包" +
                        "长度(${data.size}) + 3大于设定Mtu($mtu)")
                BleLogger.e(exception.message)
                bleWriteCallback.callWriteFail(bleDevice, i + 1, dataArray.size(), exception)
                bleWriteCallback.callWriteComplete(bleDevice, false)
                return
            }
        }

        val characteristic = getCharacteristic(serviceUUID, writeUUID)

        if (characteristic != null &&
            (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_WRITE != 0 ||
                    characteristic.properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0)
        ) {
            //循环数据包，生成对应的任务
            for (i in 0 until dataArray.size()) {
                val bleWriteData = BleWriteData(
                    operateRandomID = operateRandomID,
                    serviceUUID = serviceUUID,
                    writeUUID = writeUUID,
                    currentPackage = i + 1,
                    totalPackage = dataArray.size(),
                    data = dataArray.valueAt(i),
                    isWriting = AtomicBoolean(false),
                    bleWriteCallback = bleWriteCallback,
                    writeType = writeType
                )
                addBleWriteData(writeUUID, bleWriteData)
                startWriteJob(
                    characteristic,
                    (getTaskQueue(bleWriteData.writeUUID)?.getTaskList()?.size()?: 0) + i + 1,
                    bleWriteData
                )
            }
        } else {
            val exception = UnSupportException("$writeUUID -> 写数据失败，此特性不支持写数据")
            BleLogger.e(exception.message)
            bleWriteCallback.callWriteFail(bleDevice, 0, dataArray.size(), exception)
            bleWriteCallback.callWriteComplete(bleDevice, false)
        }
    }

    @SuppressLint("MissingPermission")
    private fun startWriteJob(
        characteristic: BluetoothGattCharacteristic,
        index: Int,
        bleWriteData: BleWriteData
    ) {
        var mContinuation: Continuation<Throwable?>? = null
        val task = getTask(
            getTaskId(bleWriteData.writeUUID, bleWriteData.operateRandomID, bleWriteData.currentPackage),
            getOperateTime() * index,
            block = {
                suspendCoroutine<Throwable?> { continuation ->
                    mContinuation = continuation
                    bleWriteData.isWriting = AtomicBoolean(true)
                    startWrite(characteristic, bleWriteData)
                }
            },
            interrupt = { _, throwable ->
                try {
                    try {
                        mContinuation?.resume(throwable)
                    } catch (e: Exception) {
                        BleLogger.e(e.message)
                    }
                } catch (e: Exception) {
                    BleLogger.e(e.message)
                }
            },
            callback = { _, throwable ->
                throwable?.let {
                    BleLogger.e(it.message)
                    if (it is TimeoutCancellationException || it is TimeoutCancelException) {
                        bleWriteData.isWriteFail.set(true)
                        val exception = TimeoutCancelException(
                            getTaskId(bleWriteData.writeUUID, bleWriteData.operateRandomID,
                                bleWriteData.currentPackage) + " -> " +
                                    "第${bleWriteData.currentPackage}包数据写失败，超时")
                        //移除监听
                        for ((key, value) in bleWriteDataHashMap) {
                            if (!bleWriteData.writeUUID.equals(key, ignoreCase = true)) {
                                continue
                            }
                            synchronized(value) {
                                val iterator = value.iterator()
                                while (iterator.hasNext()) {
                                    val writeData = iterator.next()
                                    if (writeData == bleWriteData) {
                                        iterator.remove()
                                    }
                                }
                            }
                        }
                        BleLogger.e(exception.message)
                        bleWriteData.bleWriteCallback.callWriteFail(
                            bleDevice,
                            bleWriteData.currentPackage,
                            bleWriteData.totalPackage,
                            exception
                        )
                        if (bleWriteData.currentPackage == bleWriteData.totalPackage) {
                            bleWriteData.bleWriteCallback.callWriteComplete(bleDevice, false)
                        }
                    }
                }
            }
        )
        getTaskQueue(bleWriteData.writeUUID)?.addTask(task)
    }

    private fun startWriteQueueJob(
        serviceUUID: String,
        writeUUID: String,
        operateRandomID: String,
        skipErrorPacketData: Boolean = false,
        retryWriteCount: Int = 0,
        retryDelayTime: Long = 0L,
        writeType: Int?,
        bleWriteCallback: BleWriteCallback
    ) {
        writeJobScope.launch {
            //写数据间隔，写太快会导致设备忙碌而失败率高
            delay(getOperateInterval())
            val currentWriteData = linkedBlockingQueue.peek()
            //如果这个数据包是空的，同时skipErrorPacketData为true，则跳过这个数据包，写下一个数据包
            if (currentWriteData != null && currentWriteData.isEmpty() && skipErrorPacketData) {
                linkedBlockingQueue.poll()
                if (linkedBlockingQueue.isNotEmpty() || getWriteDataFromTemp()) {
                    startWriteQueueJob(
                        serviceUUID,
                        writeUUID,
                        operateRandomID,
                        true,
                        retryWriteCount,
                        retryDelayTime,
                        writeType,
                        bleWriteCallback
                    )
                }
                return@launch
            }
            currentWriteData?.let { data ->
                writeData(
                    serviceUUID,
                    writeUUID,
                    operateRandomID,
                    SparseArray<ByteArray>(1).apply {
                        put(0, data)
                    },
                    writeType,
                    object : BleWriteCallback() {
                        override fun callWriteFail(
                            bleDevice: BleDevice,
                            current: Int,
                            total: Int,
                            throwable: Throwable
                        ) {
                            super.callWriteFail(bleDevice, current, total, throwable)
                            bleWriteCallback.callWriteFail(bleDevice, current, total, throwable)
                            launchInIOThread {
                                currentRetryWriteCount.set(currentRetryWriteCount.get() + 1)
                                val isCancelRetry = currentRetryWriteCount.get() > retryWriteCount
                                BleLogger.e("写失败，指定重试${retryWriteCount}次，是否进入下一次重试：${!isCancelRetry}")
                                if (isCancelRetry) {
                                    //如果重试了retryWriteCount次还是失败，则丢掉此包，写下一包
                                    currentRetryWriteCount.set(0)
                                    if (linkedBlockingQueue.isNotEmpty()) {
                                        linkedBlockingQueue.poll()
                                    }
                                }
//                                delay(200L + (max(currentRetryWriteCount.get(), 1) - 1)* 1000)
                                delay(retryDelayTime)
                                if (linkedBlockingQueue.isNotEmpty() || getWriteDataFromTemp()) {
                                    startWriteQueueJob(
                                        serviceUUID,
                                        writeUUID,
                                        operateRandomID,
                                        skipErrorPacketData,
                                        retryWriteCount,
                                        retryDelayTime,
                                        writeType,
                                        bleWriteCallback
                                    )
                                }
                            }
                        }

                        override fun callWriteSuccess(
                            bleDevice: BleDevice,
                            current: Int,
                            total: Int,
                            justWrite: ByteArray
                        ) {
                            super.callWriteSuccess(bleDevice, current, total, justWrite)
                            bleWriteCallback.callWriteSuccess(bleDevice, current, total, justWrite)
                            currentRetryWriteCount.set(0)
                            if (linkedBlockingQueue.isNotEmpty()) {
                                linkedBlockingQueue.poll()
                            }
                            if (linkedBlockingQueue.isNotEmpty() || getWriteDataFromTemp()) {
                                startWriteQueueJob(
                                    serviceUUID,
                                    writeUUID,
                                    operateRandomID,
                                    skipErrorPacketData,
                                    retryWriteCount,
                                    retryDelayTime,
                                    writeType,
                                    bleWriteCallback
                                )
                            }
                        }
                    }
                )
            }
        }
    }

    @SuppressLint("MissingPermission")
    @Suppress("DEPRECATION")
    private fun startWrite(
        characteristic: BluetoothGattCharacteristic,
        bleWriteData: BleWriteData
    ) {
        BleLogger.i(
            getTaskId(bleWriteData.writeUUID, bleWriteData.operateRandomID
                , bleWriteData.currentPackage) + " - > 开始写第${bleWriteData.currentPackage}包数据")
//        bleWriteData.bleWriteCallback.launchInIOThread {
//            delay(500)
//            onCharacteristicWrite(characteristic, BluetoothGatt.GATT_SUCCESS)
//        }
        //当支持WRITE_NO_RESPONSE和PROPERTY_WRITE时，用户可以指定写类型
        //否则如果支持WRITE_NO_RESPONSE，则使用WRITE_TYPE_NO_RESPONSE，否则使用WRITE_TYPE_DEFAULT
        val writeType =
            if (characteristic.properties and
                BluetoothGattCharacteristic.PROPERTY_WRITE != 0 &&
                characteristic.properties and
                BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0 &&
                bleWriteData.writeType != null) {
                bleWriteData.writeType!!
            } else if (characteristic.properties and
                BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0) {
                BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            } else {
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            }
        var errorCode: Int? = null
        val success: Boolean? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                errorCode = getBluetoothGatt(bleDevice)?.writeCharacteristic(
                    characteristic,
                    bleWriteData.data,
                    writeType
                )
                errorCode == 0
            } else {
                characteristic.writeType = writeType
                characteristic.value = bleWriteData.data
                getBluetoothGatt(bleDevice)?.writeCharacteristic(characteristic)
            }
        if (success != true) {
            //whether the characteristic was successfully written to Value is
            // BluetoothStatusCodes.SUCCESS = 0,
            // BluetoothStatusCodes.ERROR_MISSING_BLUETOOTH_CONNECT_PERMISSION = 6,
            // BluetoothStatusCodes.ERROR_DEVICE_NOT_CONNECTED = 4,
            // BluetoothStatusCodes.ERROR_PROFILE_SERVICE_NOT_BOUND = 8,
            // BluetoothStatusCodes.ERROR_GATT_WRITE_NOT_ALLOWED = 200,
            // BluetoothStatusCodes.ERROR_GATT_WRITE_REQUEST_BUSY = 201,
            // BluetoothStatusCodes.ERROR_UNKNOWN = 2147483647,
            // BluetoothStatusCodes.ERROR_NO_ACTIVE_DEVICES = 13,
            bleWriteData.isWriteFail.set(true)
            val taskId = getTaskId(bleWriteData.writeUUID, bleWriteData.operateRandomID
                , bleWriteData.currentPackage)
            val exception = UnDefinedException("$taskId -> 第${bleWriteData.currentPackage}包数据写" +
                    "失败，错误可能是没有权限、未连接、服务未绑定、不可写、请求忙碌等，code = $errorCode")
            BleLogger.e(exception.message)
            bleWriteData.bleWriteCallback.callWriteFail(
                bleDevice,
                bleWriteData.currentPackage,
                bleWriteData.totalPackage,
                exception
            )
            cancelSameWriteJob(bleWriteData)
        }
    }

    /**
     * 当向Characteristic写数据时会触发
     */
    fun onCharacteristicWrite(
        characteristic: BluetoothGattCharacteristic?,
        status: Int
    ) {
        for ((key, value) in bleWriteDataHashMap) {
            if (!characteristic?.uuid?.toString().equals(key, ignoreCase = true)) {
                continue
            }
            synchronized(value) {
                val iterator = value.iterator()
                while (iterator.hasNext()) {
                    val bleWriteData = iterator.next()
                    //只处理正在写并且没有失败的监听
                    if (!bleWriteData.isWriting.get() || bleWriteData.isWriteFail.get()) {
                        continue
                    }
                    val taskId = getTaskId(
                        bleWriteData.writeUUID,
                        bleWriteData.operateRandomID,
                        bleWriteData.currentPackage
                    )
                    bleWriteData.isWriting = AtomicBoolean(false)
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        BleLogger.i(
                            "$taskId -> 第${bleWriteData.currentPackage}包" +
                                    "数据写成功：" + BleUtil.bytesToHex(bleWriteData.data)
                        )
                        cancelWriteJob(bleWriteData.writeUUID, taskId)

                        bleWriteData.bleWriteCallback.callWriteSuccess(
                            bleDevice,
                            bleWriteData.currentPackage,
                            bleWriteData.totalPackage,
                            bleWriteData.data
                        )
                        if (bleWriteData.currentPackage == bleWriteData.totalPackage) {
                            bleWriteData.bleWriteCallback.callWriteComplete(bleDevice, true)
                            iterator.remove()
                        }
                    } else {
                        val exception = UnDefinedException(
                            "$taskId -> 第${bleWriteData.currentPackage}包数据写" +
                                    "失败，status = $status"
                        )
                        BleLogger.e(exception.message)
                        bleWriteData.bleWriteCallback.callWriteFail(
                            bleDevice,
                            bleWriteData.currentPackage,
                            bleWriteData.totalPackage,
                            exception
                        )
                        cancelSameWriteJob(bleWriteData)
                        if (bleWriteData.currentPackage == bleWriteData.totalPackage) {
                            iterator.remove()
                        }
                    }
                }
            }
        }
    }

    private fun getCharacteristic(serviceUUID: String, writeUUID: String): BluetoothGattCharacteristic? {
        val gattService = getBluetoothGatt(bleDevice)?.getService(UUID.fromString(serviceUUID))
        return gattService?.getCharacteristic(UUID.fromString(writeUUID))
    }

    private fun getTaskId(uuid: String?, operateRandomID: String, currentPackage: Int) =
        "$WRITE_TASK_ID：$uuid($operateRandomID)($currentPackage)"

    /**
     * 某个数据包写失败，后面的包不需再写
     */
    private fun cancelSameWriteJob(bleWriteData: BleWriteData) {
        val currentPackage = bleWriteData.currentPackage
        val totalPackage = bleWriteData.totalPackage
        if (currentPackage == totalPackage) {
            val taskId = getTaskId(bleWriteData.writeUUID, bleWriteData.operateRandomID
                , bleWriteData.currentPackage)
            cancelWriteJob(bleWriteData.writeUUID, taskId)
            bleWriteData.bleWriteCallback.callWriteComplete(bleDevice, false)
        } else {
            for (i in currentPackage..totalPackage) {
                val id = getTaskId(bleWriteData.writeUUID, bleWriteData.operateRandomID, i)
                cancelWriteJob(bleWriteData.writeUUID, id)
                if (i == totalPackage) {
                    bleWriteData.bleWriteCallback.callWriteComplete(bleDevice, false)
                }
            }
        }
    }

    /**
     * 取消写数据任务
     */
    private fun cancelWriteJob(writeUUID: String?, taskId: String) {
        getTaskQueue(writeUUID?: "")?.removeTask(taskId)
    }

    /**
     * 从临时队列中获取数据
     */
    private fun getWriteDataFromTemp(): Boolean {
        if (linkedBlockingQueue.isEmpty() && linkedBlockingTempQueue.isNotEmpty()) {
            linkedBlockingQueue.addAll(linkedBlockingTempQueue)
            linkedBlockingTempQueue.clear()
            return true
        }
        return false
    }

    override fun close() {
        super.close()
        removeAllWriteCallback()
        currentRetryWriteCount.set(0)
        addWriteJobScope.cancel()
        writeJobScope.cancel()
        linkedBlockingQueue.clear()
        linkedBlockingTempQueue.clear()
    }
}