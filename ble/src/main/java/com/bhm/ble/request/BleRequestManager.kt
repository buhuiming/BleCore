/*
 * Copyright (c) 2022-2032 buhuiming
 * 不能修改和删除上面的版权声明
 * 此代码属于buhuiming编写，在未经允许的情况下不得传播复制
 */
@file:Suppress("SENSELESS_COMPARISON", "UNCHECKED_CAST")

package com.bhm.ble.request

import com.bhm.ble.utils.BleLogger
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel


/**
 * Request集合管理
 *
 * @author Buhuiming
 * @date 2023年05月22日 10时43分
 */
internal class BleRequestManager {

    private val mRequestMap: HashMap<Class<*>, Any> = HashMap()

    private val mainScope = MainScope()

    companion object {

        private var instance: BleRequestManager = BleRequestManager()

        @Synchronized
        fun get(): BleRequestManager {
            if (instance == null) {
                instance = BleRequestManager()
            }
            return instance
        }
    }

    /**
     * 根据Request类型获取该对象
     */
    fun <T> getRequest(mClass: Class<T>): T {
        if (!mRequestMap.containsKey(mClass)) {
            init(mClass)
        }
        return mRequestMap[mClass] as T
    }

    /**
     * 初始化Request对象
     */
    private fun init(mClass: Class<*>) {
        try {
            mRequestMap[mClass] = mClass.newInstance()
        } catch (instantiationException: InstantiationException) {
            BleLogger.e(instantiationException.stackTraceToString())
        } catch (illegalAccessException: IllegalAccessException) {
            BleLogger.e(illegalAccessException.stackTraceToString())
        }
    }

    /**
     * 断开所有连接 释放资源
     */
    fun release() {
        mRequestMap.clear()
        mainScope.cancel()
    }

    fun getMainScope() = mainScope
}