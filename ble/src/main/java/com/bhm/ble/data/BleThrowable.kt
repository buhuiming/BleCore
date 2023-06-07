/*
 * Copyright (c) 2022-2032 buhuiming
 * 不能修改和删除上面的版权声明
 * 此代码属于buhuiming编写，在未经允许的情况下不得传播复制
 */
package com.bhm.ble.data

import kotlinx.coroutines.CancellationException


/**
 * 完成时抛出的Exception
 * @author Buhuiming
 * @date 2023年05月29日 16时18分
 */
open class CompleteException(msg: String? = null) : CancellationException(msg)

/**
 * 主动取消
 * @author Buhuiming
 * @date :2023/6/5 14:32
 */
open class CancelException(msg: String? = null) : CancellationException(msg)

/**
 * 超时抛出的Exception
 * @author Buhuiming
 * @date :2023/6/5 10:24
 */
open class TimeoutCancelException(msg: String? = null) : CancellationException(msg)

/**
 * 设备未连接抛出的Exception
 * @author Buhuiming
 * @date :2023/6/5 10:24
 */
open class UnConnectedException(msg: String? = "设备未连接") : CancellationException(msg)

/**
 * 主动断开连接时抛出的Exception
 *
 * @author Buhuiming
 * @date 2023年05月29日 16时18分
 */
open class ActiveDisConnectedException(msg: String? = null) : CancellationException(msg)
