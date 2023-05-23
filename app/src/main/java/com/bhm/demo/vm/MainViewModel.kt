package com.bhm.demo.vm

import android.app.Application
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.lifecycle.viewModelScope
import com.bhm.ble.BleManager
import com.bhm.ble.attribute.BleOptions
import com.bhm.ble.data.BleDevice
import com.bhm.ble.data.BleScanFailType
import com.bhm.ble.utils.BleLogger
import com.bhm.ble.utils.BleUtil
import com.bhm.demo.constants.LOCATION_PERMISSION
import com.bhm.support.sdk.common.BaseActivity
import com.bhm.support.sdk.common.BaseViewModel
import com.bhm.support.sdk.interfaces.ARCallBack
import com.bhm.support.sdk.interfaces.PermissionCallBack
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


/**
 * @author Buhuiming
 * @date 2023年05月18日 10时49分
 */
class MainViewModel(private val application: Application) : BaseViewModel(application) {

    private val listMutableStateFlow = MutableStateFlow(BleDevice(null,
        null, null, null, null, null))

    val listStateFlow: StateFlow<BleDevice> = listMutableStateFlow

    val listData = mutableListOf<BleDevice>()

    private val listDRMutableStateFlow = MutableStateFlow(BleDevice(null,
        null, null, null, null, null))

    val listDRStateFlow: StateFlow<BleDevice> = listDRMutableStateFlow

    val listDRData = mutableListOf<BleDevice>()

    private val scanStopMutableStateFlow = MutableStateFlow(true)

    val scanStopStateFlow: StateFlow<Boolean> = scanStopMutableStateFlow

    /**
     * 初始化蓝牙组件
     */
    fun initBle() {
//     BleManager.get().init(application)
        val options =
            BleOptions.builder()
//                .setScanServiceUuid("0000ff90-0000-1000-8000-00805f9b34fb")
//                .setScanServiceUuids(arrayListOf("0000ff80-0000-1000-8000-00805f9b34fb", "0000ff90-0000-1000-8000-00805f9b34fb"))
//                .setScanDeviceName("midea")*
//                .setScanDeviceAddress("70:86:CE:88:7A:AF")
//                .setScanDeviceAddresses(arrayListOf("70:86:CE:88:7A:AF", "5B:AE:65:88:59:5E", "B8:8C:29:8B:BE:07"))
                .isContainScanDeviceName(true)
                .setAutoConnect(false)
                .setEnableLog(true)
                .setScanMillisTimeOut(15000)
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
                        scanStopMutableStateFlow.value = false
                    }
                    onLeScan {
                        it.deviceName?.let { deviceName ->
                            BleLogger.i("onLeScan-> $deviceName, ${it.deviceAddress}")
                            listData.add(it)
                            listMutableStateFlow.value = it
                        }
                    }
                    onLeScanDuplicateRemoval {
                        it.deviceName?.let { deviceName ->
                            BleLogger.e("onLeScanDuplicateRemoval-> $deviceName, ${it.deviceAddress}")
                            listDRData.add(it)
                            listDRMutableStateFlow.value = it
                        }
                    }
                    onScanComplete { bleDeviceList, bleDeviceDuplicateRemovalList ->
                        bleDeviceList.forEach {
                            it.deviceName?.let { deviceName ->
                                BleLogger.i("bleDeviceList-> $deviceName, ${it.deviceAddress}")
                            }
                        }
                        bleDeviceDuplicateRemovalList.forEach {
                            it.deviceName?.let { deviceName ->
                                BleLogger.e("bleDeviceDuplicateRemovalList-> $deviceName, ${it.deviceAddress}")
                            }
                        }
                        scanStopMutableStateFlow.value = true
                        if (bleDeviceList.isEmpty()) {
                            Toast.makeText(application, "没有扫描到数据", Toast.LENGTH_SHORT).show()
                        }
                    }
                    onScanFail {
                        val msg: String = when (it) {
                            is BleScanFailType.UnTypeSupportBle -> "BleScanFailType.UnTypeSupportBle: 设置不支持蓝牙"
                            is BleScanFailType.NoBlePermissionType -> "BleScanFailType.NoBlePermissionType: 权限不足，请检查"
                            is BleScanFailType.BleDisable -> "BleScanFailType.BleDisable: 蓝牙未打开"
                            is BleScanFailType.AlReadyScanning -> "BleScanFailType.AlReadyScanning: 正在扫描"
                            is BleScanFailType.ScanError -> {
                                "BleScanFailType.ScanError: ${it.throwable?.message}"
                            }
                        }
                        BleLogger.e(msg)
                        Toast.makeText(application, msg, Toast.LENGTH_SHORT).show()
                        scanStopMutableStateFlow.value = true
                    }
                }
                return@launch
            }
            BleLogger.e("请检查权限、检查GPS开关、检查蓝牙开关")
        }
    }

    /**
     * 停止扫描
     */
    fun stopScan() {
        BleManager.get().stopScan()
    }
}