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
import android.bluetooth.BluetoothStatusCodes
import android.os.Build
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
import kotlinx.coroutines.TimeoutCancellationException
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
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

    private val bluetoothGatt = getBluetoothGatt(bleDevice)

    private val bleWriteCallbackHashMap:
            ConcurrentHashMap<String, CopyOnWriteArrayList<BleWriteCallback>> = ConcurrentHashMap()
    private val bleWriteDataList:
            CopyOnWriteArrayList<BleWriteData> = CopyOnWriteArrayList()

    @Synchronized
    private fun addWriteCallback(uuid: String, bleWriteCallback: BleWriteCallback) {
        if (bleWriteCallbackHashMap.contains(uuid) && bleWriteCallbackHashMap[uuid] != null) {
            bleWriteCallbackHashMap[uuid]?.add(bleWriteCallback)
        } else {
            val list = CopyOnWriteArrayList<BleWriteCallback>()
            list.add(bleWriteCallback)
            bleWriteCallbackHashMap[uuid] = list
        }
    }

    @Synchronized
    fun removeWriteCallback(uuid: String?, bleWriteCallback: BleWriteCallback? = null) {
        if (bleWriteCallbackHashMap.containsKey(uuid)) {
            if (bleWriteCallback != null) {
                bleWriteCallbackHashMap[uuid]?.remove(bleWriteCallback)
            } else {
                bleWriteCallbackHashMap.remove(uuid)
            }
        }
    }

    @Synchronized
    fun removeAllWriteCallback() {
        bleWriteCallbackHashMap.clear()
    }

    @Synchronized
    private fun addWriteData(bleWriteData: BleWriteData) {
        bleWriteDataList.add(bleWriteData)
    }

    @Synchronized
    private fun removeWriteData(operateRandomID: String) {
        val it = bleWriteDataList.iterator()
        while (it.hasNext()) {
            if (operateRandomID.equals(it.next())){
                it.remove()
            }
        }
    }

    @Synchronized
    fun writeData(serviceUUID: String,
                  writeUUID: String,
                  operateRandomID: String,
                  dataArray: SparseArray<ByteArray>,
                  bleWriteCallback: BleWriteCallback) {
        if (dataArray.size() == 0) {
            val exception = UnDefinedException(
                getTaskId(writeUUID, operateRandomID) + " -> 写数据失败，数据为空"
            )
            BleLogger.e(exception.message)
            bleWriteCallback.callWriteFail(exception)
            return
        }
        for (i in 0..dataArray.size()) {
            val data = dataArray.get(i)
            if (data == null || data.isEmpty()) {
                val exception = UnDefinedException(
                    getTaskId(writeUUID, operateRandomID) + " -> 写数据失败，第${i + 1}个数据包为空"
                )
                BleLogger.e(exception.message)
                bleWriteCallback.callWriteFail(exception)
                return
            }
            val mtu = getBleOptions()?.mtu?: DEFAULT_MTU
            //mtu长度包含了ATT的opcode一个字节以及ATT的handle2个字节
            val maxWriteLength = mtu - 3
            if (data.size > maxWriteLength) {
                val exception = UnDefinedException("${getTaskId(writeUUID, operateRandomID)} -> " +
                        "写数据失败，第${i + 1}个数据包长度(${data.size}) + 3大于设定Mtu($mtu)")
                BleLogger.e(exception.message)
                bleWriteCallback.callWriteFail(exception)
                return
            }
        }

        val characteristic = getCharacteristic(serviceUUID, writeUUID)

        if (characteristic != null &&
            (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_WRITE != 0 ||
                    characteristic.properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0)
        ) {
            bleWriteCallback.setKey(writeUUID)
            bleWriteCallback.setServiceUUID(serviceUUID)
            bleWriteCallback.setSingleKey(operateRandomID)
            addWriteCallback(writeUUID, bleWriteCallback)
            for (i in 0..dataArray.size()) {

            }
            startWriteJob(dataArray.get(0), bleWriteCallback)
        } else {
            val exception = UnSupportException("$writeUUID -> 写数据失败，此特性不支持写数据")
            BleLogger.e(exception.message)
            bleWriteCallback.callWriteFail(exception)
        }
    }

    @SuppressLint("MissingPermission")
    private fun startWriteJob(data: ByteArray,
                              bleWriteCallback: BleWriteCallback) {
        var mContinuation: Continuation<Throwable?>? = null
        val task = getTask(
            WRITE_TASK_ID,
            block = {
                suspendCoroutine<Throwable?> { continuation ->
                    mContinuation = continuation
//                    startWrite()
                }
            },
            interrupt = { _, throwable ->
                mContinuation?.resume(throwable)
            },
            callback = { _, throwable ->
                throwable?.let {
                    BleLogger.e(it.message)
                    if (it is TimeoutCancellationException || it is TimeoutCancelException) {
                        val exception = TimeoutCancelException("${bleWriteCallback.getKey()}" +
                                " -> 写数据失败，超时")
                        BleLogger.e(exception.message)
                        bleWriteCallback.callWriteFail(exception)
                    }
                }
            }
        )
        bleTaskQueue.addTask(task)
    }

    @SuppressLint("MissingPermission")
    @Suppress("DEPRECATION")
    private fun startWrite(
        writeUUID: String,
        characteristic: BluetoothGattCharacteristic,
        data: ByteArray,
        bleWriteCallback: BleWriteCallback
    ) {
        val success: Boolean? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                bluetoothGatt?.writeCharacteristic(
                    characteristic,
                    data,
                    BluetoothGattCharacteristic.WRITE_TYPE_SIGNED
                ) == BluetoothStatusCodes.SUCCESS
            } else {
                characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_SIGNED
                characteristic.value = data
                bluetoothGatt?.writeCharacteristic(characteristic)
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
            cancelWriteJob()
            val exception = UnDefinedException("$writeUUID -> 写数据失败，错误可能是没有权限、" +
                    "未连接、服务未绑定、不可写、请求忙碌等")
            BleLogger.e(exception.message)
            bleWriteCallback.callWriteFail(exception)
            return
        }
    }

    /**
     * 当向Characteristic写数据时会触发
     */
    fun onCharacteristicWrite(
        characteristic: BluetoothGattCharacteristic?,
        status: Int
    ) {
        bleWriteCallbackHashMap.keys.forEach { writeUUID ->
//            if (characteristic?.uuid?.toString().equals(writeUUID, ignoreCase = true)) {
//                if (status == BluetoothGatt.GATT_SUCCESS) {
//                    if (BleLogger.isLogger) {
//                        BleLogger.d("${it.getKey()} -> " +
//                                "写数据成功：${BleUtil.bytesToHex(value)}")
//                    }
//                    it.callWriteSuccess()
//                } else {
//                    val exception = UnDefinedException("$writeUUID -> " +
//                            "写数据失败，status = $status")
//                    BleLogger.e(exception.message)
//                    it.callWriteFail(exception)
//                }
//            }
        }
    }

    private fun getCharacteristic(serviceUUID: String, writeUUID: String): BluetoothGattCharacteristic? {
        val gattService = bluetoothGatt?.getService(UUID.fromString(serviceUUID))
        return gattService?.getCharacteristic(UUID.fromString(writeUUID))
    }

    private fun getTaskId(uuid: String?, operateRandomID: String) =
        WRITE_TASK_ID + uuid + operateRandomID

    /**
     * 取消写数据任务
     */
    @Synchronized
    private fun cancelWriteJob() {
        bleTaskQueue.removeTask(taskId = WRITE_TASK_ID)
    }
}