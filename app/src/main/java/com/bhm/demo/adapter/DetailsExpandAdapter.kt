/*
 * Copyright (c) 2022-2032 buhuiming
 * 不能修改和删除上面的版权声明
 * 此代码属于buhuiming编写，在未经允许的情况下不得传播复制
 */
package com.bhm.demo.adapter

import android.view.View
import android.widget.CheckBox
import com.bhm.demo.R
import com.bhm.demo.entity.CharacteristicNode
import com.bhm.demo.entity.OperateType
import com.bhm.demo.entity.ServiceNode
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
                           operateCallback: ((checkBox: CheckBox,
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
            helper.setText(R.id.tvServiceName, node.serviceName)
            helper.setText(R.id.tvServiceUUID, node.serviceUUID)
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

    class CharacteristicProvider(private val operateCallback: ((checkBox: CheckBox,
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
            helper.setText(R.id.tvCharacteristicName, node.characteristicName)
            helper.setText(R.id.tvCharacteristicUUID, node.characteristicUUID)
            helper.setText(R.id.tvCharacteristicProperties, node.characteristicProperties)
            helper.setText(R.id.tvCharacteristicValue, node.characteristicValue)

            val cbWrite = helper.getView<CheckBox>(R.id.cbWrite)
            val cbRead = helper.getView<CheckBox>(R.id.cbRead)
            val cbNotify = helper.getView<CheckBox>(R.id.cbNotify)
            val cbIndicate = helper.getView<CheckBox>(R.id.cbIndicate)
            helper.setVisible(R.id.cbWrite, true)
            helper.setVisible(R.id.cbRead, true)
            helper.setVisible(R.id.cbNotify, true)
            helper.setVisible(R.id.cbIndicate, true)

            cbWrite.setOnCheckedChangeListener { buttonView, isChecked ->
                operateCallback?.invoke(buttonView as CheckBox, OperateType.Write, isChecked, node)
            }
            cbRead.setOnCheckedChangeListener { buttonView, isChecked ->
                operateCallback?.invoke(buttonView as CheckBox, OperateType.Read, isChecked, node)
            }
            cbNotify.setOnCheckedChangeListener { buttonView, isChecked ->
                operateCallback?.invoke(buttonView as CheckBox, OperateType.Notify, isChecked, node)
            }
            cbIndicate.setOnCheckedChangeListener { buttonView, isChecked ->
                operateCallback?.invoke(buttonView as CheckBox, OperateType.Indicate, isChecked, node)
            }
        }
    }
}