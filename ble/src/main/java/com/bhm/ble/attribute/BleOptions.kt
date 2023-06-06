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

    var scanServiceUuids = builder.scanServiceUuids

    var scanDeviceNames = builder.scanDeviceNames

    var scanDeviceAddresses = builder.scanDeviceAddresses

    var containScanDeviceName = builder.containScanDeviceName

    var autoConnect = builder.autoConnect

    var enableLog = builder.enableLog

    var scanMillisTimeOut = builder.scanMillisTimeOut

    var scanRetryCount = builder.scanRetryCount

    var scanRetryInterval = builder.scanRetryInterval

    var connectMillisTimeOut = builder.connectMillisTimeOut

    var connectRetryCount = builder.connectRetryCount

    var connectRetryInterval = builder.connectRetryInterval

    var operateMillisTimeOut = builder.operateMillisTimeOut

    var writeInterval = builder.writeInterval

    var maxConnectNum = builder.maxConnectNum

    var mtu = builder.mtu

    var autoSetMtu = builder.autoSetMtu

    companion object {

        const val CONTAIN_SCAN_DEVICE_NAME = false

        const val AUTO_CONNECT = true

        const val ENABLE_LOG = true

        const val DEFAULT_SCAN_MILLIS_TIMEOUT: Long = 10000

        const val DEFAULT_SCAN_RETRY_COUNT: Int = 0

        const val DEFAULT_SCAN_RETRY_INTERVAL: Long = 1000

        const val DEFAULT_CONNECT_MILLIS_TIMEOUT: Long = 10000

        const val DEFAULT_CONNECT_RETRY_COUNT: Int = 0

        const val DEFAULT_CONNECT_RETRY_INTERVAL: Long = 1000

        const val DEFAULT_OPERATE_MILLIS_TIMEOUT: Long = 10000

        const val DEFAULT_WRITE_INTERVAL: Long = 100

        const val DEFAULT_MAX_CONNECT_NUM: Int = 7

        const val DEFAULT_MTU: Int = 23

        const val DEFAULT_AUTO_SET_MTU = true

        //系统提供接受通知自带的UUID
        const val UUID_CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR =
            "00002902-0000-1000-8000-00805f9b34fb"

        @JvmStatic
        fun getDefaultBleOptions() : BleOptions = BleOptions(Builder())

        @JvmStatic
        fun builder() = Builder()
    }

    class Builder {

        internal var scanServiceUuids: ArrayList<String> = ArrayList(1)

        internal var scanDeviceNames: ArrayList<String> = ArrayList(1)

        internal var scanDeviceAddresses: ArrayList<String> = ArrayList(1)

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

        internal var autoSetMtu: Boolean = DEFAULT_AUTO_SET_MTU

        /**
         * 设置扫描过滤规则：只查询对应ServiceUuid的设备
         */
        fun setScanServiceUuid(vararg scanServiceUuids: String) = apply {
            scanServiceUuids.forEach {
                if (it.isNotEmpty()) {
                    this.scanServiceUuids.add(it)
                }
            }
        }

        /**
         * 设置扫描过滤规则：只查询对应设备名的设备
         */
        fun setScanDeviceName(vararg scanDeviceNames: String) = apply {
            scanDeviceNames.forEach {
                if (it.isNotEmpty()) {
                    this.scanDeviceNames.add(it)
                }
            }
        }

        /**
         * 设置扫描过滤规则：只查询对应设备Mac的设备
         */
        fun setScanDeviceAddress(vararg scanDeviceAddresses: String) = apply {
            scanDeviceAddresses.forEach {
                if (it.isNotEmpty()) {
                    this.scanDeviceAddresses.add(it)
                }
            }
        }

        /**
         * 设置扫描过滤规则：是否模糊匹配设备名，默认[CONTAIN_SCAN_DEVICE_NAME]
         */
        fun isContainScanDeviceName(containScanDeviceName: Boolean) = apply {
            this.containScanDeviceName = containScanDeviceName
        }

        /**
         * 非主动断开后是否自动连接，默认为[AUTO_CONNECT]
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
         * 这个机制是：不会因为扫描的次数导致上一次扫描到的数据被清空，也就是onScanStart和onScanComplete
         * 都只会回调一次，而且扫描到的数据是所有扫描次数的总和
         * 设置扫描重试次数，默认为[DEFAULT_SCAN_RETRY_COUNT]次，总扫描次数=scanRetryCount+1次
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
         * 设置mtu，默认为[DEFAULT_MTU]
         * @param autoSetMtu 是否自动设置mtu，true：连接成功之后会自动设置mtu，默认为[DEFAULT_AUTO_SET_MTU]
         */
        fun setMtu(mtu: Int, autoSetMtu: Boolean = DEFAULT_AUTO_SET_MTU) = apply {
            this.mtu = mtu
            this.autoSetMtu = autoSetMtu
        }

        fun build(): BleOptions {
            return BleOptions(this)
        }
    }
}