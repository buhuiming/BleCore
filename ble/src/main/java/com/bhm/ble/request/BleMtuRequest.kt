/*
 * Copyright (c) 2022-2032 buhuiming
 * 不能修改和删除上面的版权声明
 * 此代码属于buhuiming编写，在未经允许的情况下不得传播复制
 */
@file:Suppress("RemoveExplicitTypeArguments")

package com.bhm.ble.request

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGatt
import com.bhm.ble.callback.BleMtuChangedCallback
import com.bhm.ble.control.BleTask
import com.bhm.ble.control.BleTaskQueue
import com.bhm.ble.data.Constants.SET_MTU_TASK_ID
import com.bhm.ble.data.TimeoutCancelException
import com.bhm.ble.device.BleDevice
import com.bhm.ble.utils.BleLogger
import kotlinx.coroutines.TimeoutCancellationException
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


/**
 * 设置Mtu请求
 *
 * @author Buhuiming
 * @date 2023年06月07日 15时25分
 */
internal class BleMtuRequest(private val bleDevice: BleDevice,
                             private val bleTaskQueue: BleTaskQueue
) : Request(){

    private var bleMtuChangedCallback: BleMtuChangedCallback? = null

    @Synchronized
    fun addMtuChangedCallback(callback: BleMtuChangedCallback) {
        bleMtuChangedCallback = callback
    }

    @Synchronized
    fun removeMtuChangedCallback() {
        bleMtuChangedCallback = null
    }

    /**
     * 设置mtu
     */
    @SuppressLint("MissingPermission")
    @Synchronized
    fun setMtu(mtu: Int, bleMtuChangedCallback: BleMtuChangedCallback) {
        cancelSetMtuJob()
        addMtuChangedCallback(bleMtuChangedCallback)
        var mContinuation: Continuation<Throwable?>? = null
        val task = BleTask (
            SET_MTU_TASK_ID,
            durationTimeMillis = getOperateTime(),
            callInMainThread = false,
            autoDoNextTask = true,
            block = {
                suspendCoroutine<Throwable?> { continuation ->
                    mContinuation = continuation
                    if (getBleConnectedDevice(bleDevice)?.getBluetoothGatt()?.requestMtu(mtu) == false) {
                        continuation.resume(Throwable("Gatt设置mtu失败"))
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
                        BleLogger.e("设置Mtu超时")
                        bleMtuChangedCallback.callSetMtuFail(TimeoutCancelException("设置mtu失败，超时"))
                    }
                }
            }
        )
        bleTaskQueue.addTask(task)
    }

    /**
     * 设置Mtu值后会触发
     */
    fun onMtuChanged(mtu: Int, status: Int) {
        cancelSetMtuJob()
        bleMtuChangedCallback?.let {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                BleLogger.d("设置Mtu成功：$mtu")
                it.callMtuChanged(mtu)
            } else {
                val throwable = Throwable("设置Mtu失败，status = $status")
                BleLogger.e(throwable.message)
                it.callSetMtuFail(throwable)
            }
        }
    }

    /**
     * 取消设置Mtu任务
     */
    @Synchronized
    private fun cancelSetMtuJob() {
        bleTaskQueue.removeTask(taskId = SET_MTU_TASK_ID)
    }
}