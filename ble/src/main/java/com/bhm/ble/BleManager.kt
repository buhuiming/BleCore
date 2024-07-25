@file:Suppress("SENSELESS_COMPARISON", "unused")

package com.bhm.ble

import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.util.SparseArray
import com.bhm.ble.attribute.BleOptions
import com.bhm.ble.callback.*
import com.bhm.ble.data.BleDescriptorGetType
import com.bhm.ble.data.Constants.DEFAULT_MTU
import com.bhm.ble.device.BleDevice
import com.bhm.ble.request.base.BleBaseRequest
import com.bhm.ble.request.base.BleRequestImp
import com.bhm.ble.log.BleLogger
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
        bleBaseRequest = BleRequestImp.get()
        bluetoothManager = application?.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager?
        BleLogger.isLogger = bleOptions?.enableLog?: false
        BleLogger.i("ble Successful initialization")
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
     * @param scanMillisTimeOut 扫描超时时间，单位毫秒，只对单次扫描有效
     * @param scanRetryCount 设置扫描重试次数，只对单次扫描有效
     * @param scanRetryInterval 设置扫描重试间隔，单位毫秒，只对单次扫描有效
     */
    @Synchronized
    fun startScan(
        scanMillisTimeOut: Long?,
        scanRetryCount: Int?,
        scanRetryInterval: Long?,
        bleScanCallback: BleScanCallback.() -> Unit
    ) {
        checkInitialize()
        bleBaseRequest?.startScan(
            scanMillisTimeOut,
            scanRetryCount,
            scanRetryInterval,
            bleScanCallback
        )
    }

    /**
     * 开始扫描
     */
    fun startScan(
        bleScanCallback: BleScanCallback.() -> Unit
    ) {
        startScan(null, null, null, bleScanCallback)
    }

    /**
     * 是否扫描中
     * @return true = 扫描中
     */
    fun isScanning(): Boolean {
        checkInitialize()
        return bleBaseRequest?.isScanning()?: false
    }

    /**
     * 停止扫描
     */
    @Synchronized
    fun stopScan() {
        checkInitialize()
        bleBaseRequest?.stopScan()
    }

    /**
     * 是否已连接，确保已获取到权限
     *
     * 操作断开连接后，getConnectionState马上回去到的状态还是连接状态，所以需要bleBaseRequest?.isConnected判断
     *  @param simplySystemStatus 为true，只根据系统的状态规则；为false，会根据sdk的状态；
     *  此字段的意义在于：有时，sdk资源被系统回收(状态未连接)，但是系统的状态是已连接。
     */
    @SuppressLint("MissingPermission")
    fun isConnected(bleDeviceAddress: String, simplySystemStatus: Boolean = true): Boolean {
        return isConnected(buildBleDeviceByDeviceAddress(bleDeviceAddress), simplySystemStatus)
    }

    /**
     * 是否已连接，确保已获取到权限
     */
    @SuppressLint("MissingPermission")
    fun isConnected(bleDevice: BleDevice?, simplySystemStatus: Boolean = true): Boolean {
        checkInitialize()
        if (!BleUtil.isPermission(application)) {
            return false
        }
        bleDevice?.let {
            val connectedDevices: List<BluetoothDevice>? = bluetoothManager?.getConnectedDevices(BluetoothProfile.GATT)
            if (connectedDevices.isNullOrEmpty()) {
                return false
            }
            for (connectedDevice in connectedDevices) {
                if (it.deviceAddress == connectedDevice.address) {
                    return if (simplySystemStatus) {
                        true
                    } else {
                        bleBaseRequest?.isConnected(it) == true
                    }
                }
            }
        }
        return false
    }

    /**
     * 连接
     * @param connectMillisTimeOut 连接超时时间，单位毫秒，只对单次连接有效
     * @param connectRetryCount 设置连接重试次数，只对单次连接有效
     * @param connectRetryInterval 设置连接重试间隔，只对单次连接有效
     * @param isForceConnect 是否强制连接(针对已连接情况，是否重连)
     */
    @Synchronized
    fun connect(bleDevice: BleDevice,
                connectMillisTimeOut: Long?,
                connectRetryCount: Int?,
                connectRetryInterval: Long?,
                isForceConnect: Boolean = false,
                bleConnectCallback: BleConnectCallback.() -> Unit
    ) {
        checkInitialize()
        stopScan()
        bleBaseRequest?.connect(
            bleDevice,
            connectMillisTimeOut,
            connectRetryCount,
            connectRetryInterval,
            isForceConnect,
            bleConnectCallback
        )
    }

    /**
     * 通过地址连接
     * @param connectMillisTimeOut 连接超时时间，单位毫秒，只对单次连接有效
     * @param connectRetryCount 设置连接重试次数，只对单次连接有效
     * @param connectRetryInterval 设置连接重试间隔，单位毫秒，只对单次连接有效
     */
    fun connect(address: String,
                connectMillisTimeOut: Long?,
                connectRetryCount: Int?,
                connectRetryInterval: Long?,
                isForceConnect: Boolean = false,
                bleConnectCallback: BleConnectCallback.() -> Unit
    ) {
        connect(
            buildBleDeviceByDeviceAddress(address),
            connectMillisTimeOut,
            connectRetryCount,
            connectRetryInterval,
            isForceConnect,
            bleConnectCallback
        )
    }

    /**
     * 连接
     */
    fun connect(
        bleDevice: BleDevice,
        isForceConnect: Boolean = false,
        bleConnectCallback: BleConnectCallback.() -> Unit
    ) {
        connect(
            bleDevice,
            null,
            null,
            null,
            isForceConnect,
            bleConnectCallback
        )
    }

    /**
     * 通过地址连接
     */
    fun connect(
        address: String,
        isForceConnect: Boolean = false,
        bleConnectCallback: BleConnectCallback.() -> Unit
    ) {
        connect(
            address,
            null,
            null,
            null,
            isForceConnect,
            bleConnectCallback
        )
    }

    @Synchronized
    fun startScanAndConnect(scanMillisTimeOut: Long?,
                            scanRetryCount: Int?,
                            scanRetryInterval: Long?,
                            connectMillisTimeOut: Long?,
                            connectRetryCount: Int?,
                            connectRetryInterval: Long?,
                            isForceConnect: Boolean = false,
                            bleScanCallback: BleScanCallback.() -> Unit,
                            bleConnectCallback: BleConnectCallback.() -> Unit) {
        checkInitialize()
        bleBaseRequest?.startScanAndConnect(
            scanMillisTimeOut,
            scanRetryCount,
            scanRetryInterval,
            connectMillisTimeOut,
            connectRetryCount,
            connectRetryInterval,
            isForceConnect,
            bleScanCallback,
            bleConnectCallback
        )
    }

    fun startScanAndConnect(
        isForceConnect: Boolean = false,
        bleScanCallback: BleScanCallback.() -> Unit,
        bleConnectCallback: BleConnectCallback.() -> Unit
    ) {
        startScanAndConnect(
            null,
            null,
            null,
            null,
            null,
            null,
            isForceConnect,
            bleScanCallback,
            bleConnectCallback
        )
    }

    /**
     * 取消/停止连接
     */
    @Synchronized
    fun stopConnect(bleDevice: BleDevice?) {
        checkInitialize()
        bleDevice?.let {
            bleBaseRequest?.stopConnect(it)
        }
    }

    /**
     * 断开连接
     */
    @Synchronized
    fun disConnect(bleDevice: BleDevice) {
        checkInitialize()
        bleBaseRequest?.disConnect(bleDevice)
    }

    /**
     * 通过地址断开连接
     */
    fun disConnect(address: String) {
        disConnect(buildBleDeviceByDeviceAddress(address))
    }

    /**
     * 获取设备的BluetoothGatt对象
     */
    @Synchronized
    fun getBluetoothGatt(bleDevice: BleDevice): BluetoothGatt? {
        checkInitialize()
        return bleBaseRequest?.getBluetoothGatt(bleDevice)
    }

    /**
     * notify
     */
    @Deprecated(message = "请使用BleDescriptorGetType参数方式",
        replaceWith = ReplaceWith(
            "notify(BleDevice, String, String, BleDescriptorGetType, BleNotifyCallback)"
        )
    )
    fun notify(bleDevice: BleDevice,
               serviceUUID: String,
               notifyUUID: String,
               useCharacteristicDescriptor: Boolean = false,
               bleNotifyCallback: BleNotifyCallback.() -> Unit) {
        notify(
            bleDevice = bleDevice,
            serviceUUID = serviceUUID,
            notifyUUID = notifyUUID,
            bleDescriptorGetType = if (useCharacteristicDescriptor) {
                BleDescriptorGetType.CharacteristicDescriptor
            } else {
                BleDescriptorGetType.Default
            },
            bleNotifyCallback = bleNotifyCallback,
        )
    }

    /**
     * notify
     */
    fun notify(bleDevice: BleDevice,
               serviceUUID: String,
               notifyUUID: String,
               bleDescriptorGetType: BleDescriptorGetType = BleDescriptorGetType.Default,
               bleNotifyCallback: BleNotifyCallback.() -> Unit) {
        checkInitialize()
        bleBaseRequest?.notify(
            bleDevice,
            serviceUUID,
            notifyUUID,
            bleDescriptorGetType,
            bleNotifyCallback
        )
    }

    /**
     * stop notify
     */
    @Deprecated(message = "请使用BleDescriptorGetType参数方式",
        replaceWith = ReplaceWith(
            "stopNotify(BleDevice, String, String, BleDescriptorGetType)"
        )
    )
    fun stopNotify(
        bleDevice: BleDevice,
        serviceUUID: String,
        notifyUUID: String,
        useCharacteristicDescriptor: Boolean = false
    ): Boolean? {
        return stopNotify(
            bleDevice = bleDevice,
            serviceUUID = serviceUUID,
            notifyUUID = notifyUUID,
            bleDescriptorGetType = if (useCharacteristicDescriptor) {
                BleDescriptorGetType.CharacteristicDescriptor
            } else {
                BleDescriptorGetType.Default
            },
        )
    }

    /**
     * stop notify
     */
    fun stopNotify(
        bleDevice: BleDevice,
        serviceUUID: String,
        notifyUUID: String,
        bleDescriptorGetType: BleDescriptorGetType = BleDescriptorGetType.Default,
    ): Boolean? {
        checkInitialize()
        return bleBaseRequest?.stopNotify(
            bleDevice,
            serviceUUID,
            notifyUUID,
            bleDescriptorGetType
        )
    }

    /**
     * indicate
     */
    @Deprecated(message = "请使用BleDescriptorGetType参数方式",
        replaceWith = ReplaceWith(
            "indicate(BleDevice, String, String, BleDescriptorGetType, BleIndicateCallback)"
        )
    )
    fun indicate(bleDevice: BleDevice,
                 serviceUUID: String,
                 indicateUUID: String,
                 useCharacteristicDescriptor: Boolean = false,
                 bleIndicateCallback: BleIndicateCallback.() -> Unit) {
        indicate(
            bleDevice = bleDevice,
            serviceUUID = serviceUUID,
            indicateUUID = indicateUUID,
            bleDescriptorGetType = if (useCharacteristicDescriptor) {
                BleDescriptorGetType.CharacteristicDescriptor
            } else {
                BleDescriptorGetType.Default
            },
            bleIndicateCallback = bleIndicateCallback
        )
    }

    /**
     * indicate
     */
    fun indicate(bleDevice: BleDevice,
                 serviceUUID: String,
                 indicateUUID: String,
                 bleDescriptorGetType: BleDescriptorGetType = BleDescriptorGetType.Default,
                 bleIndicateCallback: BleIndicateCallback.() -> Unit) {
        checkInitialize()
        bleBaseRequest?.indicate(
            bleDevice,
            serviceUUID,
            indicateUUID,
            bleDescriptorGetType,
            bleIndicateCallback
        )
    }

    /**
     * stop indicate
     */
    @Deprecated(message = "请使用BleDescriptorGetType参数方式",
        replaceWith = ReplaceWith(
            "stopIndicate(BleDevice, String, String, BleDescriptorGetType)"
        )
    )
    fun stopIndicate(
        bleDevice: BleDevice,
        serviceUUID: String,
        indicateUUID: String,
        useCharacteristicDescriptor: Boolean = false
    ): Boolean? {
        return stopIndicate(
            bleDevice = bleDevice,
            serviceUUID = serviceUUID,
            indicateUUID = indicateUUID,
            bleDescriptorGetType = if (useCharacteristicDescriptor) {
                BleDescriptorGetType.CharacteristicDescriptor
            } else {
                BleDescriptorGetType.Default
            },
        )
    }

    /**
     * stop indicate
     */
    fun stopIndicate(
        bleDevice: BleDevice,
        serviceUUID: String,
        indicateUUID: String,
        bleDescriptorGetType: BleDescriptorGetType = BleDescriptorGetType.Default
    ): Boolean? {
        checkInitialize()
        return bleBaseRequest?.stopIndicate(
            bleDevice,
            serviceUUID,
            indicateUUID,
            bleDescriptorGetType
        )
    }

    /**
     * 读取信号值
     */
    fun readRssi(bleDevice: BleDevice, bleRssiCallback: BleRssiCallback.() -> Unit) {
        checkInitialize()
        bleBaseRequest?.readRssi(bleDevice, bleRssiCallback)
    }

    /**
     * 设置mtu
     */
    fun setMtu(bleDevice: BleDevice, bleMtuChangedCallback: BleMtuChangedCallback.() -> Unit) {
        setMtu(bleDevice, getOptions()?.mtu?: DEFAULT_MTU, bleMtuChangedCallback)
    }

    /**
     * 设置mtu
     */
    fun setMtu(bleDevice: BleDevice, mtu: Int, bleMtuChangedCallback: BleMtuChangedCallback.() -> Unit) {
        checkInitialize()
        if (mtu > 512) {
            BleLogger.w("requiredMtu should lower than 512 !")
        }

        if (mtu < 23) {
            BleLogger.w("requiredMtu should higher than 23 !")
        }
        bleBaseRequest?.setMtu(bleDevice, mtu, bleMtuChangedCallback)
    }

    /**
     * 设置设备的传输优先级
     * connectionPriority 必须是以下的其中一个
     * [BluetoothGatt.CONNECTION_PRIORITY_BALANCED] (默认)、
     * [BluetoothGatt.CONNECTION_PRIORITY_HIGH] (高优先级，低延迟，传输完请求设置
     * CONNECTION_PRIORITY_BALANCED，以减少能源使用)、
     * [BluetoothGatt.CONNECTION_PRIORITY_LOW_POWER] (低功耗)
     *
     */
    fun setConnectionPriority(bleDevice: BleDevice, connectionPriority: Int): Boolean {
        checkInitialize()
        if (connectionPriority != BluetoothGatt.CONNECTION_PRIORITY_BALANCED &&
            connectionPriority != BluetoothGatt.CONNECTION_PRIORITY_HIGH &&
            connectionPriority != BluetoothGatt.CONNECTION_PRIORITY_LOW_POWER) {
            return false
        }
        return bleBaseRequest?.setConnectionPriority(bleDevice, connectionPriority)?: false
    }

    /**
     * 读特征值数据
     */
    fun readData(bleDevice: BleDevice,
                 serviceUUID: String,
                 readUUID: String,
                 bleReadCallback: BleReadCallback.() -> Unit) {
        checkInitialize()
        bleBaseRequest?.readData(bleDevice, serviceUUID, readUUID, bleReadCallback)
    }

    /**
     * 写数据
     * 注意：因为分包后每一个包，可能是包含完整的协议，所以分包由业务层处理，组件只会根据包的长度和mtu值对比后是否拦截
     */
    fun writeData(bleDevice: BleDevice,
                  serviceUUID: String,
                  writeUUID: String,
                  data: ByteArray,
                  writeType: Int? = null,
                  bleWriteCallback: BleWriteCallback.() -> Unit) {
        writeData(
            bleDevice,
            serviceUUID,
            writeUUID,
            SparseArray<ByteArray>(1).apply {
                put(0, data)
            },
            writeType,
            bleWriteCallback
        )
    }

    /**
     * 写数据
     * 注意：因为分包后每一个包，可能是包含完整的协议，所以分包由业务层处理，组件只会根据包的长度和mtu值对比后是否拦截
     */
    fun writeData(bleDevice: BleDevice,
                  serviceUUID: String,
                  writeUUID: String,
                  dataArray: SparseArray<ByteArray>,
                  writeType: Int? = null,
                  bleWriteCallback: BleWriteCallback.() -> Unit) {
        checkInitialize()
        bleBaseRequest?.writeData(bleDevice, serviceUUID, writeUUID, dataArray, writeType, bleWriteCallback)
    }

    /**
     * OTA推荐此方法
     * 放入一个写队列，写成功，则从队列中取下一个数据，写失败，则重试[retryWriteCount]次
     * 与[writeData]的区别在于，[writeData]写成功，则从队列中取下一个数据，写失败，则不再继续写后面的数据
     * 注意：因为分包后每一个包，可能是包含完整的协议，所以分包由业务层处理，组件只会根据包的长度和mtu值对比后是否拦截
     *
     * @param skipErrorPacketData 是否跳过数据长度为0的数据包
     * @param retryWriteCount 写失败后重试的次数
     */
    fun writeQueueData(bleDevice: BleDevice,
                       serviceUUID: String,
                       writeUUID: String,
                       data: ByteArray,
                       skipErrorPacketData: Boolean = false,
                       retryWriteCount: Int = 0,
                       retryDelayTime: Long = 0L,
                       writeType: Int? = null,
                       bleWriteCallback: BleWriteCallback.() -> Unit) {
        writeQueueData(
            bleDevice,
            serviceUUID,
            writeUUID,
            SparseArray<ByteArray>(1).apply {
                put(0, data)
            },
            skipErrorPacketData,
            retryWriteCount,
            retryDelayTime,
            writeType,
            bleWriteCallback
        )
    }

    /**
     * OTA推荐此方法
     * 放入一个写队列，写成功，则从队列中取下一个数据，写失败，则重试[retryWriteCount]次
     * 与[writeData]的区别在于，[writeData]写成功，则从队列中取下一个数据，写失败，则不再继续写后面的数据
     * 注意：因为分包后每一个包，可能是包含完整的协议，所以分包由业务层处理，组件只会根据包的长度和mtu值对比后是否拦截
     *
     * @param skipErrorPacketData 是否跳过数据长度为0的数据包
     * @param retryWriteCount 写失败后重试的次数
     */
    fun writeQueueData(bleDevice: BleDevice,
                       serviceUUID: String,
                       writeUUID: String,
                       dataArray: SparseArray<ByteArray>,
                       skipErrorPacketData: Boolean = false,
                       retryWriteCount: Int = 0,
                       retryDelayTime: Long = 0L,
                       writeType: Int? = null,
                       bleWriteCallback: BleWriteCallback.() -> Unit) {
        checkInitialize()
        bleBaseRequest?.writeQueueData(
            bleDevice,
            serviceUUID,
            writeUUID,
            dataArray,
            skipErrorPacketData,
            retryWriteCount,
            retryDelayTime,
            writeType,
            bleWriteCallback
        )
    }

    /**
     * 获取所有已连接设备集合(不包含其他应用连接的设备、系统连接的设备)
     */
    fun getAllConnectedDevice(): MutableList<BleDevice>? {
        checkInitialize()
        return bleBaseRequest?.getAllConnectedDevice()
    }

    /**
     * 获取系统已连接设备集合，确保已获取到权限
     */
    @SuppressLint("MissingPermission")
    fun getSystemAllConnectedDevice(): MutableList<BluetoothDevice>? {
        checkInitialize()
        if (!BleUtil.isPermission(application)) {
            return null
        }
        return bluetoothManager?.getConnectedDevices(BluetoothProfile.GATT)
    }

    /**
     * 添加设备的连接状态发生变化、indicate/notify收到数据、mtu改变的回调
     *  这个回调会独立存在，与[connect]的bleConnectCallback、[notify]的bleNotifyCallback、
     *  [indicate]的bleIndicateCallback、[setMtu]的bleMtuChangedCallback不冲突
     */
    fun addBleEventCallback(bleDevice: BleDevice, bleEventCallback: BleEventCallback.() -> Unit) {
        checkInitialize()
        bleBaseRequest?.addBleEventCallback(bleDevice, bleEventCallback)
    }

    /**
     * 移除该设备的连接回调
     */
    fun removeBleConnectCallback(bleDevice: BleDevice) {
        checkInitialize()
        bleBaseRequest?.removeBleConnectCallback(bleDevice)
    }

    /**
     * 替换该设备的连接回调
     */
    fun replaceBleConnectCallback(bleDevice: BleDevice, bleConnectCallback: BleConnectCallback.() -> Unit) {
        checkInitialize()
        bleBaseRequest?.replaceBleConnectCallback(bleDevice, bleConnectCallback)
    }

    /**
     * 替换该设备的连接回调
     */
    fun replaceBleConnectCallback(address: String, bleConnectCallback: BleConnectCallback.() -> Unit) {
        replaceBleConnectCallback(buildBleDeviceByDeviceAddress(address), bleConnectCallback)
    }

    /**
     * 移除该设备的Indicate回调
     */
    fun removeBleIndicateCallback(bleDevice: BleDevice, indicateUUID: String) {
        checkInitialize()
        bleBaseRequest?.removeBleIndicateCallback(bleDevice, indicateUUID)
    }

    /**
     * 移除该设备的Notify回调
     */
    fun removeBleNotifyCallback(bleDevice: BleDevice, notifyUUID: String) {
        checkInitialize()
        bleBaseRequest?.removeBleNotifyCallback(bleDevice, notifyUUID)
    }

    /**
     * 移除该设备的Read回调
     */
    fun removeBleReadCallback(bleDevice: BleDevice, readUUID: String) {
        checkInitialize()
        bleBaseRequest?.removeBleReadCallback(bleDevice, readUUID)
    }

    /**
     * 移除该设备的MtuChanged回调
     */
    fun removeBleMtuChangedCallback(bleDevice: BleDevice) {
        checkInitialize()
        bleBaseRequest?.removeBleMtuChangedCallback(bleDevice)
    }

    /**
     * 移除该设备的Rssi回调
     */
    fun removeBleRssiCallback(bleDevice: BleDevice) {
        checkInitialize()
        bleBaseRequest?.removeBleRssiCallback(bleDevice)
    }

    /**
     * 移除该设备的Write回调
     * bleWriteCallback为空，则会移除writeUUID下的所有callback
     */
    fun removeBleWriteCallback(bleDevice: BleDevice,
                               writeUUID: String,
                               bleWriteCallback: BleWriteCallback? = null
    ) {
        checkInitialize()
        bleBaseRequest?.removeBleWriteCallback(bleDevice, writeUUID, bleWriteCallback)
    }

    /**
     * 移除该设备的Scan回调
     */
    @Synchronized
    fun removeBleScanCallback() {
        checkInitialize()
        bleBaseRequest?.removeBleScanCallback()
    }

    /**
     * 移除该设备回调，BleConnectCallback除外
     */
    fun removeAllCharacterCallback(bleDevice: BleDevice) {
        checkInitialize()
        bleBaseRequest?.removeAllCharacterCallback(bleDevice)
    }

    /**
     * 移除该设备Event回调
     */
    fun removeBleEventCallback(bleDevice: BleDevice) {
        checkInitialize()
        bleBaseRequest?.removeBleEventCallback(bleDevice)
    }

    /**
     * 断开所有设备的连接，先回调状态，再close
     */
    @Synchronized
    fun disConnectAll() {
        checkInitialize()
        bleBaseRequest?.disConnectAll()
    }

    /**
     * 断开所有连接 释放资源
     */
    @Synchronized
    fun closeAll() {
        checkInitialize()
        bleBaseRequest?.closeAll()
        application = null
        bleOptions = null
        bluetoothManager = null
        bleBaseRequest = null
        BleLogger.i("资源释放完毕，BleCore SDK退出")
    }

    /**
     * 注册系统蓝牙广播
     */
    fun registerBluetoothStateReceiver(bluetoothCallback: BluetoothCallback.() -> Unit) {
        checkInitialize()
        bleBaseRequest?.registerBluetoothStateReceiver(bluetoothCallback)
    }

    /**
     * 取消注册系统蓝牙广播
     */
    fun unRegisterBluetoothStateReceiver() {
        checkInitialize()
        bleBaseRequest?.unRegisterBluetoothStateReceiver()
    }

    /**
     * 断开某个设备的连接 释放资源
     */
    @Synchronized
    fun close(bleDevice: BleDevice) {
        checkInitialize()
        bleBaseRequest?.close(bleDevice)
        BleLogger.i("${bleDevice}资源释放完毕")
    }

    fun getOptions() = bleOptions

    fun getContext() = application

    fun getBluetoothManager() = bluetoothManager

    private fun checkInitialize() {
        if (bleBaseRequest == null) {
            BleLogger.e("未初始化，请调用BleManager.init()")
        }
    }

    /**
     * 通过设备地址构建BleDevice对象，确保已获取到权限
     */
    @SuppressLint("MissingPermission")
    fun buildBleDeviceByDeviceAddress(deviceAddress: String): BleDevice {
        val deviceInfo = bluetoothManager?.adapter?.getRemoteDevice(deviceAddress)
        return BleDevice(
            deviceInfo,
            deviceInfo?.name?: "",
            deviceAddress,
            0,
            0,
            null,
            null
        )
    }
}