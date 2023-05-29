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
import com.bhm.demo.BaseActivity
import com.bhm.demo.constants.LOCATION_PERMISSION
import com.bhm.support.sdk.common.BaseViewModel
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
        BleManager.get().init(application,
            BleOptions.Builder()
                .setScanMillisTimeOut(2000)
                .setConnectMillisTimeOut(4000)
                .setConnectRetryCountAndInterval(1, 2000)
                .build()
        )
    }

    /**
     * 检查权限、检查GPS开关、检查蓝牙开关
     */
    private suspend fun hasScanPermission(activity: BaseActivity<*, *>): Boolean {
        val isBleSupport = BleManager.get().isBleSupport()
        BleLogger.e("设备是否支持蓝牙: $isBleSupport")
        if (!isBleSupport) {
            return false
        }
        var hasScanPermission = suspendCoroutine { continuation ->
            activity.requestPermission(
                LOCATION_PERMISSION,
                {
                    BleLogger.d("获取到了权限")
                    continuation.resume(true)
                }, {
                    BleLogger.w("缺少定位权限")
                    continuation.resume(false)
                }
            )
        }
        //有些设备GPS是关闭状态的话，申请定位权限之后，GPS是依然关闭状态，这里要根据GPS是否打开来跳转页面
        if (hasScanPermission && !BleUtil.isGpsOpen(application)) {
            //跳转到系统GPS设置页面，GPS设置是全局的独立的，是否打开跟权限申请无关
            hasScanPermission = suspendCoroutine {
                activity.startActivity(
                    Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                ) { _, _ ->
                    val enable = BleUtil.isGpsOpen(application)
                    BleLogger.i("是否打开了GPS: $enable")
                    it.resume(enable)
                }
            }
        }
        if (hasScanPermission && !BleManager.get().isBleEnable()) {
            //跳转到系统GPS设置页面，GPS设置是全局的独立的，是否打开跟权限申请无关
            hasScanPermission = suspendCoroutine {
                activity.startActivity(
                    Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                ) { _, _ ->
                    viewModelScope.launch {
                        //打开蓝牙后需要一些时间才能获取到时开启状态，这里延时一下处理
                        delay(1000)
                        val enable = BleManager.get().isBleEnable()
                        BleLogger.i("是否打开了蓝牙: $enable")
                        it.resume(enable)
                    }
                }
            }
        }
        return hasScanPermission
    }

    /**
     * 开始扫描
     */
    fun startScan(activity: BaseActivity<*, *>) {
        viewModelScope.launch {
            val hasScanPermission = hasScanPermission(activity)
            if (hasScanPermission) {
                BleManager.get().startScan {
                    onScanStart {
                        BleLogger.d("onScanStart")
                        scanStopMutableStateFlow.value = false
                    }
                    onLeScan { bleDevice, _ ->
                        //可以根据currentScanCount是否已有清空列表数据
                        bleDevice.deviceName?.let { _ ->
                            listData.add(bleDevice)
                            listMutableStateFlow.value = bleDevice
                        }
                    }
                    onLeScanDuplicateRemoval { bleDevice, _ ->
                        bleDevice.deviceName?.let { _ ->
                            listDRData.add(bleDevice)
                            listDRMutableStateFlow.value = bleDevice
                        }
                    }
                    onScanComplete { bleDeviceList, bleDeviceDuplicateRemovalList ->
                        //扫描到的数据是所有扫描次数的总和
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
                        if (listData.isEmpty()) {
                            Toast.makeText(application, "没有扫描到数据", Toast.LENGTH_SHORT).show()
                        }
                    }
                    onScanFail {
                        val msg: String = when (it) {
                            is BleScanFailType.UnTypeSupportBle -> "BleScanFailType.UnTypeSupportBle: 设备不支持蓝牙"
                            is BleScanFailType.NoBlePermissionType -> "BleScanFailType.NoBlePermissionType: 权限不足，请检查"
                            is BleScanFailType.GPSDisable -> "BleScanFailType.BleDisable: 设备未打开GPS定位"
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
            } else {
                BleLogger.e("请检查权限、检查GPS开关、检查蓝牙开关")
            }
        }
    }

    /**
     * 停止扫描
     */
    fun stopScan() {
        BleManager.get().stopScan()
    }

    /**
     * 是否已连接
     */
    fun isConnected(bleDevice: BleDevice?) = BleManager.get().isConnected(bleDevice)

    /**
     * 开始连接
     */
    fun connect(bleDevice: BleDevice?) {
        bleDevice?.let { device ->
            BleManager.get().connect(device) {

            }
        }
    }

    /**
     * 断开连接
     */
    fun disConnect(bleDevice: BleDevice?) {
        bleDevice?.let { device ->
            BleManager.get().disConnect(device)
        }
    }
}