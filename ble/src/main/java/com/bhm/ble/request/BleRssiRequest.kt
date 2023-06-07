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
import com.bhm.ble.control.BleTask
import com.bhm.ble.control.BleTaskQueue
import com.bhm.ble.data.TimeoutCancelException
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
internal class BleRssiRequest(private val bluetoothGatt: BluetoothGatt?,
                              private val bleTaskQueue: BleTaskQueue
) : Request(){

    init {

    }

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
        val task = BleTask (
            BleConnectRequestManager.SET_RSSI_TASK_ID,
            durationTimeMillis = getOperateTime(),
            callInMainThread = false,
            autoDoNextTask = true,
            block = {
                suspendCoroutine<Throwable?> { continuation ->
                    mContinuation = continuation
                    if (bluetoothGatt?.readRemoteRssi() == false) {
                        continuation.resume(Throwable("Gatt读取Rssi失败"))
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
                        BleLogger.e("读取Rssi超时")
                        bleRssiCallback.callRssiFail(TimeoutCancelException("读取Rssi失败，超时"))
                    }
                }
            }
        )
        bleTaskQueue.addTask(task)
    }

    /**
     * 取消读取Rssi任务
     */
    private fun cancelReadRssiJob() {
        bleTaskQueue.removeTask(taskId = BleConnectRequestManager.SET_RSSI_TASK_ID)
    }
}