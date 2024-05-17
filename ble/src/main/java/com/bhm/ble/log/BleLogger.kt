/*
 * Copyright (c) 2022-2032 buhuiming
 * 不能修改和删除上面的版权声明
 * 此代码属于buhuiming编写，在未经允许的情况下不得传播复制
 */
package com.bhm.ble.log

import android.util.Log
import com.bhm.ble.data.Constants.MARK


/**
 * 日志工具
 *
 * @author Buhuiming
 * @date 2023年05月19日 10时46分
 */
object BleLogger {

    var isLogger = true

    fun d(msg: String?) {
        if (msg.isNullOrEmpty()) {
            return
        }
        val tag = getClassNameForTag()
        if (isLogger) {
            Log.d(tag, MARK + msg)
        }
        notifyLog(BleLogLevel.Debug, tag, MARK + msg)
    }

    fun i(msg: String?) {
        if (msg.isNullOrEmpty()) {
            return
        }
        val tag = getClassNameForTag()
        if (isLogger) {
            Log.i(tag, MARK + msg)
        }
        notifyLog(BleLogLevel.Info, tag, MARK + msg)
    }

    fun e(msg: String?) {
        if (msg.isNullOrEmpty()) {
            return
        }
        val tag = getClassNameForTag()
        if (isLogger) {
            Log.e(tag, MARK + msg)
        }
        notifyLog(BleLogLevel.Error, tag, MARK + msg)
    }

    fun w(msg: String?) {
        if (msg.isNullOrEmpty()) {
            return
        }
        val tag = getClassNameForTag()
        if (isLogger) {
            Log.w(tag, MARK + msg)
        }
        notifyLog(BleLogLevel.Warn, tag, MARK + msg)
    }

    private fun notifyLog(logLevel: BleLogLevel, tag: String, message: String?) {
        BleLogManager.get().notifyLog(logLevel, tag, message)
    }

    /*
    * 获取调用方法的类名
    */
    private fun getClassNameForTag(): String {
        // 获取调用的堆栈信息
        val stackTraces = Throwable().stackTrace
        for (stackTrace in stackTraces) {
            // 获取类的全路径
            val className = stackTrace.className
            try {
                val clazz = Class.forName(className)
                if (this.javaClass != clazz) {
                    return clazz.simpleName.ifEmpty { BleLogger.javaClass.simpleName }
                }
            } catch (e: ClassNotFoundException) {
                e.printStackTrace()
            }
        }
        return BleLogger.javaClass.simpleName
    }
}