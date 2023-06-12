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
import com.bhm.ble.callback.BleReadCallback
import com.bhm.ble.control.BleTaskQueue
import com.bhm.ble.data.Constants.READ_TASK_ID
import com.bhm.ble.data.TimeoutCancelException
import com.bhm.ble.data.UnDefinedException
import com.bhm.ble.data.UnSupportException
import com.bhm.ble.device.BleDevice
import com.bhm.ble.request.base.Request
import com.bhm.ble.utils.BleLogger
import com.bhm.ble.utils.BleUtil
import kotlinx.coroutines.TimeoutCancellationException
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


/**
 * 设备读请求
 *
 * @author Buhuiming
 * @date 2023年06月07日 15时58分
 */
internal class BleReadRequest(
    private val bleDevice: BleDevice,
    private val bleTaskQueue: BleTaskQueue
) : Request() {

    private val bleReadCallbackHashMap:
            ConcurrentHashMap<String, BleReadCallback> = ConcurrentHashMap()

    @Synchronized
    private fun addReadCallback(uuid: String, bleReadCallback: BleReadCallback) {
        bleReadCallbackHashMap[uuid] = bleReadCallback
    }

    @Synchronized
    fun removeReadCallback(uuid: String?) {
        if (bleReadCallbackHashMap.containsKey(uuid)) {
            bleReadCallbackHashMap.remove(uuid)
        }
    }

    @Synchronized
    fun removeAllReadCallback() {
        bleReadCallbackHashMap.clear()
    }

    /**
     * read
     */
    @SuppressLint("MissingPermission")
    @Synchronized
    fun readCharacteristic(serviceUUID: String,
                           readUUID: String,
                           bleReadCallback: BleReadCallback
    ) {
        val characteristic = getCharacteristic(bleDevice, serviceUUID, readUUID)
        if (characteristic != null &&
            (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_READ) != 0
        ) {
            cancelReadJob(getTaskId(readUUID))
            bleReadCallback.setKey(readUUID)
            addReadCallback(readUUID, bleReadCallback)
            var mContinuation: Continuation<Throwable?>? = null
            val task = getTask(
                getTaskId(readUUID),
                block = {
                    suspendCoroutine<Throwable?> { continuation ->
                        mContinuation = continuation
                        if (getBluetoothGatt(bleDevice)?.readCharacteristic(characteristic) == false) {
                            continuation.resume(UnDefinedException("Gatt读特征值数据失败"))
                        }
                    }
                },
                interrupt = { _, throwable ->
                    mContinuation?.resume(throwable)
                },
                callback = { _, throwable ->
                    throwable?.let {
                        BleLogger.e(it.message)
                        if (it is TimeoutCancellationException || it is TimeoutCancelException) {
                            val exception = TimeoutCancelException("$readUUID -> 读特征值数据失败，超时")
                            BleLogger.e(exception.message)
                            bleReadCallback.callReadFail(exception)
                        }
                    }
                }
            )
            bleTaskQueue.addTask(task)
        } else {
            val exception = UnSupportException("$readUUID -> 读特征值数据失败，此特性不支持读特征值数据")
            BleLogger.e(exception.message)
            bleReadCallback.callReadFail(exception)
        }
    }

    /**
     * 当读取设备数据时会触发
     */
    fun onCharacteristicRead(
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray,
        status: Int
    ) {
        bleReadCallbackHashMap.values.forEach {
            if (characteristic.uuid?.toString().equals(it.getKey(), ignoreCase = true) &&
                cancelReadJob(getTaskId(it.getKey()))) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    BleLogger.d(
                        "${it.getKey()} -> " +
                                "读特征值数据成功：${BleUtil.bytesToHex(value)}"
                    )
                    it.callReadSuccess(value)
                } else {
                    val exception = UnDefinedException(
                        "${it.getKey()} -> " +
                                "读特征值数据失败，status = $status"
                    )
                    BleLogger.e(exception.message)
                    it.callReadFail(exception)
                }
            }
        }
    }

    private fun getTaskId(uuid: String?) = READ_TASK_ID + uuid

    /**
     * 取消读特征值数据任务
     */
    @Synchronized
    private fun cancelReadJob(taskId: String): Boolean {
        return bleTaskQueue.removeTask(taskId)
    }
}