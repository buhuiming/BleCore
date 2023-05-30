/*
 * Copyright (c) 2022-2032 buhuiming
 * 不能修改和删除上面的版权声明
 * 此代码属于buhuiming编写，在未经允许的情况下不得传播复制
 */
package com.bhm.ble.control

import kotlinx.coroutines.CancellationException


/**
 * 完成时抛出的Throwable
 *
 * @author Buhuiming
 * @date 2023年05月29日 16时18分
 */
internal class CompleteThrowable : CancellationException()

/**
 * 主动断开连接时抛出的Throwable
 *
 * @author Buhuiming
 * @date 2023年05月29日 16时18分
 */
internal class ActiveDisConnectedThrowable : CancellationException()