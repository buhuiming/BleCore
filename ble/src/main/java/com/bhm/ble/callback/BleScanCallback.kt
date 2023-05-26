/*
 * Copyright (c) 2022-2032 buhuiming
 * 不能修改和删除上面的版权声明
 * 此代码属于buhuiming编写，在未经允许的情况下不得传播复制
 */
package com.bhm.ble.callback

import com.bhm.ble.data.BleDevice
import com.bhm.ble.data.BleScanFailType
import com.bhm.ble.request.BleRequestManager
import kotlinx.coroutines.launch


/**
 * Ble扫描回调
 *
 * @author Buhuiming
 * @date 2023年05月22日 09时08分
 */
class BleScanCallback {

    private var start: (() -> Unit)? = null

    private var leScan: ((bleDevice: BleDevice, currentScanCount: Int) -> Unit)? = null

    //相同设备只会出现一次
    private var leScanDuplicateRemoval: ((bleDevice: BleDevice, currentScanCount: Int) -> Unit)? = null

    private var scanFail: ((scanFailType: BleScanFailType) -> Unit)? = null

    private var scanComplete: ((bleDeviceList: MutableList<BleDevice>,
                                bleDeviceDuplicateRemovalList: MutableList<BleDevice>) -> Unit)? = null

    private val mainScope = BleRequestManager.get().getMainScope()

    /**
     * 扫描开始
     */
    fun onScanStart(value: () -> Unit) {
        start = value
    }

    /**
     * 扫描过程中所有被扫描到的结果回调(同一个设备会在不同的时间，携带自身不同的状态（比如信号强度等），
     * 出现在这个回调方法中，出现次数取决于周围的设备量及外围设备的广播间隔。)
     */
    fun onLeScan(value: (bleDevice: BleDevice, currentScanCount: Int) -> Unit) {
        leScan = value
    }

    /**
     * 扫描过程中的所有过滤后的结果回调。与onLeScan区别之处在于：同一个设备只会出现一次；
     * 出现的设备是经过扫描过滤规则过滤后的设备。
     */
    fun onLeScanDuplicateRemoval(value: (bleDevice: BleDevice, currentScanCount: Int) -> Unit) {
        leScanDuplicateRemoval = value
    }

    /**
     * 扫描失败
     */
    fun onScanFail(value: (scanFailType: BleScanFailType) -> Unit) {
        scanFail = value
    }

    /**
     * 扫描完成
     * bleDeviceList [onLeScan]扫描到的设备之和
     * bleDeviceDuplicateRemovalList [onLeScanDuplicateRemoval]扫描到的设备之和
     */
    fun onScanComplete(value: (bleDeviceList: MutableList<BleDevice>,
                               bleDeviceDuplicateRemovalList: MutableList<BleDevice>) -> Unit) {
        scanComplete = value
    }

    internal fun callScanStart() {
        //MainScope是CoroutineScope类型，为协同作用域，子协程取消后，父协程也会取消
        mainScope.launch {
            start?.invoke()
        }
    }

    internal fun callLeScan(bleDevice: BleDevice, currentScanCount: Int) {
        mainScope.launch {
            leScan?.invoke(bleDevice, currentScanCount)
        }
    }

    internal fun callLeScanDuplicateRemoval(bleDevice: BleDevice, currentScanCount: Int) {
        mainScope.launch {
            leScanDuplicateRemoval?.invoke(bleDevice, currentScanCount)
        }
    }

    internal fun callScanFail(scanFailType: BleScanFailType) {
        mainScope.launch {
            scanFail?.invoke(scanFailType)
        }
    }

    internal fun callScanComplete(bleDeviceList: MutableList<BleDevice>,
                                  bleDeviceDuplicateRemovalList: MutableList<BleDevice>) {
        mainScope.launch {
            scanComplete?.invoke(bleDeviceList, bleDeviceDuplicateRemovalList)
        }
    }
}