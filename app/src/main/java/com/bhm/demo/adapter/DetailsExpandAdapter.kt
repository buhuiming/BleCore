/*
 * Copyright (c) 2022-2032 buhuiming
 * 不能修改和删除上面的版权声明
 * 此代码属于buhuiming编写，在未经允许的情况下不得传播复制
 */
package com.bhm.demo.adapter

import android.bluetooth.BluetoothGattCharacteristic
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import com.bhm.demo.R
import com.bhm.demo.entity.CharacteristicNode
import com.bhm.demo.entity.OperateType
import com.bhm.demo.entity.ServiceNode
import com.bhm.support.sdk.utils.ViewUtil
import com.chad.library.adapter.base.BaseNodeAdapter
import com.chad.library.adapter.base.entity.node.BaseNode
import com.chad.library.adapter.base.provider.BaseNodeProvider
import com.chad.library.adapter.base.viewholder.BaseViewHolder


/**
 * 折叠布局 显示服务 特征值
 *
 * @author Buhuiming
 * @date 2023年06月01日 10时10分
 */
class DetailsExpandAdapter(nodeList: MutableList<BaseNode>,
                           operateCallback: ((checkBox: CheckBox?,
                                              operateType: OperateType,
                                              isChecked: Boolean,
                                              node: CharacteristicNode) -> Unit)? = null
) : BaseNodeAdapter(nodeList) {

    init {
        // 需要占满一行的，使用此方法（例如section）
        addFullSpanNodeProvider(ServiceNodeProvider())
        // 普通的item provider
        addNodeProvider(CharacteristicProvider(operateCallback))
    }

    override fun getItemType(data: List<BaseNode>, position: Int): Int {
        return when (data[position]) {
            is ServiceNode -> 0
            is CharacteristicNode -> 1
            else -> -1
        }
    }

    class ServiceNodeProvider : BaseNodeProvider() {
        override val itemViewType: Int
            get() = 0
        override val layoutId: Int
            get() = R.layout.layout_recycler_service

        override fun convert(helper: BaseViewHolder, item: BaseNode) {
            val node = item as ServiceNode
            helper.setText(R.id.tvServiceName, "服务: (${node.serviceName})")
            helper.setText(R.id.tvServiceUUID, "ServiceUUID: ${node.serviceUUID}")
            helper.setVisible(R.id.ivExpand, node.childNode?.isNotEmpty() == true)
            if (node.isExpanded) {
                helper.setImageResource(R.id.ivExpand, R.drawable.icon_down)
            } else {
                helper.setImageResource(R.id.ivExpand, R.drawable.icon_right)
            }
        }

        override fun onClick(helper: BaseViewHolder, view: View, data: BaseNode, position: Int) {
            super.onClick(helper, view, data, position)
            getAdapter()?.expandOrCollapse(position, animate = true, notify = true)
        }
    }

    class CharacteristicProvider(private val operateCallback: ((checkBox: CheckBox?,
                                                                operateType: OperateType,
                                                                isChecked: Boolean,
                                                                node: CharacteristicNode) -> Unit)? = null
    ) : BaseNodeProvider() {
        override val itemViewType: Int
            get() = 1
        override val layoutId: Int
            get() = R.layout.layout_recycler_characteristic

        override fun convert(helper: BaseViewHolder, item: BaseNode) {
            val node = item as CharacteristicNode
            helper.setText(R.id.tvCharacteristicName, "特征(${node.characteristicName})")
            helper.setText(R.id.tvCharacteristicUUID, "CharacteristicUUID: ${node.characteristicUUID}")
            helper.setText(R.id.tvCharacteristicProperties, "CharacteristicProperties: ${node.characteristicProperties}")
            helper.setGone(R.id.tvCharacteristicProperties, node.characteristicProperties.isEmpty())

            val cbWrite = helper.getView<CheckBox>(R.id.cbWrite)
            val btnReadData = helper.getView<Button>(R.id.btnReadData)
            val cbNotify = helper.getView<CheckBox>(R.id.cbNotify)
            val cbIndicate = helper.getView<CheckBox>(R.id.cbIndicate)
            cbWrite.isChecked = node.enableWrite
            cbNotify.isChecked = node.enableNotify
            cbIndicate.isChecked = node.enableIndicate

            val charaProp: Int = node.characteristicIntProperties
            helper.setGone(R.id.btnReadData, charaProp and BluetoothGattCharacteristic.PROPERTY_READ <= 0)
            helper.setGone(R.id.cbWrite, charaProp and BluetoothGattCharacteristic.PROPERTY_WRITE <= 0 &&
                    charaProp and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE <= 0)
            helper.setGone(R.id.cbNotify, charaProp and BluetoothGattCharacteristic.PROPERTY_NOTIFY <= 0)
            helper.setGone(R.id.cbIndicate, charaProp and BluetoothGattCharacteristic.PROPERTY_INDICATE <= 0)

            cbWrite.setOnClickListener { buttonView ->
                if (ViewUtil.isInvalidClick(buttonView)) {
                    return@setOnClickListener
                }
                node.enableWrite = cbWrite.isChecked
                val isChecked = cbWrite.isChecked
                operateCallback?.invoke(buttonView as CheckBox, OperateType.Write, isChecked, node)
            }
            btnReadData.setOnClickListener {
                if (ViewUtil.isInvalidClick(it)) {
                    return@setOnClickListener
                }
                operateCallback?.invoke(null, OperateType.Read, false, node)
            }
            cbNotify.setOnClickListener { buttonView ->
                if (ViewUtil.isInvalidClick(buttonView)) {
                    return@setOnClickListener
                }
                node.enableNotify = cbNotify.isChecked
                val isChecked = cbNotify.isChecked
                operateCallback?.invoke(buttonView as CheckBox, OperateType.Notify, isChecked, node)
            }
            cbIndicate.setOnClickListener { buttonView ->
                if (ViewUtil.isInvalidClick(buttonView)) {
                    return@setOnClickListener
                }
                node.enableIndicate = cbIndicate.isChecked
                val isChecked = cbIndicate.isChecked
                operateCallback?.invoke(buttonView as CheckBox, OperateType.Indicate, isChecked, node)
            }
        }
    }
}