/*
 * Copyright (c) 2022-2032 buhuiming
 * 不能修改和删除上面的版权声明
 * 此代码属于buhuiming编写，在未经允许的情况下不得传播复制
 */
package com.bhm.ble.control

import com.bhm.ble.utils.BleLogger
import kotlinx.coroutines.sync.Mutex


/**
 * Ble任务，主要是用来处理rssi、mtu、write、read、notify、indicate操作
 * autoDoNextTask = true自动执行下一个任务，= false，则需要调用doNextTask()执行下一个任务
 * @author Buhuiming
 * @date 2023年06月02日 11时01分
 */
class BleTask(val callInMainThread: Boolean = true,
              private val autoDoNextTask: Boolean = false,
              private val block: suspend BleTask.() -> Unit
) {

    /**
     * 用于执行task时，挂起当前coroutine
     */
    private var mutex: Mutex? = null

    fun setMutexLock(mutex: Mutex){
        this.mutex = mutex
    }

    /**
     * 执行任务
     */
    suspend fun doTask() {
        BleLogger.i("开始执行任务：$this")
        block.invoke(this)
        BleLogger.i("任务结束完毕：$this")
        if (autoDoNextTask) {
            doNextTask()
        }
    }

    /**
     * 恢复当前coroutine，执行下一个task
     */
    fun doNextTask() {
        if (mutex?.isLocked == true) {
            mutex?.unlock()
        }
        mutex = null
    }
}