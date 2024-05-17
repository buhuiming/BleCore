/*
 * Copyright (c) 2022-2032 buhuiming
 * 不能修改和删除上面的版权声明
 * 此代码属于buhuiming编写，在未经允许的情况下不得传播复制
 */
@file:Suppress("SENSELESS_COMPARISON")

package com.bhm.ble.log

import java.util.concurrent.ConcurrentLinkedQueue

/**
 * @description 日志管理器
 * @author Buhuiming
 * @date 2024/05/08/ 10:30
 */
class BleLogManager private constructor() {

    companion object {

        private var instance: BleLogManager = BleLogManager()

        private val listenerList: ConcurrentLinkedQueue<BleLogEvent> = ConcurrentLinkedQueue()

        @Synchronized
        fun get(): BleLogManager {
            if (instance == null) {
                instance = BleLogManager()
            }
            return instance
        }
    }

    fun addLogListener(listener: BleLogEvent) {
        listenerList.add(listener)
    }

    fun removeLogListener(listener: BleLogEvent) {
        listenerList.remove(listener)
    }

    fun notifyLog(level: BleLogLevel, tag: String, message: String?) {
        for (listener in listenerList) {
            listener.onLog(level, tag, message)
        }
    }
}