/*
 * Copyright (c) 2022-2032 buhuiming
 * 不能修改和删除上面的版权声明
 * 此代码属于buhuiming编写，在未经允许的情况下不得传播复制
 */
@file:Suppress("SENSELESS_COMPARISON")

package com.bhm.ble.control

import com.bhm.ble.utils.BleLogger
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList


/**
 * 请求队列
 *
 * @author Buhuiming
 * @date 2023年06月02日 08时32分
 */
class BleTaskQueue {

    companion object {

        private var instance: BleTaskQueue = BleTaskQueue()

        @Synchronized
        fun get(): BleTaskQueue {
            if (instance == null) {
                instance = BleTaskQueue()
            }
            return instance
        }
    }

    private var mChannel: Channel<BleTask>? = null

    private var mCoroutineScope: CoroutineScope? = null

    private val taskList: CopyOnWriteArrayList<BleTask> = CopyOnWriteArrayList()

    init {
        initLoop()
    }

    private fun initLoop() {
        mChannel = Channel()
        mCoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        mCoroutineScope?.launch {
            mChannel?.consumeEach {
                tryHandleTask(it)
            }
        }
    }

    /**
     * 从队列中取任务处理任务
     */
    private suspend fun tryHandleTask(task: BleTask) {
        //防止有task抛出异常，用CoroutineExceptionHandler捕获异常之后父coroutine关闭了，之后的send的Task不执行了
        try {
            BleLogger.i("开始执行任务：$task")
            task.doTask()
            task.setIsCompleted(true)
            taskList.remove(task)
            BleLogger.i("任务：${task}结束完毕，剩下${taskList.size}个任务")
            if (task.autoDoNextTask) {
                sendTask(taskList.firstOrNull())
            }
        } catch (e: Exception) {
            BleLogger.i("任务执行中断：$task，\r\n ${e.message}")
            task.setIsCompleted(true)
            taskList.remove(task)
            sendTask(taskList.firstOrNull())
        }
    }

    /**
     * 添加任务
     * @param task ITask
     */
    @Synchronized
    fun addTask(task: BleTask) {
        if (mCoroutineScope == null && mChannel == null) {
            initLoop()
        }
        BleLogger.i("当前任务数量：${taskList.size}, 添加任务：$task")
        task.setIsCompleted(false)
        taskList.add(task)
        taskForTiming(task)
        if (taskList.size == 1) {
            sendTask(task)
        }
    }

    /**
     * 任务的超时计时
     */
    private fun taskForTiming(task: BleTask) {
        if (task.durationTimeMillis <= 0) {
            return
        }
        val timingJob = CoroutineScope(Dispatchers.Default).launch {
            withTimeout(task.durationTimeMillis) {
                delay(task.durationTimeMillis)
            }
        }
        timingJob.invokeOnCompletion {
            if (it is TimeoutCancellationException && !task.isCompleted()) {
                BleLogger.e("任务超时，即刻移除：$task")
                removeTask(task)
            } else if (it is CancellationException){
                BleLogger.i("任务未超时：$task")
            }
        }
        task.setTimingJob(timingJob)
    }

    /**
     * 移除任务
     */
    @Synchronized
    fun removeTask(task: BleTask?) {
        if (taskList.contains(task)) {
            if (task == taskList.firstOrNull()) {
                //正在执行
                BleLogger.e("取消正在执行的任务：$this")
                task?.remove()
            } else {
                BleLogger.e("取消队列中的任务：${task}")
                taskList.remove(task)
            }
        }
    }

    @Synchronized
    fun removeRunningTask() {
        removeTask(taskList.firstOrNull())
    }

    /**
     * 发送执行任务
     */
    @Synchronized
    private fun sendTask(task: BleTask?) {
        if (task == null) {
            BleLogger.i("所有任务执行完毕")
            return
        }
        mCoroutineScope?.launch {
            mChannel?.send(task)
        }
    }

    /**
     * 关闭并释放资源
     */
    fun clear() {
        taskList.forEach {
            removeTask(it)
        }
        mChannel?.close()
        mChannel = null
        mCoroutineScope?.cancel()
        mCoroutineScope = null
        taskList.clear()
    }
}