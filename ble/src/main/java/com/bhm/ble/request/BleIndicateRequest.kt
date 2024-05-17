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
import android.os.Build
import com.bhm.ble.callback.BleIndicateCallback
import com.bhm.ble.data.*
import com.bhm.ble.data.Constants.EXCEPTION_CODE_DESCRIPTOR_FAIL
import com.bhm.ble.data.Constants.EXCEPTION_CODE_SET_CHARACTERISTIC_NOTIFICATION_FAIL
import com.bhm.ble.data.Constants.INDICATE_TASK_ID
import com.bhm.ble.data.Constants.UUID_CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR
import com.bhm.ble.device.BleDevice
import com.bhm.ble.request.base.BleTaskQueueRequest
import com.bhm.ble.log.BleLogger
import com.bhm.ble.utils.BleUtil
import kotlinx.coroutines.TimeoutCancellationException
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


/**
 * 设置Indicate请求
 *
 * @author Buhuiming
 * @date 2023年06月07日 15时35分
 */
internal class BleIndicateRequest(
    private val bleDevice: BleDevice,
) : BleTaskQueueRequest(bleDevice, "Indicate队列") {

    private val bleIndicateCallbackHashMap:
            ConcurrentHashMap<String, BleIndicateCallback> = ConcurrentHashMap()

    private fun addIndicateCallback(uuid: String, bleIndicateCallback: BleIndicateCallback) {
        bleIndicateCallbackHashMap[uuid] = bleIndicateCallback
    }

    fun removeIndicateCallback(uuid: String?) {
        if (bleIndicateCallbackHashMap.containsKey(uuid)) {
            bleIndicateCallbackHashMap.remove(uuid)
        }
    }

    fun removeAllIndicateCallback() {
        bleIndicateCallbackHashMap.clear()
    }

    /**
     * indicate
     */
    fun enableCharacteristicIndicate(serviceUUID: String,
                                     indicateUUID: String,
                                     bleDescriptorGetType: BleDescriptorGetType,
                                     bleIndicateCallback: BleIndicateCallback) {
        if (!BleUtil.isPermission(getBleManager().getContext())) {
            bleIndicateCallback.callIndicateFail(bleDevice, indicateUUID, NoBlePermissionException())
            return
        }
        val characteristic = getCharacteristic(bleDevice, serviceUUID, indicateUUID)
        if (characteristic != null &&
            (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0
        ) {
            bleIndicateCallback.setKey(indicateUUID)
            addIndicateCallback(indicateUUID, bleIndicateCallback)
            var mContinuation: Continuation<Throwable?>? = null
            val task = getTask(
                getTaskId(indicateUUID),
                block = {
                    suspendCoroutine<Throwable?> { continuation ->
                        mContinuation = continuation
                        setCharacteristicIndicate(
                            indicateUUID,
                            characteristic,
                            bleDescriptorGetType,
                            true,
                            bleIndicateCallback
                        )
                    }
                },
                interrupt = { _, throwable ->
                    try {
                        mContinuation?.resume(throwable)
                    } catch (e: Exception) {
                        BleLogger.e(e.message)
                    }
                },
                callback = { _, throwable ->
                    throwable?.let {
                        BleLogger.e(it.message)
                        if (it is TimeoutCancellationException || it is TimeoutCancelException) {
                            val exception = TimeoutCancelException("$indicateUUID -> 设置Indicate失败，设置超时")
                            BleLogger.e(exception.message)
                            bleIndicateCallback.callIndicateFail(bleDevice, indicateUUID, exception)
                        }
                    }
                }
            )
            getTaskQueue(indicateUUID)?.addTask(task)
        } else {
            val exception = UnSupportException("$indicateUUID -> 设置Indicate失败，此特性不支持通知")
            BleLogger.e(exception.message)
            bleIndicateCallback.callIndicateFail(bleDevice, indicateUUID, exception)
        }
    }

    /**
     * stop indicate
     */
    fun disableCharacteristicIndicate(serviceUUID: String,
                                      indicateUUID: String,
                                      bleDescriptorGetType: BleDescriptorGetType): Boolean {
        if (!BleUtil.isPermission(getBleManager().getContext())) {
            return false
        }
        val characteristic = getCharacteristic(bleDevice, serviceUUID, indicateUUID)
        if (characteristic != null &&
            (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0
        ) {
            cancelIndicateJob(indicateUUID, getTaskId(indicateUUID))
            val success = setCharacteristicIndicate(
                indicateUUID,
                characteristic,
                bleDescriptorGetType,
                false,
                null
            )
            if (success) {
                removeIndicateCallback(indicateUUID)
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
        BleLogger.d("${characteristic.uuid} -> " +
                "收到Indicate数据：${BleUtil.bytesToHex(value)}")
        bleIndicateCallbackHashMap.values.forEach {
            if (characteristic.uuid?.toString().equals(it.getKey(), ignoreCase = true)) {
                it.callCharacteristicChanged(bleDevice, it.getKey().toString(), value)
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
        bleIndicateCallbackHashMap.values.forEach {
            if (descriptor?.characteristic?.uuid.toString().equals(it.getKey(), ignoreCase = true)
                && cancelIndicateJob(it.getKey(), getTaskId(it.getKey()))) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    BleLogger.i("${it.getKey()} -> 设置Indicate成功")
                    it.callIndicateSuccess(bleDevice, it.getKey().toString())
                } else {
                    val exception = UnDefinedException(
                        "${it.getKey()} -> 设置Indicate失败，Descriptor写数据失败",
                        EXCEPTION_CODE_DESCRIPTOR_FAIL
                    )
                    BleLogger.e(exception.message)
                    it.callIndicateFail(bleDevice, it.getKey().toString(), exception)
                }
            }
        }
    }

    /**
     * 配置Indicate
     */
    @SuppressLint("MissingPermission")
    private fun setCharacteristicIndicate(indicateUUID: String,
                                          characteristic: BluetoothGattCharacteristic,
                                          bleDescriptorGetType: BleDescriptorGetType,
                                          enable: Boolean,
                                          bleIndicateCallback: BleIndicateCallback?): Boolean {
        val bluetoothGatt = getBluetoothGatt(bleDevice)
        val setSuccess = bluetoothGatt?.setCharacteristicNotification(characteristic, enable)
        if (setSuccess != true) {
            val exception = UnDefinedException(
                "$indicateUUID -> 设置Indicate失败，SetCharacteristicNotificationFail",
                EXCEPTION_CODE_SET_CHARACTERISTIC_NOTIFICATION_FAIL
            )
            cancelIndicateJob(indicateUUID, getTaskId(indicateUUID))
            BleLogger.e(exception.message)
            bleIndicateCallback?.callIndicateFail(bleDevice, indicateUUID, exception)
            return false
        }
        val descriptorList = characteristic.descriptors
        BleLogger.d("descriptor size is ${descriptorList.size}")
        if (bleDescriptorGetType == BleDescriptorGetType.AllDescriptor && descriptorList.isNotEmpty()) {
            var allFail = true
            for (descriptor in descriptorList) {
                descriptor?.let {
                    BleLogger.d("descriptor uuid is ${descriptor.uuid}")
                    val writeDescriptorCode = writeDescriptor(bluetoothGatt, descriptor, enable)
                    if (writeDescriptorCode == 0) {
                        allFail = false
                    }
                }
            }
            if (allFail) {
                val exception = UnDefinedException(
                    "$indicateUUID -> 设置Indicate失败，SetCharacteristicNotificationFail",
                    EXCEPTION_CODE_SET_CHARACTERISTIC_NOTIFICATION_FAIL
                )
                cancelIndicateJob(indicateUUID, getTaskId(indicateUUID))
                BleLogger.e(exception.message)
                bleIndicateCallback?.callIndicateFail(bleDevice, indicateUUID, exception)
                return false
            }
            return true
        } else {
            val descriptor = if (bleDescriptorGetType == BleDescriptorGetType.CharacteristicDescriptor) {
                characteristic.getDescriptor(characteristic.uuid)
            } else {
                characteristic.getDescriptor(UUID.fromString(UUID_CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR))
            }
            if (descriptor == null) {
                val exception = UnDefinedException(
                    "$indicateUUID -> 设置Indicate失败，SetCharacteristicNotificationFail",
                    EXCEPTION_CODE_SET_CHARACTERISTIC_NOTIFICATION_FAIL
                )
                cancelIndicateJob(indicateUUID, getTaskId(indicateUUID))
                BleLogger.e(exception.message)
                bleIndicateCallback?.callIndicateFail(bleDevice, indicateUUID, exception)
                return false
            }
            val writeDescriptorCode = writeDescriptor(bluetoothGatt, descriptor, enable)
            if (writeDescriptorCode != 0) {
                //true, if the write operation was initiated successfully Value is
                // BluetoothStatusCodes.SUCCESS = 0,
                // BluetoothStatusCodes.ERROR_MISSING_BLUETOOTH_CONNECT_PERMISSION = 6,
                // BluetoothStatusCodes.ERROR_DEVICE_NOT_CONNECTED = 4,
                // BluetoothStatusCodes.ERROR_PROFILE_SERVICE_NOT_BOUND = 8,
                // BluetoothStatusCodes.ERROR_GATT_WRITE_NOT_ALLOWED = 200,
                // BluetoothStatusCodes.ERROR_GATT_WRITE_REQUEST_BUSY = 201,
                // BluetoothStatusCodes.ERROR_UNKNOWN = 2147483647,
                // BluetoothStatusCodes.ERROR_NO_ACTIVE_DEVICES = 13,
                val exception = UnDefinedException(
                    "$indicateUUID -> -> 设置Indicate失败，错误可能是没有权限、" +
                            "未连接、服务未绑定、不可写、请求忙碌等，code = $writeDescriptorCode",
                    EXCEPTION_CODE_DESCRIPTOR_FAIL
                )
                cancelIndicateJob(indicateUUID, getTaskId(indicateUUID))
                BleLogger.e(exception.message)
                bleIndicateCallback?.callIndicateFail(bleDevice, indicateUUID, exception)
                return false
            }
        }
        return true
    }

    @SuppressLint("MissingPermission")
    private fun writeDescriptor(bluetoothGatt: BluetoothGatt,
                                descriptor: BluetoothGattDescriptor,
                                enable: Boolean): Int {
        val writeDescriptorCode: Int
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            writeDescriptorCode = if (enable) {
                bluetoothGatt.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_INDICATION_VALUE)
            } else {
                bluetoothGatt.writeDescriptor(descriptor, BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE)
            }
        } else {
            @Suppress("DEPRECATION")
            descriptor.value = if (enable) {
                BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
            } else {
                BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
            }
            @Suppress("DEPRECATION")
            val success = bluetoothGatt.writeDescriptor(descriptor)
            writeDescriptorCode = if (success) {
                0
            } else {
                -1
            }
        }
        return writeDescriptorCode
    }

    private fun getTaskId(uuid: String?) = INDICATE_TASK_ID + uuid

    /**
     * 取消设置indicate任务
     */
    private fun cancelIndicateJob(indicateUUID: String?, taskId: String): Boolean {
        return getTaskQueue(indicateUUID?: "")?.removeTask(taskId)?: false
    }

    override fun close() {
        super.close()
        removeAllIndicateCallback()
    }
}