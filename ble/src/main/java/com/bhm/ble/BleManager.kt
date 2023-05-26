@file:Suppress("SENSELESS_COMPARISON")

package com.bhm.ble

import android.app.Application
import android.bluetooth.BluetoothManager
import android.content.Context
import com.bhm.ble.attribute.BleOptions
import com.bhm.ble.callback.BleBaseRequest
import com.bhm.ble.callback.BleConnectCallback
import com.bhm.ble.callback.BleScanCallback
import com.bhm.ble.data.BleDevice
import com.bhm.ble.request.proxy.BleRequestImp
import com.bhm.ble.request.proxy.BleRequestProxy
import com.bhm.ble.utils.BleLogger
import com.bhm.ble.utils.BleUtil


/**
 * Android蓝牙低功耗核心类
 * @author Buhuiming
 * @date 2023年05月18日 13时37分
 */
class BleManager private constructor() {

    private var application: Application? = null

    private var bleOptions: BleOptions? = null

    private var bluetoothManager: BluetoothManager? = null

    private var bleBaseRequest: BleBaseRequest? = null

    companion object {

        private var instance: BleManager = BleManager()

        @Synchronized
        fun get(): BleManager {
            if (instance == null) {
                instance = BleManager()
            }
            return instance
        }
    }

    /**
     * 初始化，使用BleManager其他方法前，需先调用此方法
     */
    fun init(context: Application, option: BleOptions? = null) {
        application = context
        bleOptions = option
        if (bleOptions == null) {
            bleOptions = BleOptions.getDefaultBleOptions()
        }
        bleBaseRequest = BleRequestProxy.get().bindProxy(BleRequestImp.get()) as BleBaseRequest
        bluetoothManager = application?.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager?
        BleLogger.isLogger = bleOptions?.enableLog?: false
        BleLogger.d("ble Successful initialization")
    }

    /**
     * 设备是否支持蓝牙
     *  @return true = 支持
     */
    fun isBleSupport(): Boolean {
        return BleUtil.isBleSupport(application?.applicationContext)
    }

    /**
     * 蓝牙是否打开
     * @return true = 打开
     */
    fun isBleEnable(): Boolean {
        val bluetoothAdapter = bluetoothManager?.adapter
        return isBleSupport() && (bluetoothAdapter?.isEnabled?: false)
    }

    /**
     * 开始扫描
     */
    @Synchronized
    fun startScan(bleScanCallback: BleScanCallback.() -> Unit) {
        val callback = BleScanCallback()
        callback.apply(bleScanCallback)
        bleBaseRequest?.startScan(callback)
    }

    /**
     * 是否扫描中
     * @return true = 扫描中
     */
    fun isScanning(): Boolean {
        return bleBaseRequest?.isScanning()?: false
    }

    /**
     * 停止扫描
     */
    @Synchronized
    fun stopScan() {
        bleBaseRequest?.stopScan()
    }

    /**
     * 开始连接
     */
    @Synchronized
    fun connect(bleDevice: BleDevice, bleConnectCallback: BleConnectCallback.() -> Unit) {
        stopScan()
        val callback = BleConnectCallback()
        callback.apply(bleConnectCallback)
        bleBaseRequest?.connect(bleDevice, callback)
    }

    /**
     * 断开连接
     */
    @Synchronized
    fun disConnect(bleDevice: BleDevice) {
        bleBaseRequest?.disConnect(bleDevice)
    }

    /**
     * 断开所有连接 释放资源
     */
    @Synchronized
    fun release() {
        bleBaseRequest?.release()
    }


//    @Synchronized
//    fun startScanAndConnect() {
//
//    }

    fun getOptions() = bleOptions

    internal fun getContext() = application

    internal fun getBluetoothManager() = bluetoothManager
}