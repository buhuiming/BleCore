/*
 * Copyright (c) 2022-2032 buhuiming
 * 不能修改和删除上面的版权声明
 * 此代码属于buhuiming编写，在未经允许的情况下不得传播复制
 */
package com.bhm.ble.control

import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean


/**
 * Ble任务，主要是用来处理rssi、mtu、write、read、notify、indicate操作
 * durationTimeMillis Long 任务执行的时间，0：表示任务执行时间不固定
 * autoDoNextTask = true自动执行下一个任务，= false，则需要调用doNextTask()执行下一个任务
 * @author Buhuiming
 * @date 2023年06月02日 11时01分
 */
class BleTask(val durationTimeMillis: Long = 0,
              val callInMainThread: Boolean = true,
              val autoDoNextTask: Boolean = false,
              private val block: suspend BleTask.() -> Unit,
              private val interrupt: ((throwable: Throwable?) -> Unit)? = null
) {

    private var isCompleted = AtomicBoolean(false)

    private var timingJob: Job? = null

    fun setIsCompleted(completed: Boolean) {
        if (completed) {
            timingJob?.cancel()
        }
        isCompleted.set(completed)
    }

    fun isCompleted() = isCompleted.get()

    fun setTimingJob(job: Job) {
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
        interrupt?.invoke(CancellationException())
    }
}