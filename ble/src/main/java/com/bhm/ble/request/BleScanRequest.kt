/*
 * Copyright (c) 2022-2032 buhuiming
 * 不能修改和删除上面的版权声明
 * 此代码属于buhuiming编写，在未经允许的情况下不得传播复制
 */
package com.bhm.ble.request

import android.annotation.SuppressLint
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.os.ParcelUuid
import com.bhm.ble.BleManager
import com.bhm.ble.attribute.BleOptions.Companion.DEFAULT_SCAN_MILLIS_TIMEOUT
import com.bhm.ble.callback.BleScanCallback
import com.bhm.ble.data.BleDevice
import com.bhm.ble.data.BleScanFailType
import com.bhm.ble.utils.BleLogger
import com.bhm.ble.utils.BleUtil
import kotlinx.coroutines.*
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean


/**
 * Ble扫描
 *
 * @author Buhuiming
 * @date 2023年05月22日 09时49分
 */
@SuppressLint("MissingPermission")
internal class BleScanRequest {

    private val isScanning = AtomicBoolean(false)

    private var scanJob: Job? = null

    private var bleScanCallback: BleScanCallback? = null

    private val duplicateRemovalResults: MutableList<BleDevice> = arrayListOf()

    /**
     * 开始扫描
     */
    fun startScan(bleScanCallback: BleScanCallback) {
        this.bleScanCallback = bleScanCallback
        val bleManager = BleManager.get()
        if (!BleUtil.isPermission(bleManager.getContext()?.applicationContext)) {
            bleScanCallback.callScanFail(BleScanFailType.NoBlePermissionType)
            return
        }
        if (!bleManager.isBleSupport()) {
            bleScanCallback.callScanFail(BleScanFailType.UnTypeSupportBle)
            return
        }
        if (!bleManager.isBleEnable()) {
            bleScanCallback.callScanFail(BleScanFailType.BleDisable)
            return
        }
        if (isScanning.get()) {
            bleScanCallback.callScanFail(BleScanFailType.AlReadyScanning)
            return
        }
        BleLogger.d("开始扫描")
        bleScanCallback.callStart()
        isScanning.set(true)
        duplicateRemovalResults.clear()
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

        scanJob = CoroutineScope(Dispatchers.IO).launch {
            withTimeout(bleOptions?.scanMillisTimeOut?: DEFAULT_SCAN_MILLIS_TIMEOUT) {
                scanner?.startScan(scanFilters, scanSetting, scanCallback)
            }
        }
        scanJob?.invokeOnCompletion {
            isScanning.set(false)
            scanner?.stopScan(scanCallback)
        }
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            result?.let {
                bleScanCallback?.callLeScan(scanResultToBleDevice(it))
            }
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            super.onBatchScanResults(results)
            bleScanCallback?.callScanComplete(scanResultListToBleDeviceList(results), duplicateRemovalResults)
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            // 1、errorCode = [android.bluetooth.le.ScanCallback.SCAN_FAILED_ALREADY_STARTED]
            // 无法启动扫描，因为应用程序已启动具有相同设置的 BLE 扫描。
            // 2、errorCode = [android.bluetooth.le.ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED]
            // 无法开始扫描，因为无法注册应用程序。
            // 3、errorCode = [android.bluetooth.le.ScanCallback.SCAN_FAILED_INTERNAL_ERROR]
            // 由于内部错误无法开始扫描。
            // 4、errorCode = [android.bluetooth.le.ScanCallback.SCAN_FAILED_FEATURE_UNSUPPORTED]
            // 无法启动电源优化扫描，因为不支持此功能。
            // 5、errorCode = [android.bluetooth.le.ScanCallback.SCAN_FAILED_OUT_OF_HARDWARE_RESOURCES]
            // 由于硬件资源不足，无法启动扫描。
            // 6、errorCode = [android.bluetooth.le.ScanCallback.SCAN_FAILED_SCANNING_TOO_FREQUENTLY]
            // 由于应用程序尝试扫描过于频繁，无法开始扫描。
            bleScanCallback?.callScanFail(BleScanFailType.ScanError(errorCode))
        }
    }

    /**
     * ScanResult转BleDevice
     */
    private fun scanResultToBleDevice(scanResult: ScanResult): BleDevice {
        return BleDevice(
            deviceInfo = scanResult.device,
            deviceName = scanResult.device?.name,
            deviceAddress = scanResult.device?.address,
            rssi = scanResult.rssi,
            timestampNanos = scanResult.timestampNanos,
            scanRecord = scanResult.scanRecord
        )
    }

    /**
     * ScanResult集合转BleDevice集合
     */
    private fun scanResultListToBleDeviceList(results: MutableList<ScanResult>?): MutableList<BleDevice> {
        if (results == null) {
            return arrayListOf()
        }
        val newResults: MutableList<BleDevice> = arrayListOf()
        results.forEach {
            newResults.add(scanResultToBleDevice(it))
        }
        return newResults
    }

    /**
     * 是否扫描中
     */
    fun isScanning() = isScanning.get()

    /**
     * 停止扫描
     */
    fun stopScan() {
        scanJob?.cancel()
    }
}