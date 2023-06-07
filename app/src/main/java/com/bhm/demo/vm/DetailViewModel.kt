/*
 * Copyright (c) 2022-2032 buhuiming
 * 不能修改和删除上面的版权声明
 * 此代码属于buhuiming编写，在未经允许的情况下不得传播复制
 */
package com.bhm.demo.vm

import android.app.Application
import android.bluetooth.BluetoothGattCharacteristic
import com.bhm.ble.BleManager
import com.bhm.ble.device.BleDevice
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
                    characteristics.properties
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
        if (charaProp and BluetoothGattCharacteristic.PROPERTY_READ > 0) {
            property.append("Read")
            property.append(" , ")
        }
        if (charaProp and BluetoothGattCharacteristic.PROPERTY_WRITE > 0) {
            property.append("Write")
            property.append(" , ")
        }
        if (charaProp and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE > 0) {
            property.append("Write No Response")
            property.append(" , ")
        }
        if (charaProp and BluetoothGattCharacteristic.PROPERTY_NOTIFY > 0) {
            property.append("Notify")
            property.append(" , ")
        }
        if (charaProp and BluetoothGattCharacteristic.PROPERTY_INDICATE > 0) {
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
               serviceUUID: String,
               notifyUUID: String,
               failCall: () -> Unit) {
        BleManager.get().notify(bleDevice, serviceUUID, notifyUUID, false) {
            onNotifyFail {
                addLogMsg(LogEntity(Level.OFF, "notify失败，${notifyUUID}：${it.message}"))
                failCall.invoke()
            }
            onNotifySuccess {
                addLogMsg(LogEntity(Level.FINE, "notify成功：${notifyUUID}"))
            }
            onCharacteristicChanged {
                addLogMsg(LogEntity(Level.INFO, "接收到${notifyUUID}的数据：${BleUtil.bytesToHex(it)}"))
            }
        }
    }

    /**
     * stop notify
     */
    fun stopNotify(
        bleDevice: BleDevice,
        serviceUUID: String,
        notifyUUID: String,
    ) {
        val success = BleManager.get().stopNotify(bleDevice, serviceUUID, notifyUUID)
        if (success == true) {
            addLogMsg(LogEntity(Level.FINE, "notify取消成功：${notifyUUID}"))
        } else {
            addLogMsg(LogEntity(Level.OFF, "notify取消失败：${notifyUUID}"))
        }
    }

    /**
     * indicate
     */
    fun indicate(bleDevice: BleDevice,
                 serviceUUID: String,
                 indicateUUID: String,
                 failCall: () -> Unit) {
        BleManager.get().indicate(bleDevice, serviceUUID, indicateUUID, false) {
            onIndicateFail {
                addLogMsg(LogEntity(Level.OFF, "indicate失败，${indicateUUID}：${it.message}"))
                failCall.invoke()
            }
            onIndicateSuccess {
                addLogMsg(LogEntity(Level.FINE, "indicate成功：${indicateUUID}"))
            }
            onCharacteristicChanged {
                addLogMsg(LogEntity(Level.INFO, "接收到${indicateUUID}的数据：${BleUtil.bytesToHex(it)}"))
            }
        }
    }

    /**
     * stop indicate
     */
    fun stopIndicate(
        bleDevice: BleDevice,
        serviceUUID: String,
        indicateUUID: String,
    ) {
        val success = BleManager.get().stopIndicate(bleDevice, serviceUUID, indicateUUID)
        if (success == true) {
            addLogMsg(LogEntity(Level.FINE, "indicate取消成功：${indicateUUID}"))
        } else {
            addLogMsg(LogEntity(Level.OFF, "indicate取消失败：${indicateUUID}"))
        }
    }

    /**
     * 读取信号值
     */
    fun readRssi(bleDevice: BleDevice) {
        BleManager.get().readRssi(bleDevice) {
            onRssiFail {
                addLogMsg(LogEntity(Level.OFF, "读取信号值失败：${it.message}"))
            }
            onRssiSuccess {
                addLogMsg(LogEntity(Level.FINE, "读取信号值成功：${it}"))
            }
        }
    }

    /**
     * 设置mtu
     */
    fun setMtu(bleDevice: BleDevice) {
        BleManager.get().setMtu(bleDevice) {
            onSetMtuFail {
                addLogMsg(LogEntity(Level.OFF, "设置mtu值失败：${it.message}"))
            }
            onMtuChanged {
                addLogMsg(LogEntity(Level.FINE, "设置mtu值成功：${it}"))
            }
        }
    }

    /**
     * 读特征值数据
     */
    fun readData(bleDevice: BleDevice,
                 serviceUUID: String,
                 readUUID: String) {
        BleManager.get().readData(bleDevice, serviceUUID, readUUID) {
            onReadFail {
                addLogMsg(LogEntity(Level.OFF, "读特征值数据失败，${readUUID}：${it.message}"))
            }
            onReadSuccess {
                addLogMsg(LogEntity(Level.FINE, "读特征值数据成功，${readUUID}：${BleUtil.bytesToHex(it)}"))
            }
        }
    }
}