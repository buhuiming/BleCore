/*
 * Copyright (c) 2022-2032 buhuiming
 * 不能修改和删除上面的版权声明
 * 此代码属于buhuiming编写，在未经允许的情况下不得传播复制
 */
package com.bhm.demo.entity


/**
 * 操作类型
 *
 * @author Buhuiming
 * @date 2023年06月01日 14时11分
 */
sealed class OperateType {

    /**
     * 写操作
     */
    object Write : OperateType()

    /**
     * 读操作
     */
    object Read : OperateType()

    /**
     * Notify操作
     */
    object Notify : OperateType()

    /**
     * Indicate操作
     */
    object Indicate : OperateType()
}