/*
 * Copyright (c) 2022-2032 buhuiming
 * 不能修改和删除上面的版权声明
 * 此代码属于buhuiming编写，在未经允许的情况下不得传播复制
 */
package com.bhm.ble.log

/**
 * @description 日志事件接口
 * @author Buhuiming
 * @date 2024/05/08/ 10:27
 */
interface BleLogEvent {
    /**
     * 注意该方法是否是线程安全的(有可能在子线程中调用)
     */
    fun onLog(level: BleLogLevel, tag: String, message: String?)
}