/*
 * Copyright (c) 2022-2032 buhuiming
 * 不能修改和删除上面的版权声明
 * 此代码属于buhuiming编写，在未经允许的情况下不得传播复制
 */
package com.bhm.ble.attribute

import com.bhm.ble.data.BleTaskQueueType
import com.bhm.ble.data.Constants.AUTO_CONNECT
import com.bhm.ble.data.Constants.CONTAIN_SCAN_DEVICE_NAME
import com.bhm.ble.data.Constants.DEFAULT_AUTO_SET_MTU
import com.bhm.ble.data.Constants.DEFAULT_CONNECT_MILLIS_TIMEOUT
import com.bhm.ble.data.Constants.DEFAULT_CONNECT_RETRY_COUNT
import com.bhm.ble.data.Constants.DEFAULT_CONNECT_RETRY_INTERVAL
import com.bhm.ble.data.Constants.DEFAULT_MAX_CONNECT_NUM
import com.bhm.ble.data.Constants.DEFAULT_MTU
import com.bhm.ble.data.Constants.DEFAULT_OPERATE_MILLIS_TIMEOUT
import com.bhm.ble.data.Constants.DEFAULT_SCAN_MILLIS_TIMEOUT
import com.bhm.ble.data.Constants.DEFAULT_SCAN_RETRY_COUNT
import com.bhm.ble.data.Constants.DEFAULT_SCAN_RETRY_INTERVAL
import com.bhm.ble.data.Constants.DEFAULT_OPERATE_INTERVAL
import com.bhm.ble.data.Constants.DEFAULT_TASK_QUEUE_TYPE
import com.bhm.ble.data.Constants.ENABLE_LOG


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

    var operateInterval = builder.operateInterval

    var maxConnectNum = builder.maxConnectNum

    var mtu = builder.mtu

    var autoSetMtu = builder.autoSetMtu

    var taskQueueType = builder.taskQueueType

    companion object {

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

        internal var operateInterval: Long = DEFAULT_OPERATE_INTERVAL

        internal var maxConnectNum: Int = DEFAULT_MAX_CONNECT_NUM

        internal var mtu: Int = DEFAULT_MTU

        internal var autoSetMtu: Boolean = DEFAULT_AUTO_SET_MTU

        internal var taskQueueType: BleTaskQueueType = DEFAULT_TASK_QUEUE_TYPE

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
        @Deprecated(message = "请在业务层处理自动重连，autoConnect设计的初衷是为了断开重连，利用bluetoothGatt" +
                "的重连，但BleCore在断开连接或连接失败后，bluetoothGatt会被close掉释放资源，bluetoothGatt的重连" +
                "不再作用，此函数将会被删除",
            replaceWith = ReplaceWith(
                "BleConnectCallback.onDisConnected、BleManager.connect"
            )
        )
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
         * 设置操作之间的间隔，单位毫秒，默认为[DEFAULT_OPERATE_INTERVAL]
         */
        fun setOperateInterval(operateInterval: Long) = apply {
            this.operateInterval = operateInterval
        }

        /**
         * 设置最大连接数，默认为[DEFAULT_MAX_CONNECT_NUM]
         */
        fun setMaxConnectNum(maxConnectNum: Int) = apply {
            this.maxConnectNum = maxConnectNum
        }

        /**
         * 设置mtu，默认为[DEFAULT_MTU]
         */
        fun setMtu(mtu: Int) = apply {
            setMtu(mtu, DEFAULT_AUTO_SET_MTU)
        }

        /**
         * 设置mtu，默认为[DEFAULT_MTU]
         * @param autoSetMtu 是否自动设置mtu，true：连接成功之后会自动设置mtu，默认为[DEFAULT_AUTO_SET_MTU]
         */
        fun setMtu(mtu: Int, autoSetMtu: Boolean) = apply {
            this.mtu = mtu
            this.autoSetMtu = autoSetMtu
        }

        /**
         * 设置任务队列类型，默认为[BleTaskQueueType.Default]，设置完需断开所有设备才可生效
         * [BleTaskQueueType.Default] 一个设备的Notify\Indicate\Read\Write\mtu操作所对应的
         * 任务共享同一个任务队列(共享队列)(不区分特征值)，rssi在rssi队列
         *
         * [BleTaskQueueType.Operate] 一个设备每个操作独立一个任务队列(不区分特征值)
         * Notify在Notify队列中，Indicate在Indicate队列中，Read在Read队列中，
         * Write在Write队列中，mtu在共享队列，rssi在rssi队列中，
         * 不同操作任务之间相互不影响，相同操作任务之间先进先出按序执行
         * 例如特征值1的写操作和特征值2的写操作，在同一个任务队列当中；特征值1的写操作和特征值1的读操作，
         * 在两个不同的任务队列当中，特征值1的读操作和特征值2的写操作，在两个不同的任务队列当中。
         *
         * [BleTaskQueueType.Independent] 一个设备每个特征值下的每个操作独立一个任务队列(区分特征值)
         * Notify\Indicate\Read\Write所对应的任务分别放入到独立的任务队列中，
         * mtu在共享队列，rssi在rssi队列中，
         * 且按特征值区分，不同操作任务之间相互不影响，相同操作任务之间相互不影响
         * 例如特征值1的写操作和特征值2的写操作，在两个不同的任务队列当中；特征值1的写操作和特征值1的读操作，
         * 在两个不同的任务队列当中，特征值1的读操作和特征值2的写操作，在两个不同的任务队列当中。
         */
        fun setTaskQueueType(taskQueueType: BleTaskQueueType) = apply {
            this.taskQueueType = taskQueueType
        }

        fun build(): BleOptions {
            return BleOptions(this)
        }
    }
}