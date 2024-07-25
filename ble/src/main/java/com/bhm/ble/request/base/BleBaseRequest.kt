/*
 * Copyright (c) 2022-2032 buhuiming
 * 不能修改和删除上面的版权声明
 * 此代码属于buhuiming编写，在未经允许的情况下不得传播复制
 */
package com.bhm.ble.request.base

import android.bluetooth.BluetoothGatt
import android.util.SparseArray
import com.bhm.ble.callback.*
import com.bhm.ble.data.BleDescriptorGetType
import com.bhm.ble.device.BleDevice


/**
 * 抽象方法
 *
 * @author Buhuiming
 * @date 2023年05月22日 10时37分
 */
internal interface BleBaseRequest {

    /**
     * 开始扫描
     */
    fun startScan(
        scanMillisTimeOut: Long?,
        scanRetryCount: Int?,
        scanRetryInterval: Long?,
        bleScanCallback: BleScanCallback.() -> Unit
    )

    /**
     * 是否扫描中
     */
    fun isScanning(): Boolean

    /**
     * 停止扫描
     */
    fun stopScan()

    /**
     * 扫描并连接，如果扫描到多个设备，则会连接第一个
     */
    fun startScanAndConnect(scanMillisTimeOut: Long?,
                            scanRetryCount: Int?,
                            scanRetryInterval: Long?,
                            connectMillisTimeOut: Long?,
                            connectRetryCount: Int?,
                            connectRetryInterval: Long?,
                            isForceConnect: Boolean = false,
                            bleScanCallback: BleScanCallback.() -> Unit,
                            bleConnectCallback: BleConnectCallback.() -> Unit)

    /**
     * 开始连接
     */
    fun connect(bleDevice: BleDevice,
                connectMillisTimeOut: Long?,
                connectRetryCount: Int?,
                connectRetryInterval: Long?,
                isForceConnect: Boolean = false,
                bleConnectCallback: BleConnectCallback.() -> Unit)

    /**
     * 断开连接
     */
    fun disConnect(bleDevice: BleDevice)

    /**
     * 取消/停止连接
     */
    fun stopConnect(bleDevice: BleDevice)

    /**
     * 是否已连接
     */
    fun isConnected(bleDevice: BleDevice): Boolean

    /**
     * 获取设备的BluetoothGatt对象
     */
    fun getBluetoothGatt(bleDevice: BleDevice): BluetoothGatt?

    /**
     * notify
     */
    fun notify(bleDevice: BleDevice,
               serviceUUID: String,
               notifyUUID: String,
               bleDescriptorGetType: BleDescriptorGetType = BleDescriptorGetType.Default,
               bleNotifyCallback: BleNotifyCallback.() -> Unit)

    /**
     * stop notify
     */
    fun stopNotify(bleDevice: BleDevice,
                   serviceUUID: String,
                   notifyUUID: String,
                   bleDescriptorGetType: BleDescriptorGetType = BleDescriptorGetType.Default): Boolean

    /**
     * indicate
     */
    fun indicate(bleDevice: BleDevice,
                 serviceUUID: String,
                 indicateUUID: String,
                 bleDescriptorGetType: BleDescriptorGetType = BleDescriptorGetType.Default,
                 bleIndicateCallback: BleIndicateCallback.() -> Unit)

    /**
     * stop indicate
     */
    fun stopIndicate(bleDevice: BleDevice,
                     serviceUUID: String,
                     indicateUUID: String,
                     bleDescriptorGetType: BleDescriptorGetType = BleDescriptorGetType.Default): Boolean

    /**
     * 读取信号值
     */
    fun readRssi(bleDevice: BleDevice,
                 bleRssiCallback: BleRssiCallback.() -> Unit)

    /**
     * 设置mtu
     */
    fun setMtu(bleDevice:
               BleDevice, mtu: Int,
               bleMtuChangedCallback: BleMtuChangedCallback.() -> Unit)

    /**
     * 设置设备的传输优先级
     * connectionPriority 必须是 [BluetoothGatt.CONNECTION_PRIORITY_BALANCED]、
     * [BluetoothGatt.CONNECTION_PRIORITY_HIGH]、
     * [BluetoothGatt.CONNECTION_PRIORITY_LOW_POWER]的其中一个
     *
     */
    fun setConnectionPriority(bleDevice: BleDevice, connectionPriority: Int): Boolean

    /**
     * 读特征值数据
     */
    fun readData(bleDevice: BleDevice,
                 serviceUUID: String,
                 readUUID: String,
                 bleReadCallback: BleReadCallback.() -> Unit)

    /**
     * 写数据
     */
    fun writeData(bleDevice: BleDevice,
                  serviceUUID: String,
                  writeUUID: String,
                  dataArray: SparseArray<ByteArray>,
                  writeType: Int? = null,
                  bleWriteCallback: BleWriteCallback.() -> Unit)

    /**
     * 放入一个写队列，写成功，则从队列中取下一个数据，写失败，则重试[retryWriteCount]次
     * 与[writeData]的区别在于，[writeData]写成功，则从队列中取下一个数据，写失败，则不再继续写后面的数据
     *
     * @param skipErrorPacketData 是否跳过数据长度为0的数据包
     * @param retryWriteCount 写失败后重试的次数
     */
    fun writeQueueData(bleDevice: BleDevice,
                       serviceUUID: String,
                       writeUUID: String,
                       dataArray: SparseArray<ByteArray>,
                       skipErrorPacketData: Boolean = false,
                       retryWriteCount: Int = 0,
                       retryDelayTime: Long = 0L,
                       writeType: Int? = null,
                       bleWriteCallback: BleWriteCallback.() -> Unit)

    /**
     * 获取所有已连接设备集合
     */
    fun getAllConnectedDevice(): MutableList<BleDevice>

    /**
     * 添加设备的连接状态发生变化、indicate/notify收到数据、mtu改变的回调
     */
    fun addBleEventCallback(bleDevice: BleDevice, bleEventCallback: BleEventCallback.() -> Unit)

    /**
     * 移除该设备的连接回调
     */
    fun removeBleConnectCallback(bleDevice: BleDevice)

    /**
     * 替换该设备的连接回调
     */
    fun replaceBleConnectCallback(bleDevice: BleDevice, bleConnectCallback: BleConnectCallback.() -> Unit)

    /**
     * 移除该设备的Indicate回调
     */
    fun removeBleIndicateCallback(bleDevice: BleDevice, indicateUUID: String)

    /**
     * 移除该设备的Notify回调
     */
    fun removeBleNotifyCallback(bleDevice: BleDevice, notifyUUID: String)

    /**
     * 移除该设备的Read回调
     */
    fun removeBleReadCallback(bleDevice: BleDevice, readUUID: String)

    /**
     * 移除该设备的MtuChanged回调
     */
    fun removeBleMtuChangedCallback(bleDevice: BleDevice)

    /**
     * 移除该设备的Rssi回调
     */
    fun removeBleRssiCallback(bleDevice: BleDevice)

    /**
     * 移除该设备的Write回调
     */
    fun removeBleWriteCallback(
        bleDevice: BleDevice,
        writeUUID: String,
        bleWriteCallback: BleWriteCallback? = null
    )

    /**
     * 移除该设备回调，BleConnectCallback除外
     */
    fun removeAllCharacterCallback(bleDevice: BleDevice)

    /**
     * 移除该设备Event回调
     */
    fun removeBleEventCallback(bleDevice: BleDevice)

    /**
     * 断开所有设备的连接
     */
    fun disConnectAll()

    /**
     * 移除该设备的Scan回调
     */
    fun removeBleScanCallback()

    /**
     * 注册系统蓝牙广播
     */
    fun registerBluetoothStateReceiver(bluetoothCallback: BluetoothCallback.() -> Unit)

    /**
     * 取消注册系统蓝牙广播
     */
    fun unRegisterBluetoothStateReceiver()

    /**
     * 断开某个设备的连接 释放资源
     */
    fun close(bleDevice: BleDevice)

    /**
     * 断开所有连接 释放资源
     */
    fun closeAll()
}