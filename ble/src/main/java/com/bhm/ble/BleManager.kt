@file:Suppress("SENSELESS_COMPARISON")

package com.bhm.ble

import android.app.Application
import com.bhm.ble.attribute.BleOptions
import com.bhm.ble.utils.BleLogger


/**
 * Android蓝牙低功耗核心类
 * @author Buhuiming
 * @date 2023年05月18日 13时37分
 */
class BleManager private constructor() {

    private var application: Application? = null

    private var bleOptions: BleOptions? = null

    companion object {

        private var instance: BleManager = BleManager()

        @Synchronized
        private fun get(): BleManager {
            if (instance == null) {
                instance = BleManager()
            }
            return instance
        }

        /**
         * 初始化
         */
        fun init(context: Application, option: BleOptions? = null) {
            get().init(context, option)
        }

        fun connect() {
            get()
        }

    }

    /**
     * 初始化
     */
    fun init(context: Application, option: BleOptions? = null) {
        application = context
        bleOptions = option
        if (bleOptions == null) {
            bleOptions = BleOptions.getDefaultBleOptions()
        }
        BleLogger.isLogger = bleOptions?.enableLog?: false
        BleLogger.d("ble Successful initialization")
    }

    fun scan() {

    }
}