package com.bhm.demo.vm

import android.app.Application
import android.content.Intent
import android.provider.Settings
import androidx.lifecycle.viewModelScope
import com.bhm.ble.BleManager
import com.bhm.ble.attribute.BleOptions
import com.bhm.ble.data.BleScanFailType
import com.bhm.ble.utils.BleLogger
import com.bhm.ble.utils.BleUtil
import com.bhm.demo.constants.LOCATION_PERMISSION
import com.bhm.support.sdk.common.BaseActivity
import com.bhm.support.sdk.common.BaseViewModel
import com.bhm.support.sdk.interfaces.ARCallBack
import com.bhm.support.sdk.interfaces.PermissionCallBack
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


/**
 * @author Buhuiming
 * @date 2023年05月18日 10时49分
 */
class MainViewModel(private val application: Application) : BaseViewModel(application) {

    /**
     * 初始化蓝牙组件
     */
    fun initBle() {
//     BleManager.init(application)
        val options =
            BleOptions.builder()
                .setScanServiceUuid("0000414b-0000-1000-8000-00805f9b34fb")
                .setScanDeviceName("V8001")
                .setScanDeviceAddress("DC:A1:2F:44:NC")
                .isContainScanDeviceName(true)
                .setAutoConnect(false)
                .setEnableLog(true)
                .setScanMillisTimeOut(12000)
                .setScanRetryCountAndInterval(2, 1000)
                .setConnectMillisTimeOut(10000)
                .setConnectRetryCountAndInterval(2, 5000)
                .setOperateMillisTimeOut(6000)
                .setWriteInterval(80)
                .setMaxConnectNum(5)
                .setMtu(500)
                .build()
        BleManager.get().init(application, options)
    }

    /**
     * 检查权限、检查GPS开关、检查蓝牙开关
     */
    private suspend fun hasScanPermission(activity: BaseActivity<*>): Boolean {
        val isBleSupport = BleManager.get().isBleSupport()
        BleLogger.e("设置是否支持蓝牙: $isBleSupport")
        if (!isBleSupport) {
            return false
        }
        var hasScanPermission = suspendCoroutine {
            activity.requestPermission(LOCATION_PERMISSION, object : PermissionCallBack {
                override fun agree() {
                    BleLogger.d("获取到了权限")
                    it.resume(true)
                }

                override fun refuse(refusePermissions: ArrayList<String>) {
                    BleLogger.w("缺少定位权限")
                    it.resume(false)
                }
            })
        }
        //有些设备GPS是关闭状态的话，申请定位权限之后，GPS是依然关闭状态，这里要根据GPS是否打开来跳转页面
        if (hasScanPermission && !BleUtil.isGpsOpen(application)) {
            //跳转到系统GPS设置页面，GPS设置是全局的独立的，是否打开跟权限申请无关
            hasScanPermission = suspendCoroutine {
                activity.startActivity(
                    Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS),
                    object : ARCallBack {
                        override fun call(resultCode: Int, resultIntent: Intent?) {
                            val enable = BleUtil.isGpsOpen(application)
                            BleLogger.i("是否打开了GPS: $enable")
                            it.resume(enable)
                        }
                    })
            }
        }
        if (hasScanPermission && !BleManager.get().isBleEnable()) {
            //跳转到系统GPS设置页面，GPS设置是全局的独立的，是否打开跟权限申请无关
            hasScanPermission = suspendCoroutine {
                activity.startActivity(
                    Intent(Settings.ACTION_BLUETOOTH_SETTINGS),
                    object : ARCallBack {
                        override fun call(resultCode: Int, resultIntent: Intent?) {
                            viewModelScope.launch {
                                //打开蓝牙后需要一些时间才能获取到时开启状态，这里延时一下处理
                                delay(1000)
                                val enable = BleManager.get().isBleEnable()
                                BleLogger.i("是否打开了蓝牙: $enable")
                                it.resume(enable)
                            }
                        }
                    })
            }
        }
        return hasScanPermission
    }

    /**
     * 开始扫描
     */
    fun startScan(activity: BaseActivity<*>) {
        viewModelScope.launch {
            val hasScanPermission = hasScanPermission(activity)
            if (hasScanPermission) {
                BleManager.get().startScan {
                    onStart {
                        BleLogger.d("onStart")
                    }
                    onLeScan {

                    }
                    onLeScanDuplicateRemoval {

                    }
                    onScanComplete { bleDeviceList, bleDeviceDuplicateRemovalList ->

                    }
                    onScanFail {
                        when (it) {
                            is BleScanFailType.UnTypeSupportBle -> BleLogger.e("设置不支持蓝牙")
                            is BleScanFailType.NoBlePermissionType -> BleLogger.e("权限不足，请检查")
                            is BleScanFailType.BleDisable -> BleLogger.e("蓝牙未打开")
                            is BleScanFailType.AlReadyScanning -> BleLogger.e("正在扫描")
                            is BleScanFailType.ScanError -> BleLogger.e("未知错误")
                        }
                    }
                }
            }
            BleLogger.e("请检查权限、检查GPS开关、检查蓝牙开关")
        }
    }
}