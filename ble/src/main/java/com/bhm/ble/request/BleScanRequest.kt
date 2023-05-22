/*
 * Copyright (c) 2022-2032 buhuiming
 * 不能修改和删除上面的版权声明
 * 此代码属于buhuiming编写，在未经允许的情况下不得传播复制
 */
package com.bhm.ble.request

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.os.ParcelUuid
import com.bhm.ble.BleManager
import com.bhm.ble.callback.BleScanCallback
import com.bhm.ble.data.BleScanFailType
import com.bhm.ble.utils.BleLogger
import com.bhm.ble.utils.BleUtil
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean


/**
 * Ble扫描
 *
 * @author Buhuiming
 * @date 2023年05月22日 09时49分
 */

internal class BleScanRequest {

    private val isScanning = AtomicBoolean(false)

    /**
     * 开始扫描
     */
    @SuppressLint("MissingPermission")
    fun startScan(bleScanCallback: BleScanCallback) {
        val bleManager = BleManager.get()
        if (!bleManager.isBleSupport()) {
            bleScanCallback.callScanFail(BleScanFailType.UnTypeSupportBle)
            return
        }
        if (!bleManager.isBleEnable()) {
            bleScanCallback.callScanFail(BleScanFailType.BleDisable)
            return
        }
        if (!BleUtil.isPermission(bleManager.getContext()?.applicationContext,
                Manifest.permission.ACCESS_FINE_LOCATION) &&
            !BleUtil.isPermission(bleManager.getContext()?.applicationContext,
                Manifest.permission.ACCESS_COARSE_LOCATION)) {
            bleScanCallback.callScanFail(BleScanFailType.NoBlePermissionType)
            return
        }
        if (isScanning.get()) {
            bleScanCallback.callScanFail(BleScanFailType.AlReadyScanning)
            return
        }
        BleLogger.d("开始扫描")
        bleScanCallback.callStart()
        isScanning.set(true)
        val scanFilters = arrayListOf<ScanFilter>()
        val bleOptions = bleManager.getOptions()
        bleOptions?.let { options ->
            //设置过滤条件-ServiceUuid
            options.scanServiceUuids.forEach { serviceUuid ->
                val scanFilter = ScanFilter.Builder()
                    .setServiceUuid(ParcelUuid(UUID.fromString(serviceUuid)))
                    .build()
                scanFilters.add(scanFilter)
            }
            //设置过滤条件-设备广播名称
            //这里先不过滤，扫描到后再根据containScanDeviceName条件过滤
//            options.scanDeviceNames.forEach { deviceName ->
//                val scanFilter = ScanFilter.Builder()
//                    .setDeviceName(deviceName)
//                    .build()
//                scanFilters.add(scanFilter)
//            }
            options.scanDeviceAddresses.forEach { deviceAddress ->
                val scanFilter = ScanFilter.Builder()
                    .setDeviceAddress(deviceAddress)
                    .build()
                scanFilters.add(scanFilter)
            }
        }
        val scanSetting = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        val scanner = bleManager.getBluetoothManager()?.adapter?.bluetoothLeScanner
        
        scanner?.startScan(scanFilters, scanSetting, object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                super.onScanResult(callbackType, result)
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>?) {
                super.onBatchScanResults(results)
            }

            override fun onScanFailed(errorCode: Int) {
                super.onScanFailed(errorCode)
            }
        })
    }

    /**
     * 是否扫描中
     */
    fun isScanning() = isScanning.get()
}