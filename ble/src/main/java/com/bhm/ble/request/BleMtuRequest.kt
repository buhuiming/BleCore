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
import com.bhm.ble.control.BleTaskQueue
import com.bhm.ble.data.Constants.SET_MTU_TASK_ID
import com.bhm.ble.data.NoBlePermissionException
import com.bhm.ble.data.TimeoutCancelException
import com.bhm.ble.data.UnDefinedException
import com.bhm.ble.device.BleDevice
import com.bhm.ble.request.base.Request
import com.bhm.ble.log.BleLogger
import com.bhm.ble.utils.BleUtil
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
internal class BleMtuRequest(
    private val bleDevice: BleDevice,
    private val bleTaskQueue: BleTaskQueue
) : Request() {

    private var bleMtuChangedCallback: BleMtuChangedCallback? = null

    private fun addMtuChangedCallback(callback: BleMtuChangedCallback) {
        bleMtuChangedCallback = callback
    }

    fun removeMtuChangedCallback() {
        bleMtuChangedCallback = null
    }

    /**
     * 设置mtu
     */
    @SuppressLint("MissingPermission")
    fun setMtu(mtu: Int, bleMtuChangedCallback: BleMtuChangedCallback) {
        if (!BleUtil.isPermission(getBleManager().getContext())) {
            bleMtuChangedCallback.callSetMtuFail(bleDevice, NoBlePermissionException())
            return
        }
        cancelSetMtuJob()
        addMtuChangedCallback(bleMtuChangedCallback)
        var mContinuation: Continuation<Throwable?>? = null
        val task = getTask(
            getTaskId(),
            block = {
                suspendCoroutine<Throwable?> { continuation ->
                    mContinuation = continuation
                    if (getBluetoothGatt(bleDevice)?.requestMtu(mtu) == false) {
                        try {
                            continuation.resume(UnDefinedException("Gatt设置mtu失败"))
                        } catch (e: Exception) {
                            BleLogger.e(e.message)
                        }
                    }
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
                        val exception = TimeoutCancelException("${bleDevice.deviceAddress} -> 设置Mtu超时")
                        BleLogger.e(exception.message)
                        bleMtuChangedCallback.callSetMtuFail(bleDevice, exception)
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
        bleMtuChangedCallback?.let {
            if (cancelSetMtuJob()) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    BleLogger.d("${bleDevice.deviceAddress} -> 设置Mtu成功：$mtu")
                    it.callMtuChanged(bleDevice, mtu)
                    getBleOptions()?.mtu = mtu
                } else {
                    val exception = UnDefinedException(
                        "${bleDevice.deviceAddress} -> " +
                                "设置Mtu失败，status = $status"
                    )
                    BleLogger.e(exception.message)
                    it.callSetMtuFail(bleDevice, exception)
                }
            }
        }
    }

    private fun getTaskId() = SET_MTU_TASK_ID + bleDevice.deviceAddress

    /**
     * 取消设置Mtu任务
     */
    private fun cancelSetMtuJob(): Boolean {
        return bleTaskQueue.removeTask(getTaskId())
    }
}