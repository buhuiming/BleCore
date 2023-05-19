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
class BleOptions private constructor(builder: Builder) {

    internal var scanServiceUuids = builder.scanServiceUuids

    internal var scanDeviceNames = builder.scanDeviceNames

    internal var scanDeviceMacs = builder.scanDeviceMacs

    internal var containScanDeviceName = builder.containScanDeviceName

    internal var autoConnect = builder.autoConnect

    internal var enableLog = builder.enableLog

    internal var scanMillisTimeOut = builder.scanMillisTimeOut

    internal var scanRetryCount = builder.scanRetryCount

    internal var scanRetryInterval = builder.scanRetryInterval

    internal var connectMillisTimeOut = builder.connectMillisTimeOut

    internal var connectRetryCount = builder.connectRetryCount

    internal var connectRetryInterval = builder.connectRetryInterval

    internal var operateMillisTimeOut = builder.operateMillisTimeOut

    internal var writeInterval = builder.writeInterval

    internal var maxConnectNum = builder.maxConnectNum

    internal var mtu = builder.mtu

    companion object {

        internal const val CONTAIN_SCAN_DEVICE_NAME = false

        internal const val AUTO_CONNECT = false

        internal const val ENABLE_LOG = true

        internal const val DEFAULT_SCAN_MILLIS_TIMEOUT: Long = 10000

        internal const val DEFAULT_SCAN_RETRY_COUNT: Int = 0

        internal const val DEFAULT_SCAN_RETRY_INTERVAL: Long = 1000

        internal const val DEFAULT_CONNECT_MILLIS_TIMEOUT: Long = 10000

        internal const val DEFAULT_CONNECT_RETRY_COUNT: Int = 0

        internal const val DEFAULT_CONNECT_RETRY_INTERVAL: Long = 1000

        internal const val DEFAULT_OPERATE_MILLIS_TIMEOUT: Long = 5000

        internal const val DEFAULT_WRITE_INTERVAL: Long = 100

        internal const val DEFAULT_MAX_CONNECT_NUM: Int = 7

        internal const val DEFAULT_MTU: Int = 20

        @JvmStatic
        fun getDefaultBleOptions() : BleOptions = BleOptions(Builder())

        @JvmStatic
        fun builder() = Builder()
    }

    class Builder() {

        internal var scanServiceUuids: ArrayList<String> = ArrayList(1)

        internal var scanDeviceNames: ArrayList<String> = ArrayList(1)

        internal var scanDeviceMacs: ArrayList<String> = ArrayList(1)

        internal var containScanDeviceName = CONTAIN_SCAN_DEVICE_NAME

        internal var autoConnect = AUTO_CONNECT

        internal var enableLog = ENABLE_LOG

        internal var scanMillisTimeOut: Long = DEFAULT_SCAN_MILLIS_TIMEOUT

        internal var scanRetryCount: Int = DEFAULT_SCAN_RETRY_COUNT

        internal var scanRetryInterval: Long = DEFAULT_SCAN_RETRY_INTERVAL

        internal var connectMillisTimeOut: Long = DEFAULT_CONNECT_MILLIS_TIMEOUT

        internal var connectRetryCount: Int = DEFAULT_CONNECT_RETRY_COUNT

        internal var connectRetryInterval: Long = DEFAULT_CONNECT_RETRY_INTERVAL

        internal var operateMillisTimeOut: Long = DEFAULT_OPERATE_MILLIS_TIMEOUT

        internal var writeInterval: Long = DEFAULT_WRITE_INTERVAL

        internal var maxConnectNum: Int = DEFAULT_MAX_CONNECT_NUM

        internal var mtu: Int = DEFAULT_MTU

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

        /**
         * 设置扫描过滤规则：是否模糊匹配设备名，默认[CONTAIN_SCAN_DEVICE_NAME]
         */
        fun isContainScanDeviceName(containScanDeviceName: Boolean) = apply {
            this.containScanDeviceName = containScanDeviceName
        }

        /**
         * 扫描后是否自动连接，当扫描到多个设备时，自动连接第一个设备
         */
        fun setAutoConnect(autoConnect: Boolean) = apply {
            this.autoConnect = autoConnect
        }

        /**
         * 扫描超时时间，单位毫秒，默认为[DEFAULT_SCAN_MILLIS_TIMEOUT]
         */
        fun setScanMillisTimeOut(scanMillisTimeOut: Long) = apply {
            this.scanMillisTimeOut = scanMillisTimeOut
        }

        /**
         * 设置扫描重试次数，默认为[DEFAULT_SCAN_RETRY_COUNT]次
         * 设置扫描重试间隔，默认为[DEFAULT_SCAN_RETRY_INTERVAL]
         */
        fun setScanRetryCountAndInterval(scanRetryCount: Int, scanRetryInterval: Long) = apply {
            this.scanRetryCount = scanRetryCount
            this.scanRetryInterval = scanRetryInterval
        }

        /**
         * 默认打开库中的运行日志 默认[ENABLE_LOG]
         */
        fun setEnableLog(enableLog: Boolean) = apply {
            this.enableLog = enableLog
        }

        /**
         * 连接超时时间，单位毫秒，默认为[DEFAULT_CONNECT_MILLIS_TIMEOUT]
         */
        fun setConnectMillisTimeOut(connectMillisTimeOut: Long) = apply {
            this.connectMillisTimeOut = connectMillisTimeOut
        }

        /**
         * 设置连接重试次数，默认为[DEFAULT_CONNECT_RETRY_COUNT]次
         * 设置连接重试间隔，单位毫秒，默认为[DEFAULT_CONNECT_RETRY_INTERVAL]
         */
        fun setConnectRetryCountAndInterval(connectRetryCount: Int, connectRetryInterval: Long) = apply {
            this.connectRetryCount = connectRetryCount
            this.connectRetryInterval = connectRetryInterval
        }

        /**
         * 设置readRssi、setMtu、write、read、notify、indicate的超时时间，
         * 单位毫秒，默认为[DEFAULT_OPERATE_MILLIS_TIMEOUT]
         */
        fun setOperateMillisTimeOut(operateMillisTimeOut: Long) = apply {
            this.operateMillisTimeOut = operateMillisTimeOut
        }

        /**
         * 设置写操作之间的间隔，单位毫秒，默认为[DEFAULT_WRITE_INTERVAL]
         */
        fun setWriteInterval(writeInterval: Long) = apply {
            this.writeInterval = writeInterval
        }

        /**
         * 设置最大连接数，默认为[DEFAULT_MAX_CONNECT_NUM]
         */
        fun setMaxConnectNum(maxConnectNum: Int) = apply {
            this.maxConnectNum = maxConnectNum
        }

        /**
         * 设置mtu，默认为[DEFAULT_MTU]，最大为512
         */
        fun setMtu(mtu: Int) = apply {
            this.mtu = mtu
            if (this.mtu > 512) {
                this.mtu = 512
            }
        }

        fun build(): BleOptions {
            return BleOptions(this)
        }
    }
}