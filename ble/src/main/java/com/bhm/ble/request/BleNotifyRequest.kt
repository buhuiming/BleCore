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
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothStatusCodes
import android.os.Build
import com.bhm.ble.callback.BleNotifyCallback
import com.bhm.ble.control.BleTask
import com.bhm.ble.control.BleTaskQueue
import com.bhm.ble.data.BleNotificationFailType
import com.bhm.ble.data.Constants
import com.bhm.ble.data.Constants.NOTIFY
import com.bhm.ble.data.Constants.NOTIFY_TASK_ID
import com.bhm.ble.data.Constants.UUID_CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR
import com.bhm.ble.data.TimeoutCancelException
import com.bhm.ble.device.BleDevice
import com.bhm.ble.utils.BleLogger
import kotlinx.coroutines.TimeoutCancellationException
import java.util.*
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


/**
 * 设置Notify请求
 *
 * @author Buhuiming
 * @date 2023年06月07日 15时35分
 */
internal class BleNotifyRequest(
    private val bleDevice: BleDevice,
    private val bleTaskQueue: BleTaskQueue
) : Request(){

    private val bleNotifyCallbackHashMap: HashMap<String, BleNotifyCallback> = HashMap()

    @Synchronized
    fun addNotifyCallback(uuid: String, bleNotifyCallback: BleNotifyCallback) {
        bleNotifyCallbackHashMap[uuid] = bleNotifyCallback
    }

    @Synchronized
    fun removeNotifyCallback(uuid: String?) {
        if (bleNotifyCallbackHashMap.containsKey(uuid)) {
            bleNotifyCallbackHashMap.remove(uuid)
        }
    }

    @Synchronized
    fun removeAllNotifyCallback() {
        bleNotifyCallbackHashMap.clear()
    }

    /**
     * notify
     */
    @Synchronized
    fun enableCharacteristicNotify(serviceUUID: String,
                                   notifyUUID: String,
                                   useCharacteristicDescriptor: Boolean,
                                   bleNotifyCallback: BleNotifyCallback) {
        val bluetoothGatt = getBleConnectedDevice(bleDevice)?.getBluetoothGatt()
        val gattService = bluetoothGatt?.getService(UUID.fromString(serviceUUID))
        val characteristic = gattService?.getCharacteristic(UUID.fromString(notifyUUID))
        if (bluetoothGatt != null && gattService != null && characteristic != null &&
            (characteristic.properties or BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0
        ) {
            bleNotifyCallback.setKey(notifyUUID)
            addNotifyCallback(notifyUUID, bleNotifyCallback)
//            var job: Job? = null
            var mContinuation: Continuation<Throwable?>? = null
            val task = BleTask (
                NOTIFY_TASK_ID,
                durationTimeMillis = getOperateTime(),
                callInMainThread = false,
                autoDoNextTask = true,
                block = {
//                    suspendCoroutine<Throwable?> { continuation ->
//                        job = CoroutineScope(Dispatchers.IO).launch {
//                            withTimeout(operateTime) {
////                            setCharacteristicNotify(characteristic, useCharacteristicDescriptor, true, bleNotifyCallback)
//                                delay(operateTime)
//                            }
//                        }
//                        job?.invokeOnCompletion {
//                            continuation.resume(it)
//                        }
//                    }
                    suspendCoroutine<Throwable?> { continuation ->
                        mContinuation = continuation
                        setCharacteristicNotify(characteristic, useCharacteristicDescriptor, true, bleNotifyCallback)
                    }
                },
                interrupt = { _, throwable ->
//                    job?.cancel(CancellationException(throwable?.message))
                    mContinuation?.resume(throwable)
                },
                callback = { _, throwable ->
                    throwable?.let {
                        BleLogger.e(it.message)
                        if (it is TimeoutCancellationException || it is TimeoutCancelException) {
                            BleLogger.e("设置Notify超时")
                            bleNotifyCallback.callNotifyFail(
                                BleNotificationFailType.TimeoutCancellationFailType(
                                    NOTIFY
                                ))
                        }
                    }
                }
            )
            bleTaskQueue.addTask(task)
        } else {
            BleLogger.e("设置Notify失败，此特性不支持通知")
            bleNotifyCallback.callNotifyFail(
                BleNotificationFailType.UnSupportNotifyFailType(
                    NOTIFY
                ))
        }
    }

    /**
     * stop notify
     */
    @Synchronized
    fun disableCharacteristicNotify(serviceUUID: String,
                                    notifyUUID: String,
                                    useCharacteristicDescriptor: Boolean): Boolean {
        val bluetoothGatt = getBleConnectedDevice(bleDevice)?.getBluetoothGatt()
        val gattService = bluetoothGatt?.getService(UUID.fromString(serviceUUID))
        val characteristic = gattService?.getCharacteristic(UUID.fromString(notifyUUID))
        if (bluetoothGatt != null && gattService != null && characteristic != null &&
            (characteristic.properties or BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0
        ) {
            cancelNotifyJob()
            val success = setCharacteristicNotify(characteristic, useCharacteristicDescriptor, false, null)
            if (success) {
                removeNotifyCallback(notifyUUID)
            }
            return success
        }
        return false
    }

    /**
     * 设备发出通知时会时会触发
     */
    fun onCharacteristicChanged(
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray
    ) {
        bleNotifyCallbackHashMap.values.forEach {
            if (characteristic.uuid?.toString().equals(it.getKey(), ignoreCase = true)) {
                BleLogger.d("收到Notify数据：${value}")
                it.callCharacteristicChanged(value)
            }
        }
    }

    /**
     * 当向设备Descriptor中写数据时会触发
     */
    fun onDescriptorWrite(
        descriptor: BluetoothGattDescriptor?,
        status: Int
    ) {
        bleNotifyCallbackHashMap.values.forEach {
            if (descriptor?.characteristic?.uuid?.toString().equals(it.getKey(), ignoreCase = true)) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    cancelNotifyJob()
                    BleLogger.d("设置Notify成功")
                    it.callNotifySuccess()
                } else {
                    val exception = BleNotificationFailType.DescriptorFailType(NOTIFY)
                    cancelNotifyJob()
                    BleLogger.e("设置Notify失败：${exception.message}")
                    it.callNotifyFail(exception)
                }
            }
        }
    }

    /**
     * 配置Notify
     */
    @SuppressLint("MissingPermission")
    private fun setCharacteristicNotify(characteristic: BluetoothGattCharacteristic,
                                        useCharacteristicDescriptor: Boolean,
                                        enable: Boolean,
                                        bleNotifyCallback: BleNotifyCallback?): Boolean {
        val bluetoothGatt = getBleConnectedDevice(bleDevice)?.getBluetoothGatt()
        val setSuccess = bluetoothGatt?.setCharacteristicNotification(characteristic, enable)
        if (setSuccess != true) {
            val exception = BleNotificationFailType.SetCharacteristicNotificationFailType(
                NOTIFY
            )
            cancelNotifyJob()
            BleLogger.e("设置Notify失败，SetCharacteristicNotificationFail")
            bleNotifyCallback?.callNotifyFail(exception)
            return false
        }
        val descriptor = if (useCharacteristicDescriptor) {
            characteristic.getDescriptor(characteristic.uuid)
        } else {
            characteristic.getDescriptor(UUID.fromString(UUID_CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR))
        }
        if (descriptor == null) {
            val exception = BleNotificationFailType.SetCharacteristicNotificationFailType(
                NOTIFY
            )
            cancelNotifyJob()
            BleLogger.e("设置Notify失败，SetCharacteristicNotificationFail")
            bleNotifyCallback?.callNotifyFail(exception)
            return false
        }
        val success: Boolean
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val writeDescriptor: Int = if (enable) {
                bluetoothGatt.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
            } else {
                bluetoothGatt.writeDescriptor(descriptor, BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE)
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
            val writeDescriptor: Boolean = bluetoothGatt.writeDescriptor(descriptor)
            success = writeDescriptor == true
        }
        if (!success) {
            val exception = BleNotificationFailType.DescriptorFailType(NOTIFY)
            cancelNotifyJob()
            BleLogger.e("设置Notify失败，Descriptor写数据失败")
            bleNotifyCallback?.callNotifyFail(exception)
            return false
        }
        return true
    }

    /**
     * 取消设置notify任务
     */
    private fun cancelNotifyJob() {
        bleTaskQueue.removeTask(taskId = NOTIFY_TASK_ID)
    }
}