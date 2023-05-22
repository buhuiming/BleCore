/*
 * Copyright (c) 2022-2032 buhuiming
 * 不能修改和删除上面的版权声明
 * 此代码属于buhuiming编写，在未经允许的情况下不得传播复制
 */
@file:Suppress("SENSELESS_COMPARISON", "UNCHECKED_CAST")

package com.bhm.ble.request

import com.bhm.ble.utils.BleLogger


/**
 * Request集合管理
 *
 * @author Buhuiming
 * @date 2023年05月22日 10时43分
 */
internal class BleRequestManager {

    private val mRequestMap: HashMap<Class<*>, Any> = HashMap()

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
        return mRequestMap[mClass] as T
    }

    /**
     * 初始化所有Request对象
     */
    fun init(mClasses: Array<Class<*>>) {
        mClasses.forEach { mClass ->
            try {
                mRequestMap[mClass] = mClass.newInstance()
            } catch (instantiationException: InstantiationException) {
                BleLogger.e(instantiationException.stackTraceToString())
            } catch (illegalAccessException: IllegalAccessException) {
                BleLogger.e(illegalAccessException.stackTraceToString())
            }
        }
    }
}