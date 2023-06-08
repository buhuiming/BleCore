/*
 * Copyright (c) 2022-2032 buhuiming
 * 不能修改和删除上面的版权声明
 * 此代码属于buhuiming编写，在未经允许的情况下不得传播复制
 */
package com.bhm.demo.entity

import com.bhm.ble.device.BleDevice


/**
 * @author Buhuiming
 * @date 2023年05月30日 11时57分
 */
data class RefreshBleDevice(
    val bleDevice: BleDevice?,
    var tag: Long? = null
)