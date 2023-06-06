/*
 * Copyright (c) 2022-2032 buhuiming
 * 不能修改和删除上面的版权声明
 * 此代码属于buhuiming编写，在未经允许的情况下不得传播复制
 */
package com.bhm.ble.control

import java.util.concurrent.CopyOnWriteArrayList


/**
 * 存放操作任务
 *
 * @author Buhuiming
 * @date 2023年06月06日 10时41分
 */
class BleTaskList : CopyOnWriteArrayList<BleTask>() {

    private val taskIdList = CopyOnWriteArrayList<Int>()

    override fun add(element: BleTask?): Boolean {
        taskIdList.add(element?.taskId)
        return super.add(element)
    }

    override fun remove(element: BleTask?): Boolean {
        if (taskIdList.contains(element?.taskId)) {
            taskIdList.remove(element?.taskId)
        }
        return super.remove(element)
    }

    fun containsTaskId(taskId: Int): Boolean {
        return taskIdList.contains(taskId)
    }
}