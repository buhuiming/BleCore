@file:Suppress("SENSELESS_COMPARISON")

package com.bhm.ble

import android.app.Application
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import com.bhm.ble.attribute.BleOptions
import com.bhm.ble.callback.BleBaseRequest
import com.bhm.ble.callback.BleScanCallback
import com.bhm.ble.proxy.BleRequestImp
import com.bhm.ble.proxy.BleRequestProxy
import com.bhm.ble.utils.BleLogger


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
     * 蓝牙是否打开
     */
    fun isBleSupport(): Boolean {
        return application?.packageManager?.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)?: false
    }

    /**
     * 蓝牙是否打开
     */
    fun isBleEnable(): Boolean {
        val bluetoothAdapter = bluetoothManager?.adapter
        return isBleSupport() && (bluetoothAdapter?.isEnabled?: false)
    }

    fun startScan(bleScanCallback: BleScanCallback.() -> Unit) {
        val callback = BleScanCallback()
        callback.apply(bleScanCallback)
        bleBaseRequest?.startScan(callback)
    }
}