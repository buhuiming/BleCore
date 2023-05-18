/*
 * Copyright (c) 2022-2032 buhuiming
 * 不能修改和删除上面的版权声明
 * 此代码属于buhuiming编写，在未经允许的情况下不得传播复制
 */
package com.bhm.ble.attribute


/**
 * 配置项
 *
 * @author Buhuiming
 * @date 2023年05月18日 15时04分
 */
class BleOptions private constructor() {

    companion object {

        @JvmStatic
        fun getDefaultBleOptions() : BleOptions = BleOptions()

        @JvmStatic
        fun builder() = Builder()
    }

    class Builder() {

        internal var scanServiceUuids: ArrayList<String> = ArrayList(1)

        internal var scanDeviceNames: ArrayList<String> = ArrayList(1)

        internal var scanDeviceMacs: ArrayList<String> = ArrayList(1)

        /**
         * 设置扫描过滤规则：只查询对应ServiceUuid的设备
         */
        fun setScanServiceUuids(scanServiceUuids: ArrayList<String>) = apply {
            this.scanServiceUuids = scanServiceUuids
        }

        /**
         * 设置扫描过滤规则：只查询对应ServiceUuid的设备
         */
        fun setScanServiceUuid(scanServiceUuid: String) = apply {
            this.scanServiceUuids.add(scanServiceUuid)
        }

        /**
         * 设置扫描过滤规则：只查询对应设备名的设备
         */
        fun setScanDeviceNames(scanDeviceNames: ArrayList<String>) = apply {
            this.scanDeviceNames = scanDeviceNames
        }

        /**
         * 设置扫描过滤规则：只查询对应设备名的设备
         */
        fun setScanDeviceName(scanDeviceName: String) = apply {
            this.scanDeviceNames.add(scanDeviceName)
        }

        /**
         * 设置扫描过滤规则：只查询对应设备Mac的设备
         */
        fun setScanDeviceMacs(scanDeviceMacs: ArrayList<String>) = apply {
            this.scanDeviceMacs = scanDeviceMacs
        }

        /**
         * 设置扫描过滤规则：只查询对应设备Mac的设备
         */
        fun setScanDeviceMac(scanDeviceMac: String) = apply {
            this.scanDeviceMacs.add(scanDeviceMac)
        }

        fun build(): BleOptions {
            return BleOptions()
        }
    }
}