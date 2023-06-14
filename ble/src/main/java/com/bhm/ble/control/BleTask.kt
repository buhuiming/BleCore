/*
 * Copyright (c) 2022-2032 buhuiming
 * 不能修改和删除上面的版权声明
 * 此代码属于buhuiming编写，在未经允许的情况下不得传播复制
 */
package com.bhm.ble.control

import com.bhm.ble.data.CancelException
import com.bhm.ble.data.Constants.CANCEL_UN_COMPLETE
import com.bhm.ble.data.Constants.COMPLETED
import com.bhm.ble.data.Constants.UN_COMPLETE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger


/**
 * Ble任务，主要是用来处理rssi、mtu、write、read、notify、indicate操作
 * @param taskId 任务Id
 * @param durationTimeMillis： Long 任务执行的时间，0：表示任务执行时间不固定
 * @param operateInterval：间隔上个任务的时间
 * @param callInMainThread：指定在主线程执行，默认为false
 * @param autoDoNextTask = true自动执行下一个任务，= false，则需要调用doNextTask()执行下一个任务
 * @param canceled 是否已经取消
 * @param block：任务执行函数
 * @param interrupt：中断任务执行
 * @param callback：任务执行回调，成功throwable = CompleteException，超时throwable = TimeoutCancelException，
 * 任务中断抛异常 throwable = CancellationException
 * @author Buhuiming
 * @date 2023年06月02日 11时01分
 */
internal class BleTask(val taskId: String,
              val durationTimeMillis: Long = 0,
              val operateInterval: Long = 100,
              val callInMainThread: Boolean = false,
              val autoDoNextTask: Boolean = true,
              var canceled: AtomicBoolean = AtomicBoolean(false),
              private val block: suspend BleTask.() -> Unit,
              private val interrupt: ((task: BleTask, throwable: Throwable?) -> Unit)? = null,
              val callback: ((task: BleTask, throwable: Throwable?) -> Unit)? = null
) {

    private var completed = AtomicInteger(UN_COMPLETE)

    private var timingJob: Job? = null

    fun setCompleted(complete: Int) {
        if (complete == COMPLETED || complete == CANCEL_UN_COMPLETE) {
            timingJob?.cancel()
        }
        completed.set(complete)
    }

    fun completed() = completed.get()

    /**
     * 任务计时器，用于超时
     */
    fun setTimingJob(job: Job?) {
        timingJob = job
    }

    /**
     * 执行任务
     */
    suspend fun doTask() {
        if (callInMainThread) {
            withContext(SupervisorJob() + Dispatchers.Main) {
                block.invoke(this@BleTask)
            }
        } else {
            block.invoke(this@BleTask)
        }
    }

    /**
     * 中断任务
     */
    fun remove() {
        interrupt?.invoke(this@BleTask, CancelException("主动取消任务"))
    }
}