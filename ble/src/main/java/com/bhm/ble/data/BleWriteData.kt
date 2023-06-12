/*
 * Copyright (c) 2022-2032 buhuiming
 * 不能修改和删除上面的版权声明
 * 此代码属于buhuiming编写，在未经允许的情况下不得传播复制
 */
package com.bhm.ble.data

import java.util.concurrent.atomic.AtomicInteger


/**
 * 写数据
 *
 * @author Buhuiming
 * @date 2023年06月12日 09时45分
 */
data class BleWriteData(
    var currentPackage: AtomicInteger = AtomicInteger(0),
    var totalPackage: Int = 0,
    var operateRandomID: String? = null,
    var data: ByteArray? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BleWriteData

        if (currentPackage != other.currentPackage) return false
        if (totalPackage != other.totalPackage) return false
        if (data != null) {
            if (other.data == null) return false
            if (!data.contentEquals(other.data)) return false
        } else if (other.data != null) return false
        if (operateRandomID != other.operateRandomID) return false

        return true
    }

    override fun hashCode(): Int {
        var result = currentPackage.hashCode()
        result = 31 * result + totalPackage
        result = 31 * result + (data?.contentHashCode() ?: 0)
        result = 31 * result + (operateRandomID?.hashCode() ?: 0)
        return result
    }
}