/*
 * Copyright (c) 2022-2032 buhuiming
 * 不能修改和删除上面的版权声明
 * 此代码属于buhuiming编写，在未经允许的情况下不得传播复制
 */
package com.bhm.ble.control

import java.util.*


/**
 * 存放操作任务
 *
 * @author Buhuiming
 * @date 2023年06月06日 10时41分
 */
internal class BleTaskList {

    private val taskIdList = Collections.synchronizedList(LinkedList<String>())

    private val list = Collections.synchronizedList(LinkedList<BleTask>())

    fun list(): MutableList<BleTask> = list

    fun add(element: BleTask) {
        taskIdList.add(element.taskId)
        list.add(element)
    }

    fun remove(element: BleTask?) {
        if (taskIdList.contains(element?.taskId)) {
            taskIdList.remove(element?.taskId)
        }
        list.remove(element)
    }

    fun iterator() = list.iterator()

    fun size() = list.size

    fun contains(task: BleTask?) = list.contains(task)

    fun clear() {
        list.clear()
        taskIdList.clear()
    }

    fun firstOrNull() = list.firstOrNull()

    fun containsTaskId(taskId: String) = taskIdList.contains(taskId)
}