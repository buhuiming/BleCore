/*
 * Copyright (c) 2022-2032 buhuiming
 * 不能修改和删除上面的版权声明
 * 此代码属于buhuiming编写，在未经允许的情况下不得传播复制
 */
package com.bhm.demo.entity

import com.chad.library.adapter.base.entity.node.BaseExpandNode
import com.chad.library.adapter.base.entity.node.BaseNode


/**
 * 特征值数据
 * 不能使用data class, 属性写是var，否则BaseNodeAdapter会因为折叠刷新数据闪退
 * @author Buhuiming
 * @date 2023年06月01日 10时16分
 */
class CharacteristicNode(var characteristicName: String,
                         var serviceUUID: String,
                         var characteristicUUID: String,
                         var characteristicProperties: String,
                         var characteristicIntProperties: Int,
                         var enableNotify: Boolean = false,
                         var enableIndicate: Boolean = false,
                         var enableWrite: Boolean = false,
)  : BaseExpandNode() {

    init {
        isExpanded = false
    }

    override val childNode: MutableList<BaseNode>?
        get() = null
}