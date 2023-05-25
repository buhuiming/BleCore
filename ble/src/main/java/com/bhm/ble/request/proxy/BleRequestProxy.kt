/*
 * Copyright (c) 2022-2032 buhuiming
 * 不能修改和删除上面的版权声明
 * 此代码属于buhuiming编写，在未经允许的情况下不得传播复制
 */
@file:Suppress("SENSELESS_COMPARISON")

package com.bhm.ble.request.proxy

import com.bhm.ble.request.BleConnectRequest
import com.bhm.ble.request.BleRequestManager
import com.bhm.ble.request.BleScanRequest
import com.bhm.ble.utils.BleLogger
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy


/**
 * 通过代理获取Request对象
 *
 * @author Buhuiming
 * @date 2023年05月22日 11时07分
 */
internal class BleRequestProxy : InvocationHandler {

    private lateinit var target: Any

    companion object {

        private var instance: BleRequestProxy = BleRequestProxy()

        @Synchronized
        fun get(): BleRequestProxy {
            if (instance == null) {
                instance = BleRequestProxy()
            }
            return instance
        }
    }

    /**
     * 绑定实现类
     */
    fun bindProxy(paramObject: Any): Any {
        this.target = paramObject
        return Proxy.newProxyInstance(
            paramObject.javaClass.classLoader,
            paramObject.javaClass.interfaces,
            this
        )
    }

    /**
     * 触发实现类方法
     */
    @Throws(Throwable::class)
    override fun invoke(
        paramObject: Any?,
        paramMethod: Method,
        paramArrayOfObject: Array<Any?>?
    ): Any? {
        try {
            paramArrayOfObject?.let {
                return paramMethod.invoke(this.target, *it)
            }
            return paramMethod.invoke(this.target)
        } catch (e: Exception) {
            BleLogger.e(e.stackTraceToString())
        }
        return null
    }
}