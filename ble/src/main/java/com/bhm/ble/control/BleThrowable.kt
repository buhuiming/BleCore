/*
 * Copyright (c) 2022-2032 buhuiming
 * 不能修改和删除上面的版权声明
 * 此代码属于buhuiming编写，在未经允许的情况下不得传播复制
 */
package com.bhm.ble.control

import kotlinx.coroutines.CancellationException


/**
 * 完成时抛出的Throwable
 * @author Buhuiming
 * @date 2023年05月29日 16时18分
 */
internal class CompleteThrowable(msg: String? = null) : CancellationException(msg)

/**
 * 主动取消
 * @author Buhuiming
 * @date :2023/6/5 14:32
 */
internal class CancellationThrowable(msg: String? = null) : CancellationException(msg)

/**
 * 超时抛出的Throwable
 * @author Buhuiming
 * @date :2023/6/5 10:24
 */
internal class TimeoutCancellationThrowable(msg: String? = null) : CancellationException(msg)

/**
 * 主动断开连接时抛出的Throwable
 *
 * @author Buhuiming
 * @date 2023年05月29日 16时18分
 */
internal class ActiveDisConnectedThrowable(msg: String? = null) : CancellationException(msg)

/**
 * 设置Notify失败
 */
sealed class NotifyFailException(message: String): CancellationException(message) {

    object DescriptorException : NotifyFailException("设置Notify失败，Descriptor写数据失败")

    object UnConnectedException : NotifyFailException("设置Notify失败，设备未连接")

    object UnSupportNotifyException : NotifyFailException("设置Notify失败，此特性不支持Notify")

    object SetCharacteristicNotificationFailException : NotifyFailException("设置Notify失败，SetCharacteristicNotificationFail")

    object TimeoutCancellationException : NotifyFailException("设置Notify失败，设置超时")
}