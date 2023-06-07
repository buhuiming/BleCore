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
import com.bhm.ble.control.BleTask
import com.bhm.ble.control.BleTaskQueue
import com.bhm.ble.data.Constants.READ_TASK_ID
import com.bhm.ble.data.TimeoutCancelException
import com.bhm.ble.data.UnSupportException
import com.bhm.ble.device.BleDevice
import com.bhm.ble.utils.BleLogger
import com.bhm.ble.utils.BleUtil
import kotlinx.coroutines.TimeoutCancellationException
import java.util.*
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
) : Request(){

    private val bleReadCallbackHashMap: HashMap<String, BleReadCallback> = HashMap()

    @Synchronized
    fun addReadCallback(uuid: String, bleReadCallback: BleReadCallback) {
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
        val bluetoothGatt = getBleConnectedDevice(bleDevice)?.getBluetoothGatt()
        val gattService = bluetoothGatt?.getService(UUID.fromString(serviceUUID))
        val characteristic = gattService?.getCharacteristic(UUID.fromString(readUUID))
        if (bluetoothGatt != null && gattService != null && characteristic != null &&
            (characteristic.properties or BluetoothGattCharacteristic.PROPERTY_READ) > 0
        ) {
            cancelReadJob()
            bleReadCallback.setKey(readUUID)
            addReadCallback(readUUID, bleReadCallback)
            var mContinuation: Continuation<Throwable?>? = null
            val task = BleTask (
                READ_TASK_ID,
                durationTimeMillis = getOperateTime(),
                callInMainThread = false,
                autoDoNextTask = true,
                block = {
                    suspendCoroutine<Throwable?> { continuation ->
                        mContinuation = continuation
                        if (getBleConnectedDevice(bleDevice)?.getBluetoothGatt()?.readCharacteristic(characteristic) == false) {
                            continuation.resume(Throwable("Gatt读特征值数据失败"))
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
                            BleLogger.e("${bleDevice.deviceAddress} -> 读特征值数据超时")
                            bleReadCallback.callReadFail(TimeoutCancelException("${bleDevice.deviceAddress} -> 读特征值数据失败，超时"))
                        }
                    }
                }
            )
            bleTaskQueue.addTask(task)
        } else {
            val exception = UnSupportException("${bleDevice.deviceAddress} -> 读特征值数据失败，此特性不支持读特征值数据")
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
        cancelReadJob()
        bleReadCallbackHashMap.values.forEach {
            if (characteristic.uuid?.toString().equals(it.getKey(), ignoreCase = true)) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    if (BleLogger.isLogger) {
                        BleLogger.d("${bleDevice.deviceAddress} -> 读特征值数据成功：${BleUtil.bytesToHex(value)}")
                    }
                    it.callReadSuccess(value)
                } else {
                    val throwable = Throwable("${bleDevice.deviceAddress} -> 读特征值数据失败，status = $status")
                    BleLogger.e(throwable.message)
                    it.callReadFail(throwable)
                }
            }
        }
    }

    /**
     * 取消设置Mtu任务
     */
    @Synchronized
    private fun cancelReadJob() {
        bleTaskQueue.removeTask(taskId = READ_TASK_ID)
    }
}