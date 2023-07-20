/*
 * Copyright (c) 2022-2032 buhuiming
 * 不能修改和删除上面的版权声明
 * 此代码属于buhuiming编写，在未经允许的情况下不得传播复制
 */
package com.bhm.ble.data


/**
 * 用来配置打开或关闭notify、indicate时，获取的描述符(Descriptor)的类型
 * 原理：正常情况下，每个特征值下至少有一个默认描述符，并且遵循蓝牙联盟定义的UUID规则，如
 * [com.bhm.ble.data.Constants.UUID_CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR]便是蓝牙联盟定义的
 * 客户端特性配置的描述符UUID，这样做是方便BLE终端在接入不同类型设备时，能够获取到正确的配置。比如有一个APP，需要
 * 接入A商家的智能手表和B商家的智能手表来监听用户的心跳，而如果A商家的智能手表或者B商家的智能手表不遵循蓝牙联盟定义关于
 * 心跳相关的UUID，则对APP来说就要分别去获取A商家的智能手表或者B商家的智能手表对应特征值的描述符UUID，显然是不合理的。
 * 当然这个是需要硬件设备支持的，也就是说硬件设备可以自定义UUID，但需要遵循规则。
 * 在开发过程中，我们会遇到不同硬件设备定义UUID的情况，有的硬件设备通过特征值的UUID来获取描述符(用来writeDescriptor，
 * 打开或关闭notify、indicate)，而非是通过系统提供接受通知自带的UUID获取描述符。此外特征值有多个描述符时，获取其中
 * 一个描述符来写入数据，可能会导致onCharacteristicChanged函数没有回调，我不确定是否是硬件设备需要支持修改的问题。
 * 因此[AllDescriptor]方式则是简单粗暴的将特征值下所有的描述符都写入数据，以保证onCharacteristicChanged函数回调，
 * 这个方法经过了一系列设备的验证可行，但不保证是完全有效的。
 * @author Buhuiming
 * @date 2023年07月19日 16时07分
 */
sealed class BleDescriptorGetType {

    /**
     * 默认方式，将通过系统提供接受通知自带的UUID获取Descriptor
     * [com.bhm.ble.data.Constants.UUID_CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR]
     */
    object Default : BleDescriptorGetType()

    /**
     * 将通过特征值的UUID获取Descriptor
     */
    object CharacteristicDescriptor : BleDescriptorGetType()

    /**
     * 将获取特征值下所有的Descriptor
     */
    object AllDescriptor : BleDescriptorGetType()
}