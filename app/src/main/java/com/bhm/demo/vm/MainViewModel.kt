package com.bhm.demo.vm

import android.app.Application
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.lifecycle.viewModelScope
import com.bhm.ble.BleManager
import com.bhm.ble.attribute.BleOptions
import com.bhm.ble.callback.BleConnectCallback
import com.bhm.ble.callback.BleScanCallback
import com.bhm.ble.data.BleConnectFailType
import com.bhm.ble.data.BleScanFailType
import com.bhm.ble.device.BleDevice
import com.bhm.ble.log.BleLogger
import com.bhm.ble.utils.BleUtil
import com.bhm.demo.BaseActivity
import com.bhm.demo.constants.LOCATION_PERMISSION
import com.bhm.demo.entity.RefreshBleDevice
import com.bhm.support.sdk.common.BaseViewModel
import com.bhm.support.sdk.entity.MessageEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


/**
 * @author Buhuiming
 * @date 2023年05月18日 10时49分
 */
class MainViewModel(private val application: Application) : BaseViewModel(application) {

    private val listDRMutableStateFlow = MutableStateFlow(
        BleDevice(null, null, null, null, null, null, null)
    )

    val listDRStateFlow: StateFlow<BleDevice> = listDRMutableStateFlow

    val listDRData = mutableListOf<BleDevice>()

    private val scanStopMutableStateFlow = MutableStateFlow(true)

    val scanStopStateFlow: StateFlow<Boolean> = scanStopMutableStateFlow

    private val refreshMutableStateFlow = MutableStateFlow(
        RefreshBleDevice(null, null)
    )

    val refreshStateFlow: StateFlow<RefreshBleDevice?> = refreshMutableStateFlow

    /**
     * 初始化蓝牙组件
     */
    fun initBle() {
        BleManager.get().init(application,
            BleOptions.Builder()
                .setScanMillisTimeOut(5000)
                .setConnectMillisTimeOut(5000)
                //一般不推荐autoSetMtu，因为如果设置的等待时间会影响其他操作
//                .setMtu(100, true)
                .setMaxConnectNum(2)
                .setConnectRetryCountAndInterval(2, 1000)
                .build()
        )
        BleManager.get().registerBluetoothStateReceiver {
            onStateOff {
                refreshMutableStateFlow.value = RefreshBleDevice(null, System.currentTimeMillis())
            }
        }
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
                    try {
                        continuation.resume(true)
                    } catch (e: Exception) {
                        BleLogger.e(e.message)
                    }
                }, {
                    BleLogger.w("缺少定位权限")
                    try {
                        continuation.resume(false)
                    } catch (e: Exception) {
                        BleLogger.e(e.message)
                    }
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
                    try {
                        it.resume(enable)
                    } catch (e: Exception) {
                        BleLogger.e(e.message)
                    }
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
                        try {
                            it.resume(enable)
                        } catch (e: Exception) {
                            BleLogger.e(e.message)
                        }
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
                BleManager.get().startScan(getScanCallback(true))
            } else {
                BleLogger.e("请检查权限、检查GPS开关、检查蓝牙开关")
            }
        }
    }

    private fun getScanCallback(showData: Boolean): BleScanCallback.() -> Unit {
        return {
            onScanStart {
                BleLogger.d("onScanStart")
                scanStopMutableStateFlow.value = false
            }
            onLeScan { bleDevice, _ ->
                //可以根据currentScanCount是否已有清空列表数据
                bleDevice.deviceName?.let { _ ->

                }
            }
            onLeScanDuplicateRemoval { bleDevice, _ ->
                bleDevice.deviceName?.let { _ ->
                    if (showData) {
                        listDRData.add(bleDevice)
                        listDRMutableStateFlow.value = bleDevice
                    }
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
                if (listDRData.isEmpty() && showData) {
                    Toast.makeText(application, "没有扫描到数据", Toast.LENGTH_SHORT).show()
                }
            }
            onScanFail {
                val msg: String = when (it) {
                    is BleScanFailType.UnSupportBle -> "设备不支持蓝牙"
                    is BleScanFailType.NoBlePermission -> "权限不足，请检查"
                    is BleScanFailType.GPSDisable -> "设备未打开GPS定位"
                    is BleScanFailType.BleDisable -> "蓝牙未打开"
                    is BleScanFailType.AlReadyScanning -> "正在扫描"
                    is BleScanFailType.ScanError -> {
                        "${it.throwable?.message}"
                    }
                }
                BleLogger.e(msg)
                Toast.makeText(application, msg, Toast.LENGTH_SHORT).show()
                scanStopMutableStateFlow.value = true
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
    fun connect(address: String) {
        connect(BleManager.get().buildBleDeviceByDeviceAddress(address))
    }

    /**
     * 扫描并连接，如果扫描到多个设备，则会连接第一个
     */
    fun startScanAndConnect(activity: BaseActivity<*, *>) {
        viewModelScope.launch {
            val hasScanPermission = hasScanPermission(activity)
            if (hasScanPermission) {
                BleManager.get().startScanAndConnect(
                    false,
                    getScanCallback(false),
                    connectCallback
                )
            }
        }
    }

    /**
     * 开始连接
     */
    fun connect(bleDevice: BleDevice?) {
        bleDevice?.let { device ->
            BleManager.get().connect(device, false, connectCallback)
        }
    }

    private val connectCallback: BleConnectCallback.() -> Unit = {
        onConnectStart {
            BleLogger.e("-----onConnectStart")
        }
        onConnectFail { bleDevice, connectFailType ->
            val msg: String = when (connectFailType) {
                is BleConnectFailType.UnSupportBle -> "设备不支持蓝牙"
                is BleConnectFailType.NoBlePermission -> "权限不足，请检查"
                is BleConnectFailType.NullableBluetoothDevice -> "设备为空"
                is BleConnectFailType.BleDisable -> "蓝牙未打开"
                is BleConnectFailType.ConnectException -> "连接异常(${connectFailType.throwable.message})"
                is BleConnectFailType.ConnectTimeOut -> "连接超时"
                is BleConnectFailType.AlreadyConnecting -> "连接中"
                is BleConnectFailType.ScanNullableBluetoothDevice -> "连接失败，扫描数据为空"
            }
            BleLogger.e(msg)
            Toast.makeText(application, msg, Toast.LENGTH_SHORT).show()
            refreshMutableStateFlow.value = RefreshBleDevice(bleDevice, System.currentTimeMillis())
        }
        onDisConnecting { isActiveDisConnected, bleDevice, _, _ ->
            BleLogger.e("-----${bleDevice.deviceAddress} -> onDisConnecting: $isActiveDisConnected")
        }
        onDisConnected { isActiveDisConnected, bleDevice, _, _ ->
            Toast.makeText(application, "断开连接(${bleDevice.deviceAddress}，isActiveDisConnected: " +
                    "$isActiveDisConnected)", Toast.LENGTH_SHORT).show()
            BleLogger.e("-----${bleDevice.deviceAddress} -> onDisConnected: $isActiveDisConnected")
            refreshMutableStateFlow.value = RefreshBleDevice(bleDevice, System.currentTimeMillis())
            //发送断开的通知
            val message = MessageEvent()
            message.data = bleDevice
            EventBus.getDefault().post(message)
        }
        onConnectSuccess { bleDevice, _ ->
            Toast.makeText(application, "连接成功(${bleDevice.deviceAddress})", Toast.LENGTH_SHORT).show()
            refreshMutableStateFlow.value = RefreshBleDevice(bleDevice, System.currentTimeMillis())
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

    /**
     * 断开所有连接 释放资源
     */
    fun close() {
        BleManager.get().closeAll()
    }
}