# SupportCore

### 用法

        allprojects {
            repositories {
                ...
                maven { url 'https://jitpack.io' }
            }
        }

        dependencies {
            implementation 'com.github.buhuiming:SupportCore:1.0.0-beta01'
        }

#### 1、添加权限
    
    //动态申请
    val LOCATION_PERMISSION = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            )
        } else {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_CONNECT,
            )
        }

    注意：
    有些设备GPS是关闭状态的话，申请定位权限之后，GPS是依然关闭状态，这里要根据GPS是否打开来跳转页面
    BleUtil.isGpsOpen(context) 判断GPS是否打开
    跳转到系统GPS设置页面，GPS设置是全局的独立的，是否打开跟权限申请无关
    startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
    跳转到系统蓝牙设置页面
    startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))

#### 1、初始化
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

    //或者使用默认配置
    BleManager.get().init(application)

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