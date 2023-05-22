/*
 * Copyright (c) 2022-2032 buhuiming
 * 不能修改和删除上面的版权声明
 * 此代码属于buhuiming编写，在未经允许的情况下不得传播复制
 */
package com.bhm.ble.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import com.bhm.ble.data.BleScanFailType


/**
 * 工具类
 *
 * @author Buhuiming
 * @date 2023年05月19日 14时04分
 */
object BleUtil {

    /**
     * 系统GPS是否打开
     * @return true = 打开
     */
    fun isGpsOpen(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
    }

    /**
     * 判断是否拥有[permission]权限
     * @return true = 拥有该权限
     */
    private fun isPermission(context: Context?, permission: String): Boolean {
        return context?.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * 判断是否拥有蓝牙权限
     * @return true = 拥有该权限
     */
    fun isPermission(context: Context?): Boolean {
        if (isPermission(context?.applicationContext,
                Manifest.permission.ACCESS_FINE_LOCATION) &&
            isPermission(context?.applicationContext,
                Manifest.permission.ACCESS_COARSE_LOCATION)) {
            return true
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            isPermission(context?.applicationContext,
                Manifest.permission.BLUETOOTH_SCAN) &&
            isPermission(context?.applicationContext,
                Manifest.permission.BLUETOOTH_ADVERTISE) &&
            isPermission(context?.applicationContext,
                Manifest.permission.BLUETOOTH_CONNECT)
        ) {
            return true
        }
        return false
    }
}