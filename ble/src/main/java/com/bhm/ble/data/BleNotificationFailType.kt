/*
 * Copyright (c) 2022-2032 buhuiming
 * 不能修改和删除上面的版权声明
 * 此代码属于buhuiming编写，在未经允许的情况下不得传播复制
 */
package com.bhm.ble.data

import kotlinx.coroutines.CancellationException


/**
 * 设置Notify或者Indicate失败类型
 *
 * @author Buhuiming
 * @date 2023年06月07日 11时31分
 */
sealed class BleNotificationFailType(message: String): CancellationException(message) {

    class DescriptorFailType(type: String = "通知")
        : BleNotificationFailType("设置${type}失败，Descriptor写数据失败")

    class UnConnectedFailType(type: String = "通知")
        : BleNotificationFailType("设置${type}失败，设备未连接")

    class UnSupportNotifyFailType(type: String = "通知")
        : BleNotificationFailType("设置${type}失败，此特性不支持通知")

    class SetCharacteristicNotificationFailType(type: String = "通知")
        : BleNotificationFailType("设置${type}失败，SetCharacteristicNotificationFail")

    class TimeoutCancellationFailType(type: String = "通知")
        : BleNotificationFailType("设置${type}失败，设置超时")
}