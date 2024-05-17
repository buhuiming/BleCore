# BleCore Android蓝牙低功耗(BLE)快速开发框架

## 本项目持续维护更新

*   当前版本[![](https://jitpack.io/v/buhuiming/BleCore.svg)](https://jitpack.io/#buhuiming/BleCore) 
*   minSdk 24
*   targetSdk 34
*   compileSdk 34

#### * 基于Kotlin、协程
#### * 基于sdk 34，最新API
#### * 详细的完整的容错机制
#### * 基于多个蓝牙库的设计思想
#### * 强大的Notify\Indicate\Read\Write任务队列

![20230613110126](https://github.com/buhuiming/BleCore/blob/main/screenshots/20230613110126.png)
![20230613110146](https://github.com/buhuiming/BleCore/blob/main/screenshots/20230613110146.png)
![20230614090104](https://github.com/buhuiming/BleCore/blob/main/screenshots/20230614090104.png)

### demo体验

![apk_address](https://github.com/buhuiming/BleCore/blob/main/screenshots/apk_address.png)

###
###

### 详细用法参考demo 
### 详细用法参考demo 
### 详细用法参考demo 

### 用法

        allprojects {
            repositories {
                ...
                maven { url 'https://jitpack.io' }
            }
        }

        dependencies {
            implementation 'com.github.buhuiming:BleCore:latest version'
        }

#### 添加权限

    //动态申请
    val LOCATION_PERMISSION = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            arrayOf(
                //Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            )
        } else {
            arrayOf(
                //Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_CONNECT,
            )
        }
*    特别注意：如果需要用到扫描蓝牙设备的功能，需要申请精准位置权限：Manifest.permission.ACCESS_FINE_LOCATION，
     否则可能会导致扫描不到设备。

*    注意：
*    有些设备GPS是关闭状态的话，申请定位权限之后，GPS是依然关闭状态，这里要根据GPS是否打开来跳转页面
*    BleUtil.isGpsOpen(context) 判断GPS是否打开
*    跳转到系统GPS设置页面，GPS设置是全局的独立的，是否打开跟权限申请无关
     startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
*    跳转到系统蓝牙设置页面
     startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))

#### 初始化
    val options =
            BleOptions.builder()
                .setScanServiceUuid("0000ff80-0000-1000-8000-00805f9b34fb", "0000ff90-0000-1000-8000-00805f9b34fb")
                .setScanDeviceName("midea", "BYD BLE3")
                .setScanDeviceAddress("70:86:CE:88:7A:AF", "5B:AE:65:88:59:5E", "B8:8C:29:8B:BE:07")
                .isContainScanDeviceName(true)
                .setAutoConnect(false)
                .setEnableLog(true)
                .setScanMillisTimeOut(12000)
                //这个机制是：不会因为扫描的次数导致上一次扫描到的数据被清空，也就是onScanStart和onScanComplete
                //都只会回调一次，而且扫描到的数据是所有扫描次数的总和
                .setScanRetryCountAndInterval(2, 1000)
                .setConnectMillisTimeOut(10000)
                .setConnectRetryCountAndInterval(2, 5000)
                .setOperateMillisTimeOut(6000)
                .setOperateInterval(80)
                .setMaxConnectNum(5)
                .setMtu(500)
                .setTaskQueueType(BleTaskQueueType.Operate)
                .build()
    BleManager.get().init(application, options)

    //或者使用默认配置
    BleManager.get().init(application)

setTaskQueueType方法，有3个选项分别是：
*   BleTaskQueueType.Default
    一个设备的Notify\Indicate\Read\Write\mtu操作所对应的任务共享同一个任务
    队列(共享队列)(不区分特征值)，rssi在rssi队列
*   BleTaskQueueType.Operate
    一个设备每个操作独立一个任务队列(不区分特征值)
    Notify在Notify队列中，Indicate在Indicate队列中，Read在Read队列中，
    Write在Write队列中，mtu在共享队列，rssi在rssi队列中，
    不同操作任务之间相互不影响，相同操作任务之间先进先出按序执行
    例如特征值1的写操作和特征值2的写操作，在同一个任务队列当中；特征值1的写操作和特征值1的读操作，
    在两个不同的任务队列当中，特征值1的读操作和特征值2的写操作，在两个不同的任务队列当中。
*   BleTaskQueueType.Independent
    一个设备每个特征值下的每个操作独立一个任务队列(区分特征值)
    Notify\Indicate\Read\Write所对应的任务分别放入到独立的任务队列中，
    mtu在共享队列，rssi在rssi队列中，
    且按特征值区分，不同操作任务之间相互不影响，相同操作任务之间相互不影响
    例如特征值1的写操作和特征值2的写操作，在两个不同的任务队列当中；特征值1的写操作和特征值1的读操作，
    在两个不同的任务队列当中，特征值1的读操作和特征值2的写操作，在两个不同的任务队列当中。

注意：BleTaskQueueType.Operate、BleTaskQueueType.Independent这两种模式下
* 1、在Notify\Indicate\Read\Write 未完成的情况下，不要执行设置Mtu，否则会导致前者操作失败
* 2、同时执行Notify\Indicate\Read\Write其中两个以上操作，会可能报设备忙碌失败
 
建议：以上模式主要也是针对操作之间的问题，强烈建议不要同时执行2个及以上操作，模式BleTaskQueueType.Default就是为
     了让设备所有操作同一时间只执行一个，Rssi不受影响
    
#### 扫描
    注意：扫描之前先检查权限、检查GPS开关、检查蓝牙开关
    扫描及过滤过程是在工作线程中进行，所以不会影响主线程的UI操作，最终每一个回调结果都会回到主线程。
    开启扫描：
    BleManager.get().startScan {
        onScanStart {

        }
        onLeScan { bleDevice, currentScanCount ->
            //可以根据currentScanCount是否已有清空列表数据
        }
        onLeScanDuplicateRemoval { bleDevice, currentScanCount ->
            //与onLeScan区别之处在于：同一个设备只会出现一次
        }
        onScanComplete { bleDeviceList, bleDeviceDuplicateRemovalList ->
            //扫描到的数据是所有扫描次数的总和
        }
        onScanFail {
            val msg: String = when (it) {
                is BleScanFailType.UnSupportBle -> "BleScanFailType.UnSupportBle: 设备不支持蓝牙"
                is BleScanFailType.NoBlePermission -> "BleScanFailType.NoBlePermission: 权限不足，请检查"
                is BleScanFailType.GPSDisable -> "BleScanFailType.BleDisable: 设备未打开GPS定位"
                is BleScanFailType.BleDisable -> "BleScanFailType.BleDisable: 蓝牙未打开"
                is BleScanFailType.AlReadyScanning -> "BleScanFailType.AlReadyScanning: 正在扫描"
                is BleScanFailType.ScanError -> {
                    "BleScanFailType.ScanError: ${it.throwable?.message}"
                }
            }
            BleLogger.e(msg)
            Toast.makeText(application, msg, Toast.LENGTH_SHORT).show()
        }
    }

#### 停止扫描
    BleManager.get().stopScan()

#### 是否扫描中
    BleManager.get().isScanning()

#### 连接
    BleManager.get().connect(device)
    BleManager.get().connect(deviceAddress)

*    在某些型号手机上，connectGatt必须在主线程才能有效，所以把连接过程放在主线程，回调也在主线程。
*    为保证重连成功率，建议断开后间隔一段时间之后进行重连。（非常关键，因为断开后会有释放资源的等待时间，如果马上重连，会导致连接的资源会被释放掉，而产生错误）
*    v1.9.0添加字段isForeConnect，主要针对某些机型，当触发连接超时回调连接失败并释放资源之后，此时外设开启触发手机系统已连接，但BleCore资源被
     释放 (bluetoothGatt是null)，或BleCore和系统的连接状态不一致，而导致setMtu和Notify/Indicate都失败。

#### 停止连接
    BleManager.get().stopConnect(device)

#### 断开连接
    BleManager.get().disConnect(device)
    BleManager.get().disConnect(deviceAddress)

*    断开后，并不会马上更新状态，所以马上连接会直接返回已连接，而且扫描不出来，要等待一定时间才可以
*    BleConnectCallback中onDisConnecting、onDisConnected分别是，断开连接时触发onDisConnecting，
     真正断开之后触发onDisConnected。(isActiveDisConnected = true的时候，触发onDisConnecting之后大约1秒左右
     才会触发onDisConnected；isActiveDisConnected = false的时候，触发onDisConnecting之后大约5毫秒左右
     才会触发onDisConnected)

#### 是否已连接
    BleManager.get().isConnected(bleDeviceAddress: String, simplySystemStatus: Boolean = true)
    BleManager.get().isConnected(bleDevice: BleDevice?, simplySystemStatus: Boolean = true)

*    simplySystemStatus 为true，只根据系统的状态规则；为false，会根据sdk的状态，换句话说，只根据系统的状态返回。
     此字段的意义在于：有时，sdk资源被系统回收(状态未连接)，但是系统的状态是已连接。

#### 扫描并连接，如果扫描到多个设备，则会连接第一个
    BleManager.get().startScanAndConnect(bleScanCallback: BleScanCallback,
                                         bleConnectCallback: BleConnectCallback)

*    扫描到首个符合扫描规则的设备后，便停止扫描，然后连接该设备。

#### 获取设备的BluetoothGatt对象
    BleManager.get().getBluetoothGatt(device)

#### 设置Notify
    BleManager.get().notify(bleDevice: BleDevice,
                                  serviceUUID: String,
                                  notifyUUID: String,
                                  bleDescriptorGetType: BleDescriptorGetType = BleDescriptorGetType.Default,
                                  bleIndicateCallback: BleIndicateCallback)
BleDescriptorGetType设计原则
*    正常情况下，每个特征值下至少有一个默认描述符，并且遵循蓝牙联盟定义的UUID规则，如
     [com.bhm.ble.data.Constants.UUID_CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR]便是蓝牙联盟定义的
     客户端特性配置的描述符UUID，这样做是方便BLE终端在接入不同类型设备时，能够获取到正确的配置。
     比如有一个APP，需要 接入A商家的智能手表和B商家的智能手表来监听用户的心跳，而如果A商家的智能手表或者B商家的智能手表
     不遵循蓝牙联盟定义关于 心跳相关的UUID，则对APP来说就要分别去获取A商家的智能手表或者B商家的智能手表对应特征值的描述
     符UUID，显然是不合理的。当然这个是需要硬件设备支持的，也就是说硬件设备可以自定义UUID，但需要遵循规则。
*    在开发过程中，我们会遇到不同硬件设备定义UUID的情况，有的硬件设备通过特征值的UUID来获取描述符(用来writeDescriptor，
     打开或关闭notify、indicate)，而非是通过系统提供接受通知自带的UUID获取描述符。此外特征值有多个描述符时，获取其中
     一个描述符来写入数据，可能会导致onCharacteristicChanged函数没有回调，我不确定是否是硬件设备需要支持修改的问题。
     因此[AllDescriptor]方式则是简单粗暴的将特征值下所有的描述符都写入数据，以保证onCharacteristicChanged函数回调，
     这个方法经过了一系列设备的验证可行，但不保证是完全有效的。

#### 取消Notify
    BleManager.get().stopNotify(bleDevice: BleDevice,
                                  serviceUUID: String,
                                  notifyUUID: String,
                                  bleDescriptorGetType: BleDescriptorGetType = BleDescriptorGetType.Default)

#### 设置Indicate
    BleManager.get().indicate(bleDevice: BleDevice,
                                  serviceUUID: String,
                                  indicateUUID: String,
                                  bleDescriptorGetType: BleDescriptorGetType = BleDescriptorGetType.Default,
                                  bleIndicateCallback: BleIndicateCallback)

#### 取消Indicate
    BleManager.get().stopIndicate(bleDevice: BleDevice,
                                  serviceUUID: String,
                                  indicateUUID: String,
                                  bleDescriptorGetType: BleDescriptorGetType = BleDescriptorGetType.Default)

#### 读取信号值
    BleManager.get().readRssi(bleDevice: BleDevice, bleRssiCallback: BleRssiCallback)
    
*    获取设备的信号强度，需要在设备连接之后进行。
*    某些设备可能无法读取Rssi，不会回调onRssiSuccess(),而会因为超时而回调onRssiFail()。

#### 设置Mtu值
    BleManager.get().setMtu(bleDevice: BleDevice, bleMtuChangedCallback: BleMtuChangedCallback) 

*    设置MTU，需要在设备连接之后进行操作。
*    默认每一个BLE设备都必须支持的MTU为23。
*    MTU为23，表示最多可以发送20个字节的数据。
*    该方法的参数mtu，最小设置为23，最大设置为512。
*    并不是每台设备都支持拓展MTU，需要通讯双方都支持才行，也就是说，需要设备硬件也支持拓展MTU该方法才会起效果。
     调用该方法后，可以通过onMtuChanged(int mtu)查看最终设置完后，设备的最大传输单元被拓展到多少。如果设备不支持，
     可能无论设置多少，最终的mtu还是23。 
*    建议在indicate、notify、read、write未完成的情况下，不要执行设置Mtu，否则会导致前者操作失败


#### 设置连接的优先级
    BleManager.get().setConnectionPriority(connectionPriority: Int)

*    设置连接的优先级，一般用于高速传输大量数据的时候可以进行设置。

#### 读特征值数据
    BleManager.get().readData(bleDevice: BleDevice,
                              serviceUUID: String,
                              readUUID: String,
                              bleIndicateCallback: BleReadCallback)

#### 写数据
     BleManager.get().writeData(bleDevice: BleDevice,
                                serviceUUID: String,
                                writeUUID: String,
                                data: ByteArray,
                                bleWriteCallback: BleWriteCallback)
     BleManager.get().writeData(bleDevice: BleDevice,
                                serviceUUID: String,
                                writeUUID: String,
                                data: SparseArray,
                                bleWriteCallback: BleWriteCallback)

*    因为分包后每一个包，可能是包含完整的协议，所以分包由业务层处理，组件只会根据包的长度和mtu值对比后是否拦截
*    特殊情况下：indicate\mtu\notify\read\rssi 这些操作，同一个特征值在不同地方调用(不同callback)，最后面的操作
     对应的回调才会触发，其他地方先前的操作对应的回调不会触发
     解决方案：业务层每个特征值对应的操作维护一个单例的callback对象（假如为SingleCallback），在不同地方调用再传递callback
             (放入到SingleCallback中的集合CallbackList)，SingleCallback 回调时循环CallbackList中的callback，这样就达到了
              同一个特征值在不同地方调用，都能收到回调
     
*    indicate\mtu\notify\read\rssi这些操作 ，同一个特征值在不同地方调用，后面的操作会取消前面未完成的操作；write操作比较
     特殊，每个写操作都会有回调，且write操作之间不会被取消。具体详情看taskId

*    一次写操作，分包后，假如某个数据包写失败，后面的数据包不会继续写，例如一次写操作分包后有10个数据包，第7个写失败，后面第8、9、10不会再写     

#### 断开某个设备的连接 释放资源
    BleManager.get().close(bleDevice: BleDevice)

#### 断开所有连接 释放资源
    BleManager.get().closeAll()

#### 一些移除监听的函数
    BleManager.get().removeBleScanCallback()
    BleManager.get().removeBleConnectCallback(bleDevice: BleDevice)
    BleManager.get().removeBleIndicateCallback(bleDevice: BleDevice, indicateUUID: String)
    BleManager.get().removeBleNotifyCallback(bleDevice: BleDevice, notifyUUID: String)
    BleManager.get().removeBleRssiCallback(bleDevice: BleDevice)
    BleManager.get().removeBleMtuChangedCallback(bleDevice: BleDevice)
    BleManager.get().removeBleReadCallback(bleDevice: BleDevice, readUUID: String)
    BleManager.get().removeBleWriteCallback(bleDevice: BleDevice, writeUUID: String)

#### v1.5.0新增addBleEventCallback方法
    有用户反馈，设置[connect]的bleConnectCallback、[notify]的bleNotifyCallback、
     [indicate]的bleIndicateCallback、[setMtu]的bleMtuChangedCallback之后，当其他地方需要监听这些回调时比较
    不方便，所以添加addBleEventCallback来实现。addBleEventCallback与上述回调共存

#### v1.7.0新增系统蓝牙变化广播监听
    BleManager.get().registerBluetoothStateReceiver()
    BleManager.get().unRegisterBluetoothStateReceiver()

#### v1.8.0新增stopConnect方法停止或者取消连接
    BleManager.get().stopConnect(device)

#### v2.0.0新增writeQueueData方法
    BleManager.get().writeQueueData()，此方法支持跳过空数据包，支持写失败后重试，提高成功率。可以用于OTA升级

#### 获取BleCore日志，使用自定义的日志框架打印日志或收集BleCore日志
    通过第一步初始化时候，setEnableLog方法来决定是否使用BleCore的日志打印；
    业务层，通过实现BleLogEvent接口，如下：

    class MainActivity : BaseActivity(), BleLogEvent {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            //添加BleCore日志监听
            BleLogManager.get().addLogListener(this)
        }

        override fun onDestroy() {
            super.onDestroy()
            //移除BleCore日志监听
            BleLogManager.get().removeLogListener(this)
        }

        /**
         * 获取BleCore库的日志，并统一使用Logger来打印日志获取其他收集功能
        */
        override fun onLog(level: BleLogLevel, tag: String, message: String?) {
            if (message.isNullOrEmpty()) {
                return
            }
           when (level) {
               is BleLogLevel.Debug ->  Logger.d(tag, message)
               is BleLogLevel.Info ->  Logger.i(tag, message)
               is BleLogLevel.Warn ->  Logger.w(tag, message)
               is BleLogLevel.Error ->  Logger.e(tag, message)
           }
        }
    } 
    

#### [问题锦集](https://juejin.cn/post/6844903896100372494)，但愿对你有帮助
    https://blog.51cto.com/u_16213573/7811086
* 1、少部分机型会存在断开连接(gatt.disconnect)后，连接状态仍未刷新，导致其他机型连接不上外设。
  [参考](https://stackoverflow.com/questions/44521828/android-ble-gatt-disconnected-vs-device-disconnected)

#### 其他
* 1、关闭系统蓝牙，没有触发onConnectionStateChange
  解决方案：
  1、操作前判断蓝牙状态，
  2、系统蓝牙变化广播监听
     BleManager.get().registerBluetoothStateReceiver(getBluetoothCallback())
     BleManager.get().unRegisterBluetoothStateReceiver()


#### 考虑把Collections.synchronizedList换成其他不会导致死锁的集合

## License

```
Copyright (c) 2023 Bekie

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```