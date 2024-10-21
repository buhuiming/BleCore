package com.bhm.ble.data

/**
 * @description [com.bhm.ble.request.BleWriteRequest.writeQueueData]方法中队列存放的数据结构
 * @author Buhuiming
 * @date 2024/10/18/ 18:04
 */
internal data class BleWriteQueueData(
    var operateRandomID: String,
    var serviceUUID: String,
    var writeUUID: String,
    var data: ByteArray,
    var skipErrorPacketData: Boolean,
    var retryWriteCount: Int,
    var retryDelayTime: Long,
    var writeType: Int?,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BleWriteQueueData

        if (operateRandomID != other.operateRandomID) return false
        if (serviceUUID != other.serviceUUID) return false
        if (writeUUID != other.writeUUID) return false
        if (!data.contentEquals(other.data)) return false
        if (skipErrorPacketData != other.skipErrorPacketData) return false
        if (retryWriteCount != other.retryWriteCount) return false
        if (retryDelayTime != other.retryDelayTime) return false
        if (writeType != other.writeType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = operateRandomID.hashCode()
        result = 31 * result + serviceUUID.hashCode()
        result = 31 * result + writeUUID.hashCode()
        result = 31 * result + data.contentHashCode()
        result = 31 * result + skipErrorPacketData.hashCode()
        result = 31 * result + retryWriteCount
        result = 31 * result + retryDelayTime.hashCode()
        result = 31 * result + (writeType ?: 0)
        return result
    }
}
