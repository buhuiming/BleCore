/*
 * Copyright (c) 2022-2032 buhuiming
 * 不能修改和删除上面的版权声明
 * 此代码属于buhuiming编写，在未经允许的情况下不得传播复制
 */
package com.bhm.demo.vm

import android.app.Application
import android.bluetooth.BluetoothGattCharacteristic
import android.util.SparseArray
import com.bhm.ble.BleManager
import com.bhm.ble.data.BleDescriptorGetType
import com.bhm.ble.data.Constants.DEFAULT_MTU
import com.bhm.ble.device.BleDevice
import com.bhm.ble.log.BleLogger
import com.bhm.ble.utils.BleUtil
import com.bhm.demo.entity.CharacteristicNode
import com.bhm.demo.entity.LogEntity
import com.bhm.demo.entity.ServiceNode
import com.bhm.support.sdk.common.BaseViewModel
import com.chad.library.adapter.base.entity.node.BaseNode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.logging.Level


/**
 *
 * @author Buhuiming
 * @date 2023年06月01日 09时18分
 */
class DetailViewModel(application: Application) : BaseViewModel(application) {

    private val listLogMutableStateFlow = MutableStateFlow(LogEntity(Level.INFO, "数据适配完毕"))

    val listLogStateFlow: StateFlow<LogEntity> = listLogMutableStateFlow

    private val listRefreshMutableStateFlow = MutableStateFlow("")

    val listRefreshStateFlow: StateFlow<String> = listRefreshMutableStateFlow

    val listLogData = mutableListOf<LogEntity>()

    /**
     * 根据bleDevice拿到服务特征值数据
     */
    fun getListData(bleDevice: BleDevice): MutableList<BaseNode> {
        val gatt = BleManager.get().getBluetoothGatt(bleDevice)
        val list: MutableList<BaseNode> = arrayListOf()
        gatt?.services?.forEachIndexed { index, service ->
            val childList: MutableList<BaseNode> = arrayListOf()
            service.characteristics?.forEachIndexed { position, characteristics ->
                val characteristicNode = CharacteristicNode(
                    position.toString(),
                    service.uuid.toString(),
                    characteristics.uuid.toString(),
                    getOperateType(characteristics),
                    characteristics.properties,
                    enableNotify = false,
                    enableIndicate = false,
                    enableWrite = false
                )
                childList.add(characteristicNode)
            }
            val serviceNode = ServiceNode(
                index.toString(),
                service.uuid.toString(),
                childList
            )
            list.add(serviceNode)
        }
        return list
    }

    /**
     * 获取特征值的属性
     */
    private fun getOperateType(characteristic: BluetoothGattCharacteristic): String {
        val property = StringBuilder()
        val charaProp: Int = characteristic.properties
        if (charaProp and BluetoothGattCharacteristic.PROPERTY_READ != 0) {
            property.append("Read")
            property.append(" , ")
        }
        if (charaProp and BluetoothGattCharacteristic.PROPERTY_WRITE != 0) {
            property.append("Write")
            property.append(" , ")
        }
        if (charaProp and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0) {
            property.append("Write No Response")
            property.append(" , ")
        }
        if (charaProp and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) {
            property.append("Notify")
            property.append(" , ")
        }
        if (charaProp and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0) {
            property.append("Indicate")
            property.append(" , ")
        }
        if (property.length > 1) {
            property.delete(property.length - 2, property.length - 1)
        }
        return if (property.isNotEmpty()) {
            property.toString()
        } else {
            ""
        }
    }

    /**
     * 添加日志显示
     */
    @Synchronized
    fun addLogMsg(logEntity: LogEntity) {
        listLogMutableStateFlow.value = logEntity
    }

    /**
     * notify
     */
    fun notify(bleDevice: BleDevice,
               node: CharacteristicNode
    ) {
        BleManager.get().notify(bleDevice, node.serviceUUID, node.characteristicUUID, BleDescriptorGetType.AllDescriptor) {
            onNotifyFail { _, _, t ->
                addLogMsg(LogEntity(Level.OFF, "notify失败：${t.message}"))
                node.enableNotify = false
                listRefreshMutableStateFlow.value = System.currentTimeMillis().toString()
            }
            onNotifySuccess { _, _ ->
                addLogMsg(LogEntity(Level.FINE, "notify成功：${node.characteristicUUID}"))
            }
            onCharacteristicChanged {_, _, data ->
                //数据处理在IO线程，显示UI要切换到主线程
                launchInMainThread {
                    addLogMsg(
                        LogEntity(
                            Level.INFO, "Notify接收到${node.characteristicUUID}的数据：" +
                                    BleUtil.bytesToHex(data)
                        )
                    )
                }
            }
        }
    }

    /**
     * stop notify
     */
    fun stopNotify(
        bleDevice: BleDevice,
        node: CharacteristicNode
    ) {
        val success = BleManager.get().stopNotify(bleDevice, node.serviceUUID, node.characteristicUUID, BleDescriptorGetType.AllDescriptor)
        if (success == true) {
            addLogMsg(LogEntity(Level.FINE, "notify取消成功：${node.characteristicUUID}"))
        } else {
            addLogMsg(LogEntity(Level.OFF, "notify取消失败：${node.characteristicUUID}"))
        }
    }

    /**
     * indicate
     */
    fun indicate(bleDevice: BleDevice,
                 node: CharacteristicNode
    ) {
        BleManager.get().indicate(bleDevice, node.serviceUUID, node.characteristicUUID, BleDescriptorGetType.AllDescriptor) {
            onIndicateFail {_, _, t ->
                addLogMsg(LogEntity(Level.OFF, "indicate失败：${t.message}"))
                node.enableIndicate = false
                listRefreshMutableStateFlow.value = System.currentTimeMillis().toString()
            }
            onIndicateSuccess { _, _, ->
                addLogMsg(LogEntity(Level.FINE, "indicate成功：${node.characteristicUUID}"))
            }
            onCharacteristicChanged {_, _, data ->
                //数据处理在IO线程，显示UI要切换到主线程
                launchInMainThread {
                    addLogMsg(
                        LogEntity(
                            Level.INFO, "Indicate接收到${node.characteristicUUID}的数据：" +
                                    BleUtil.bytesToHex(data)
                        )
                    )
                }
            }
        }
    }

    /**
     * stop indicate
     */
    fun stopIndicate(
        bleDevice: BleDevice,
        node: CharacteristicNode
    ) {
        val success = BleManager.get().stopIndicate(bleDevice, node.serviceUUID, node.characteristicUUID, BleDescriptorGetType.AllDescriptor)
        if (success == true) {
            addLogMsg(LogEntity(Level.FINE, "indicate取消成功：${node.characteristicUUID}"))
        } else {
            addLogMsg(LogEntity(Level.OFF, "indicate取消失败：${node.characteristicUUID}"))
        }
    }

    /**
     * 设置设备的传输优先级
     */
    fun setConnectionPriority(bleDevice: BleDevice, connectionPriority: Int) {
        val success = BleManager.get().setConnectionPriority(bleDevice, connectionPriority)
        if (success) {
            addLogMsg(LogEntity(Level.FINE, "设置设备的传输优先级成功：$connectionPriority"))
        } else {
            addLogMsg(LogEntity(Level.OFF, "设置设备的传输优先级失败：$connectionPriority"))
        }
    }

    /**
     * 读取信号值
     */
    fun readRssi(bleDevice: BleDevice) {
        BleManager.get().readRssi(bleDevice) {
            onRssiFail {_, t ->
                addLogMsg(LogEntity(Level.OFF, "读取信号值失败：${t.message}"))
            }
            onRssiSuccess {_, rssi ->
                addLogMsg(LogEntity(Level.FINE, "${bleDevice.deviceAddress} -> 读取信号值成功：${rssi}"))
            }
        }
    }

    /**
     * 设置mtu
     */
    fun setMtu(bleDevice: BleDevice) {
        BleManager.get().setMtu(bleDevice) {
            onSetMtuFail {_, t ->
                addLogMsg(LogEntity(Level.OFF, "设置mtu值失败：${t.message}"))
            }
            onMtuChanged {_, mtu ->
                addLogMsg(LogEntity(Level.FINE, "${bleDevice.deviceAddress} -> 设置mtu值成功：${mtu}"))
            }
        }
    }

    /**
     * 读特征值数据
     */
    fun readData(bleDevice: BleDevice,
                 node: CharacteristicNode
    ) {
        BleManager.get().readData(bleDevice, node.serviceUUID, node.characteristicUUID) {
            onReadFail {_, t ->
                addLogMsg(LogEntity(Level.OFF, "读特征值数据失败：${t.message}"))
            }
            onReadSuccess {_, data ->
                addLogMsg(LogEntity(Level.FINE, "${node.characteristicUUID} -> 读特征值数据成功：${BleUtil.bytesToHex(data)}"))
            }
        }
    }

    /**
     * 写数据
     * 注意：因为分包后每一个包，可能是包含完整的协议，所以分包由业务层处理，组件只会根据包的长度和mtu值对比后是否拦截
     */
    fun writeData(bleDevice: BleDevice,
                  node: CharacteristicNode,
                  text: String) {

        val data = text.toByteArray()
        BleLogger.i("data is: ${BleUtil.bytesToHex(data)}")
        val mtu = BleManager.get().getOptions()?.mtu?: DEFAULT_MTU
        //mtu长度包含了ATT的opcode一个字节以及ATT的handle2个字节
        val maxLength = mtu - 3
        val listData: SparseArray<ByteArray> = BleUtil.subpackage(data, maxLength)
        BleManager.get().writeData(bleDevice, node.serviceUUID, node.characteristicUUID, listData) {
            onWriteFail { _, currentPackage, _, t ->
                addLogMsg(LogEntity(Level.OFF, "第${currentPackage}包数据写失败：${t.message}"))
            }
            onWriteSuccess { _, currentPackage, _, justWrite ->
                addLogMsg(LogEntity(Level.FINE, "${node.characteristicUUID} -> 第${currentPackage}包数据写成功：" +
                        BleUtil.bytesToHex(justWrite)
                ))
            }
            onWriteComplete { _, allSuccess ->
                //代表所有数据写成功，可以在这个方法中处理成功的逻辑
                addLogMsg(LogEntity(Level.FINE, "${node.characteristicUUID} -> 写数据完成，是否成功：$allSuccess"))
            }
        }
    }
}