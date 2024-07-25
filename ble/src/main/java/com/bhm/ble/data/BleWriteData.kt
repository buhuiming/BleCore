/*
 * Copyright (c) 2022-2032 buhuiming
 * 不能修改和删除上面的版权声明
 * 此代码属于buhuiming编写，在未经允许的情况下不得传播复制
 */
package com.bhm.ble.data

import com.bhm.ble.callback.BleWriteCallback
import java.util.concurrent.atomic.AtomicBoolean


/**
 * 写数据
 * @param operateRandomID 数据id，以时间戳生成
 * @param serviceUUID 服务UUID
 * @param writeUUID 特征值UUID
 * @param currentPackage 当前第几数据包
 * @param totalPackage 总数据包数量
 * @param data 数据包
 * @param isWriting 是否正在写
 * @param isWriteFail 是否写失败
 * @param bleWriteCallback 写回调
 *
 * @author Buhuiming
 * @date 2023年06月12日 09时45分
 */
internal data class BleWriteData(
    var operateRandomID: String,
    var serviceUUID: String,
    var writeUUID: String,
    var currentPackage: Int,
    var totalPackage: Int,
    var data: ByteArray,
    var isWriting: AtomicBoolean = AtomicBoolean(false),
    var isWriteFail: AtomicBoolean = AtomicBoolean(false),
    var bleWriteCallback: BleWriteCallback,
    var writeType: Int? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BleWriteData

        if (operateRandomID != other.operateRandomID) return false
        if (serviceUUID != other.serviceUUID) return false
        if (writeUUID != other.writeUUID) return false
        if (currentPackage != other.currentPackage) return false
        if (totalPackage != other.totalPackage) return false
        if (!data.contentEquals(other.data)) return false
        if (isWriting != other.isWriting) return false
        if (bleWriteCallback != other.bleWriteCallback) return false
        if (writeType != other.writeType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = operateRandomID.hashCode()
        result = 31 * result + serviceUUID.hashCode()
        result = 31 * result + writeUUID.hashCode()
        result = 31 * result + currentPackage
        result = 31 * result + totalPackage
        result = 31 * result + data.contentHashCode()
        result = 31 * result + isWriting.hashCode()
        result = 31 * result + isWriteFail.hashCode()
        result = 31 * result + bleWriteCallback.hashCode()
        result = 31 * result + (writeType ?: 0)
        return result
    }


}