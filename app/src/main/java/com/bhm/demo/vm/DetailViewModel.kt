/*
 * Copyright (c) 2022-2032 buhuiming
 * 不能修改和删除上面的版权声明
 * 此代码属于buhuiming编写，在未经允许的情况下不得传播复制
 */
package com.bhm.demo.vm

import android.app.Application
import android.bluetooth.BluetoothGattCharacteristic
import com.bhm.ble.BleManager
import com.bhm.ble.data.BleDevice
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
               notifyUUID: String) {
        BleManager.get().notify(bleDevice, serviceUUID, notifyUUID, false) {
            onNotifyFail {
                addLogMsg(LogEntity(Level.OFF, "notify失败，notifyUUID：${it.message}"))
            }
            onNotifySuccess {
                addLogMsg(LogEntity(Level.FINE, "notify成功，notifyUUID：${notifyUUID}"))
            }
            onCharacteristicChanged {
                addLogMsg(LogEntity(Level.INFO, "接收到${notifyUUID}的数据：$it"))
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
            addLogMsg(LogEntity(Level.FINE, "notify取消成功，notifyUUID：${notifyUUID}"))
        } else {
            addLogMsg(LogEntity(Level.OFF, "notify取消失败，notifyUUID：${notifyUUID}"))
        }
    }
}