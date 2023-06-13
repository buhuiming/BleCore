/*
 * Copyright (c) 2022-2032 buhuiming
 * 不能修改和删除上面的版权声明
 * 此代码属于buhuiming编写，在未经允许的情况下不得传播复制
 */
package com.bhm.ble.data


/**
 * 任务队列类型，针对notify\indicate\read\write操作
 * 提供可选的任务队列类型
 *
 * @author Buhuiming
 * @date 2023年06月13日 14时49分
 */
sealed class BleTaskQueueType {

    /**
     * 默认
     * 一个设备所有操作共享一个任务队列(不区分特征值)
     * Notify\Indicate\Read\Write(包括rssi\mtu)所对应的任务
     * 都将放入到同一个任务队列中，先进先出按序执行
     */
    object Single : BleTaskQueueType()

    /**
     * 一个设备每个操作独立一个任务队列(不区分特征值)
     * Notify\Indicate\Read\Write(不包括rssi\mtu，rssi\mtu仍在共享队列)所对应的任务
     * 分别放入到独立的任务队列中，不同操作任务之间相互不影响，相同操作任务之间先进先出按序执行
     * 例如特征值1的写操作和特征值2的写操作，在同一个任务队列当中；特征值1的写操作和特征值1的读操作，
     * 在两个不同的任务队列当中，特征值1的读操作和特征值2的写操作，在两个不同的任务队列当中。
     */
    object Operate : BleTaskQueueType()

    /**
     * 一个设备每个特征值下的每个操作独立一个任务队列(区分特征值)
     * Notify\Indicate\Read\Write(不包括rssi\mtu，rssi\mtu仍在共享队列)所对应的任务
     * 分别放入到独立的任务队列中，且按特征值区分，不同操作任务之间相互不影响，相同操作任务之间相互不影响
     * 例如特征值1的写操作和特征值2的写操作，在两个不同的任务队列当中；特征值1的写操作和特征值1的读操作，
     * 在两个不同的任务队列当中，特征值1的读操作和特征值2的写操作，在两个不同的任务队列当中。
     */
    object Independent : BleTaskQueueType()
}