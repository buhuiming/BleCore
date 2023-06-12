/*
 * Copyright (c) 2022-2032 buhuiming
 * 不能修改和删除上面的版权声明
 * 此代码属于buhuiming编写，在未经允许的情况下不得传播复制
 */
@file:Suppress("RemoveExplicitTypeArguments")

package com.bhm.ble.request

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGatt
import com.bhm.ble.callback.BleRssiCallback
import com.bhm.ble.control.BleTaskQueue
import com.bhm.ble.data.CancelException
import com.bhm.ble.data.Constants.SET_RSSI_TASK_ID
import com.bhm.ble.data.TimeoutCancelException
import com.bhm.ble.data.UnDefinedException
import com.bhm.ble.device.BleDevice
import com.bhm.ble.request.base.Request
import com.bhm.ble.utils.BleLogger
import kotlinx.coroutines.TimeoutCancellationException
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


/**
 * 读取设备Rssi请求
 *
 * @author Buhuiming
 * @date 2023年06月07日 11时05分
 */
internal class BleRssiRequest(
    private val bleDevice: BleDevice,
    private val bleTaskQueue: BleTaskQueue
) : Request() {

    private var bleRssiCallback: BleRssiCallback? = null

    @Synchronized
    fun addRssiCallback(callback: BleRssiCallback) {
        bleRssiCallback = callback
    }

    @Synchronized
    fun removeRssiCallback() {
        bleRssiCallback = null
    }

    /**
     * 读取信号值
     */
    @SuppressLint("MissingPermission")
    @Synchronized
    fun readRemoteRssi(bleRssiCallback: BleRssiCallback) {
        cancelReadRssiJob()
        addRssiCallback(bleRssiCallback)
        var mContinuation: Continuation<Throwable?>? = null
        val task = getTask(
            getTaskId(),
            block = {
                suspendCoroutine<Throwable?> { continuation ->
                    mContinuation = continuation
                    if (getBluetoothGatt(bleDevice)?.readRemoteRssi() == false) {
                        continuation.resume(UnDefinedException("Gatt读取Rssi失败"))
                    }
                }
            },
            interrupt = { _, throwable ->
                if (throwable is CancelException) {
                    bleRssiCallback.callRssiFail(throwable)
                }
                mContinuation?.resume(throwable)
            },
            callback = { _, throwable ->
                throwable?.let {
                    BleLogger.e(it.message)
                    if (it is TimeoutCancellationException || it is TimeoutCancelException) {
                        BleLogger.e("${bleDevice.deviceAddress} -> 读取Rssi超时")
                        bleRssiCallback.callRssiFail(
                            TimeoutCancelException("${bleDevice.deviceAddress}" +
                                    " -> 读取Rssi失败，超时")
                        )
                    }
                }
            }
        )
        bleTaskQueue.addTask(task)
    }

    /**
     * 读取信号值后会触发
     */
    fun onReadRemoteRssi(rssi: Int, status: Int) {
        bleRssiCallback?.let {
            if (cancelReadRssiJob()) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    BleLogger.d("${bleDevice.deviceAddress} -> 读取Rssi成功：$rssi")
                    it.callRssiSuccess(rssi)
                } else {
                    val exception = UnDefinedException(
                        "${bleDevice.deviceAddress} -> " +
                                "读取Rssi失败，status = $status"
                    )
                    BleLogger.e(exception.message)
                    it.callRssiFail(exception)
                }
            }
        }
    }

    private fun getTaskId() = SET_RSSI_TASK_ID + bleDevice.deviceAddress

    /**
     * 取消读取Rssi任务
     */
    @Synchronized
    private fun cancelReadRssiJob(): Boolean {
        return bleTaskQueue.removeTask(getTaskId())
    }
}