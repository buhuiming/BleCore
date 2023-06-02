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
import kotlinx.coroutines.channels.toList
import kotlinx.coroutines.sync.Mutex
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

    private var mLock = Mutex()

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

    private suspend fun tryHandleTask(task: BleTask) {
        //防止有task抛出异常，用CoroutineExceptionHandler捕获异常之后父coroutine关闭了，之后的send的Task不执行了
        try {
            task.setMutexLock(mLock)
            mLock.lock()
            if (task.callInMainThread) {
                withContext(Dispatchers.Main) {
                    task.doTask()
                }
            } else {
                task.doTask()
            }
        } catch (e: Exception) {
            BleLogger.i("任务执行中断：$task")
            e.printStackTrace()
            task.doNextTask()
        }
    }

    /**
     * 开始任务
     * @param task ITask
     */
    fun sendTask(task: BleTask) {
        if (mCoroutineScope == null && mChannel == null) {
            initLoop()
        }
//        BleLogger.i("当前任务数量：${taskList.size}, 添加任务：$task")
//        taskList.add(task)
        mCoroutineScope?.launch {
//            if (taskList.size == 1) {
                mChannel?.send(task)
//            }
        }
    }

    /**
     * 关闭并释放资源
     */
    fun clear() {
        mChannel?.close()
        mChannel = null
        mCoroutineScope?.cancel()
        mCoroutineScope = null
        taskList.clear()
    }
}