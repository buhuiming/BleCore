/*
 * Copyright (c) 2022-2032 buhuiming
 * 不能修改和删除上面的版权声明
 * 此代码属于buhuiming编写，在未经允许的情况下不得传播复制
 */
package com.bhm.demo.adapter

import android.view.View
import com.bhm.demo.R
import com.bhm.demo.entity.CharacteristicNode
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
class DetailsExpandAdapter(nodeList: MutableList<BaseNode>) : BaseNodeAdapter(nodeList) {

    init {
        // 需要占满一行的，使用此方法（例如section）
        addFullSpanNodeProvider(ServiceNodeProvider())
        // 普通的item provider
        addNodeProvider(CharacteristicProvider())
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
        }

        override fun onClick(helper: BaseViewHolder, view: View, data: BaseNode, position: Int) {
            super.onClick(helper, view, data, position)
            getAdapter()?.expandOrCollapse(position, true, true)
        }
    }

    class CharacteristicProvider : BaseNodeProvider() {
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
        }
    }
}