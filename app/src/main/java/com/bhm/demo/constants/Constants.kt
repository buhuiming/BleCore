/*
 * Copyright (c) 2022-2032 buhuiming
 * 不能修改和删除上面的版权声明
 * 此代码属于buhuiming编写，在未经允许的情况下不得传播复制
 */
package com.bhm.demo.constants

import android.Manifest
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES


/**
 * 常量
 *
 * @author Buhuiming
 * @date 2023年05月19日 13时37分
 */
val LOCATION_PERMISSION = if (VERSION.SDK_INT < VERSION_CODES.S) {
    arrayOf(
        //注册精准位置权限，否则可能Ble扫描不到设备
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    )
} else {
    arrayOf(
        //注册精准位置权限，否则可能Ble扫描不到设备
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_ADVERTISE,
        Manifest.permission.BLUETOOTH_CONNECT,
    )
}