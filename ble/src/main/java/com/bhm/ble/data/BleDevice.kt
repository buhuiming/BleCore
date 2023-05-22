/*
 * Copyright (c) 2022-2032 buhuiming
 * 不能修改和删除上面的版权声明
 * 此代码属于buhuiming编写，在未经允许的情况下不得传播复制
 */
@file:Suppress("DEPRECATION")

package com.bhm.ble.data

import android.bluetooth.BluetoothDevice
import android.os.Parcel
import android.os.Parcelable


/**
 * BleDevice信息
 *
 * @author Buhuiming
 * @date 2023年05月22日 09时11分
 */
data class BleDevice(
    val deviceInfo: BluetoothDevice?, //设备信息
    val deviceName: String?, //蓝牙广播名
    val DeviceAddress: String?, //蓝牙Mac地址
    val rssi: Int?, //被扫描到时候的信号强度
    val scanRecord: ByteArray?, // 被扫描到时候携带的广播数据
) : Parcelable{

    constructor(parcel: Parcel) : this(
        parcel.readParcelable(BluetoothDevice::class.java.classLoader),
        parcel.readString(),
        parcel.readString(),
        parcel.readValue(Int::class.java.classLoader) as? Int,
        parcel.createByteArray()
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BleDevice

        if (deviceInfo != other.deviceInfo) return false
        if (deviceName != other.deviceName) return false
        if (DeviceAddress != other.DeviceAddress) return false
        if (rssi != other.rssi) return false
        if (scanRecord != null) {
            if (other.scanRecord == null) return false
            if (!scanRecord.contentEquals(other.scanRecord)) return false
        } else if (other.scanRecord != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = deviceInfo?.hashCode() ?: 0
        result = 31 * result + (deviceName?.hashCode() ?: 0)
        result = 31 * result + (DeviceAddress?.hashCode() ?: 0)
        result = 31 * result + (rssi ?: 0)
        result = 31 * result + (scanRecord?.contentHashCode() ?: 0)
        return result
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(deviceInfo, flags)
        parcel.writeString(deviceName)
        parcel.writeString(DeviceAddress)
        parcel.writeValue(rssi)
        parcel.writeByteArray(scanRecord)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<BleDevice> {
        override fun createFromParcel(parcel: Parcel): BleDevice {
            return BleDevice(parcel)
        }

        override fun newArray(size: Int): Array<BleDevice?> {
            return arrayOfNulls(size)
        }
    }

}