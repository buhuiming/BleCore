/*
 * Copyright (c) 2022-2032 buhuiming
 * 不能修改和删除上面的版权声明
 * 此代码属于buhuiming编写，在未经允许的情况下不得传播复制
 */
package com.bhm.ble.callback


/**
 * 写回调
 *
 * @author Buhuiming
 * @date 2023年05月26日 15时54分
 */
open class BleWriteCallback : BleBaseCallback() {

    /**
     * [BleBaseCallback.key]和[singleKey]不一样，前者是特征值UUID，后者是触发写操作时根据时间戳生成。
     * 前者可以用来移除写操作的回调，后者用来区分写操作任务，每个特征值(UUID)可能存在多个写操作任务(不同singleKey)，
     * 一个写操作任务(共用一个singleKey)可能多个包，通过singleKey来取消一次写操作任务(不是取消一个包的写任务)
     */
    private var singleKey: String? = null

    private var serviceUUID: String? = null

    private var writeSuccess: ((current: Int, total: Int, justWrite: ByteArray) -> Unit)? = null

    private var writeFail: ((throwable: Throwable) -> Unit)? = null

    private var writeComplete: (() -> Unit)? = null

    fun setSingleKey(singleKey: String) {
        this.singleKey = singleKey
    }

    fun getSingleKey() = singleKey

    fun setServiceUUID(serviceUUID: String) {
        this.serviceUUID = serviceUUID
    }

    fun getServiceUUID() = serviceUUID

    fun onWriteFail(value: ((throwable: Throwable) -> Unit)) {
        writeFail = value
    }

    fun onWriteSuccess(value: ((current: Int, total: Int, justWrite: ByteArray) -> Unit)) {
        writeSuccess = value
    }

    fun onWriteComplete(value: (() -> Unit)) {
        writeComplete = value
    }

    open fun callWriteFail(throwable: Throwable) {
        launchInMainThread {
            writeFail?.invoke(throwable)
        }
    }

    open fun callWriteSuccess(current: Int, total: Int, justWrite: ByteArray) {
        launchInMainThread {
            writeSuccess?.invoke(current, total, justWrite)
        }
    }

    open fun callWriteComplete() {
        launchInMainThread {
            writeComplete?.invoke()
        }
    }
}