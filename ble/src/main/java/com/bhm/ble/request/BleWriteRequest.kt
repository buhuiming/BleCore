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
import android.util.SparseArray
import com.bhm.ble.callback.BleWriteCallback
import com.bhm.ble.control.BleTaskQueue
import com.bhm.ble.data.BleWriteData
import com.bhm.ble.data.Constants.DEFAULT_MTU
import com.bhm.ble.data.Constants.WRITE_TASK_ID
import com.bhm.ble.data.TimeoutCancelException
import com.bhm.ble.data.UnDefinedException
import com.bhm.ble.data.UnSupportException
import com.bhm.ble.device.BleDevice
import com.bhm.ble.request.base.Request
import com.bhm.ble.utils.BleLogger
import com.bhm.ble.utils.BleUtil
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
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
    private val bleTaskQueue: BleTaskQueue
) : Request() {

    private val bleWriteDataHashMap:
            ConcurrentHashMap<String, MutableList<BleWriteData>> = ConcurrentHashMap()

    @Synchronized
    private fun addBleWriteData(uuid: String, bleWriteData: BleWriteData) {
        if (bleWriteDataHashMap.containsKey(uuid) && bleWriteDataHashMap[uuid] != null) {
            bleWriteDataHashMap[uuid]?.add(bleWriteData)
        } else {
            val list = Collections.synchronizedList(LinkedList<BleWriteData>())
            list.add(bleWriteData)
            bleWriteDataHashMap[uuid] = list
        }
    }

    @Synchronized
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

    @Synchronized
    fun removeAllWriteCallback() {
        bleWriteDataHashMap.clear()
    }

    @Synchronized
    fun writeData(serviceUUID: String,
                  writeUUID: String,
                  operateRandomID: String,
                  dataArray: SparseArray<ByteArray>,
                  bleWriteCallback: BleWriteCallback) {
        if (dataArray.size() == 0) {
            val exception = UnDefinedException(
                getTaskId(writeUUID, operateRandomID, 0) + " -> 写数据失败，数据为空"
            )
            BleLogger.e(exception.message)
            bleWriteCallback.callWriteFail(0, 0, exception)
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
                bleWriteCallback.callWriteFail(i + 1, dataArray.size(), exception)
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
                bleWriteCallback.callWriteFail(i + 1, dataArray.size(),exception)
                return
            }
        }

        val characteristic = getCharacteristic(serviceUUID, writeUUID)

        if (characteristic != null &&
            (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_WRITE != 0 ||
                    characteristic.properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0)
        ) {
            bleWriteCallback.launchInDefaultThread {
                for (i in 0 until dataArray.size()) {
                    val bleWriteData = BleWriteData(
                        operateRandomID = operateRandomID,
                        serviceUUID = serviceUUID,
                        writeUUID = writeUUID,
                        currentPackage = i + 1,
                        totalPackage = dataArray.size(),
                        data = dataArray.valueAt(i),
                        isWriting = AtomicBoolean(false),
                        bleWriteCallback = bleWriteCallback
                    )
                    addBleWriteData(writeUUID, bleWriteData)
                    delay(getOperateInterval())
                    startWriteJob(
                        characteristic,
                        bleWriteData
                    )
                }
            }
        } else {
            val exception = UnSupportException("$writeUUID -> 写数据失败，此特性不支持写数据")
            BleLogger.e(exception.message)
            bleWriteCallback.callWriteFail(0, dataArray.size(), exception)
        }
    }

    @SuppressLint("MissingPermission")
    private fun startWriteJob(characteristic: BluetoothGattCharacteristic,
                              bleWriteData: BleWriteData) {
        var mContinuation: Continuation<Throwable?>? = null
        val task = getTask(
            getTaskId(bleWriteData.writeUUID, bleWriteData.operateRandomID, bleWriteData.currentPackage),
            block = {
                suspendCoroutine<Throwable?> { continuation ->
                    mContinuation = continuation
                    bleWriteData.isWriting = AtomicBoolean(true)
                    startWrite(characteristic, bleWriteData)
                }
            },
            interrupt = { _, throwable ->
                mContinuation?.resume(throwable)
            },
            callback = { _, throwable ->
                throwable?.let {
                    BleLogger.e(it.message)
                    if (it is TimeoutCancellationException || it is TimeoutCancelException) {
                        val exception = TimeoutCancelException(
                            getTaskId(bleWriteData.writeUUID, bleWriteData.operateRandomID,
                                bleWriteData.currentPackage) + " -> " +
                                    "第${bleWriteData.currentPackage}包数据写失败，超时")
                        BleLogger.e(exception.message)
                        bleWriteData.bleWriteCallback.callWriteFail(
                            bleWriteData.currentPackage,
                            bleWriteData.totalPackage,
                            exception
                        )
                        if (bleWriteData.currentPackage == bleWriteData.totalPackage) {
                            bleWriteData.bleWriteCallback.callWriteComplete(false)
                        }
                    }
                }
            }
        )
        bleTaskQueue.addTask(task)
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
        bleWriteData.bleWriteCallback.launchInIOThread {
            delay(500)
            onCharacteristicWrite(characteristic, BluetoothGatt.GATT_SUCCESS)
        }
        /*val success: Boolean? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                getBluetoothGatt(bleDevice)?.writeCharacteristic(
                    characteristic,
                    bleWriteData.data,
                    BluetoothGattCharacteristic.WRITE_TYPE_SIGNED
                ) == BluetoothStatusCodes.SUCCESS
            } else {
                characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_SIGNED
                characteristic.value = bleWriteData.data
                getBluetoothGatt(bleDevice)?.writeCharacteristic(characteristic)
            }
        if (success != true) {
            //whether the characteristic was successfully written to Value is
            // BluetoothStatusCodes.SUCCESS,
            // BluetoothStatusCodes.ERROR_MISSING_BLUETOOTH_CONNECT_PERMISSION,
            // android.bluetooth.BluetoothStatusCodes.ERROR_DEVICE_NOT_CONNECTED,
            // BluetoothStatusCodes.ERROR_PROFILE_SERVICE_NOT_BOUND,
            // BluetoothStatusCodes.ERROR_GATT_WRITE_NOT_ALLOWED,
            // BluetoothStatusCodes.ERROR_GATT_WRITE_REQUEST_BUSY,
            // BluetoothStatusCodes.ERROR_UNKNOWN
            val taskId = getTaskId(bleWriteData.writeUUID, bleWriteData.operateRandomID
            , bleWriteData.currentPackage)
            cancelWriteJob(taskId)
            val exception = UnDefinedException("$taskId -> 第${bleWriteData.currentPackage}包数据写" +
                    "失败，错误可能是没有权限、未连接、服务未绑定、不可写、请求忙碌等")
            BleLogger.e(exception.message)
            bleWriteData.bleWriteCallback.callWriteFail(exception)
        }*/
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
                    if (bleWriteData.isWriting.get()) {
                        val taskId = getTaskId(
                            bleWriteData.writeUUID,
                            bleWriteData.operateRandomID,
                            bleWriteData.currentPackage
                        )
                        bleWriteData.isWriting = AtomicBoolean(false)
                        if (status == BluetoothGatt.GATT_SUCCESS) {
                            BleLogger.d(
                                "$taskId -> 第${bleWriteData.currentPackage}包" +
                                        "数据写成功：" + BleUtil.bytesToHex(bleWriteData.data)
                            )
                            cancelWriteJob(taskId)
                            bleWriteData.bleWriteCallback.callWriteSuccess(
                                bleWriteData.currentPackage,
                                bleWriteData.totalPackage,
                                bleWriteData.data
                            )
                            if (bleWriteData.currentPackage == bleWriteData.totalPackage) {
                                bleWriteData.bleWriteCallback.callWriteComplete(true)
                                iterator.remove()
                            }
                        } else {
                            cancelWriteJob(taskId)

                            val exception = UnDefinedException(
                                "$taskId -> 第${bleWriteData.currentPackage}包数据写" +
                                        "失败，status = $status"
                            )
                            BleLogger.e(exception.message)
                            bleWriteData.bleWriteCallback.callWriteFail(
                                bleWriteData.currentPackage,
                                bleWriteData.totalPackage,
                                exception
                            )
                            if (bleWriteData.currentPackage == bleWriteData.totalPackage) {
                                bleWriteData.bleWriteCallback.callWriteComplete(false)
                                iterator.remove()
                            }
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
     * 取消写数据任务
     */
    @Synchronized
    private fun cancelWriteJob(taskId: String) {
        bleTaskQueue.removeTask(taskId)
    }
}