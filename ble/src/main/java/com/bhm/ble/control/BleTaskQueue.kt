/*
 * Copyright (c) 2022-2032 buhuiming
 * 不能修改和删除上面的版权声明
 * 此代码属于buhuiming编写，在未经允许的情况下不得传播复制
 */

package com.bhm.ble.control

import com.bhm.ble.data.CancelException
import com.bhm.ble.data.CompleteException
import com.bhm.ble.data.Constants.CANCEL_UN_COMPLETE
import com.bhm.ble.data.Constants.COMPLETED
import com.bhm.ble.data.Constants.UN_COMPLETE
import com.bhm.ble.data.TimeoutCancelException
import com.bhm.ble.log.BleLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.withTimeout
import java.util.concurrent.ExecutorService


/**
 * 请求队列
 *
 * @author Buhuiming
 * @date 2023年06月02日 08时32分
 */
internal class BleTaskQueue(private val tag: String = "") {

    private var mCoroutineScope: CoroutineScope? = null

    /*
    * 只有一个线程的线程池，不使用CoroutineScope(Dispatchers.IO)，是因为不同设备的请求可能
    * 使用相同的线程，从而导致一个设备的请求被阻塞，影响其他设备的请求。
    */
    @OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
    private val threadContext = newSingleThreadContext(tag)

    private val taskList = BleTaskList()

    init {
        initLoop()
    }

    private fun initLoop() {
        mCoroutineScope = CoroutineScope(threadContext)
    }

    /**
     * 从队列中取任务处理任务
     */
    private suspend fun tryHandleTask(task: BleTask) {
        //防止有task抛出异常，用CoroutineExceptionHandler捕获异常之后父coroutine关闭了，之后的send的Task不执行了
        try {
            BleLogger.i("($tag) 开始执行任务：$task")
            task.doTask()
            if (task.completed() == UN_COMPLETE) {
                task.setCompleted(COMPLETED)
                task.callback?.invoke(task, CompleteException())
            } else if (task.completed() == CANCEL_UN_COMPLETE) {
                task.callback?.invoke(task, CancelException())
            }
            task.canceled.set(true)
            taskList.remove(task)
            BleLogger.d("($tag) 任务：${task}结束完毕，剩下${taskList.size()}个任务")
            if (task.autoDoNextTask) {
                sendTask(taskList.firstOrNull())
            }
        } catch (e: Exception) {
            BleLogger.i("($tag) 任务执行中断：$task，\r\n ${e.message}")
            task.setCompleted(CANCEL_UN_COMPLETE)
            task.callback?.invoke(task, CancellationException(e.message))
            task.canceled.set(true)
            taskList.remove(task)
            if (task.autoDoNextTask) {
                sendTask(taskList.firstOrNull())
            }
        }
    }

    /**
     * 添加任务
     * @param task ITask
     */
    @Synchronized
    fun addTask(task: BleTask) {
        if (mCoroutineScope == null) {
            initLoop()
        }
        BleLogger.d("($tag) 当前任务数量：${taskList.size()}, 添加任务：$task")
        task.setCompleted(UN_COMPLETE)
        taskList.add(task)
        taskForTiming(task)
        if (taskList.size() == 1) {
            sendTask(task)
        }
    }

    fun getTaskList() = taskList

    /**
     * 任务的超时计时
     */
    private fun taskForTiming(task: BleTask) {
        if (task.durationTimeMillis <= 0) {
            return
        }
        val context = if (task.callInMainThread) {
            SupervisorJob() + Dispatchers.Main
        } else {
            SupervisorJob() + Dispatchers.IO
        }
        val timingJob = CoroutineScope(context).launch {
            withTimeout(task.durationTimeMillis) {
                delay(task.durationTimeMillis)
            }
        }
        timingJob.invokeOnCompletion {
            if (it is TimeoutCancellationException &&
                task.completed() == UN_COMPLETE) {
                task.canceled.set(true)
                removeTask(task)
                task.callback?.invoke(task, TimeoutCancelException())
            } else if (it is CancellationException) {
                task.canceled.set(true)
                BleLogger.i("($tag) 任务完成，未超时：$task")
            }
        }
        task.setTimingJob(timingJob)
    }

    /**
     * 移除任务
     */
    @Synchronized
    private fun removeTask(task: BleTask?) {
        if (taskList.contains(task)) {
            task?.setCompleted(CANCEL_UN_COMPLETE)
            if (task == taskList.firstOrNull()) {
                //正在执行
                BleLogger.e("($tag) 任务正在执行，但超时，移除任务：$task")
                task?.remove()
                taskList.remove(task)
            } else {
                BleLogger.e("($tag) 任务在队列未执行，但超时，移除任务：${task}")
                taskList.remove(task)
            }
        }
    }

//    fun doNextTask() {
//        val task = taskList.firstOrNull()
//        if (task?.completed() == UN_COMPLETE || task?.completed() == CANCEL_UN_COMPLETE) {
//            doNextTask()
//            return
//        }
//        sendTask(task)
//    }

    /**
     * 移除任务
     */
    @Synchronized
    fun removeTask(taskId: String): Boolean {
        if (!taskList.containsTaskId(taskId)) {
            return false
        }
        var success = false
        synchronized(taskList.list()) {
            val iterator = taskList.iterator()
            while (iterator.hasNext()) {
                val task = iterator.next()
                if (task.taskId == taskId) {
                    task.canceled.set(true)
                    task.setCompleted(CANCEL_UN_COMPLETE)
                    success = true
                    if (task == taskList.firstOrNull()) {
                        //正在执行
                        BleLogger.e("($tag) 移除正在执行的任务：$task")
                        task.remove()
                    } else {
                        BleLogger.e("($tag) 移除队列中的任务：${task}")
                        iterator.remove()
                    }
                }
            }
        }
        return success
    }

    /**
     * 发送执行任务
     */
    @Synchronized
    private fun sendTask(task: BleTask?) {
        if (task == null) {
            BleLogger.i("($tag) 所有任务执行完毕")
            return
        }
        mCoroutineScope?.launch {
            //两次操作之间最好间隔一小段时间，如100ms（具体时间可以根据自己实际蓝牙外设自行尝试延长或缩短）
            delay(task.operateInterval)
            //任务还在队列中并且未取消才执行
            if (taskList.contains(task) && !task.canceled.get()) {
                tryHandleTask(task)
            }
        }
    }

    /**
     * 关闭并释放资源
     */
    fun clear() {
        synchronized(taskList.list()) {
            val iterator = taskList.iterator()
            while (iterator.hasNext()) {
                val task = iterator.next()
                task.setTimingJob(null)
                task.setCompleted(CANCEL_UN_COMPLETE)
                if (task == taskList.firstOrNull()) {
                    task.remove()
                } else {
                    iterator.remove()
                }
            }
        }
        taskList.clear()
        mCoroutineScope?.cancel()
        mCoroutineScope = null
        (threadContext.executor as ExecutorService).shutdown()
    }
}