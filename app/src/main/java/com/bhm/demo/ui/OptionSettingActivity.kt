/*
 * Copyright (c) 2022-2032 buhuiming
 * 不能修改和删除上面的版权声明
 * 此代码属于buhuiming编写，在未经允许的情况下不得传播复制
 */
package com.bhm.demo.ui

import com.bhm.ble.BleManager
import com.bhm.ble.attribute.BleOptions
import com.bhm.demo.BaseActivity
import com.bhm.demo.R
import com.bhm.demo.databinding.ActivitySettingBinding
import com.bhm.support.sdk.common.BaseViewModel
import com.bhm.support.sdk.core.AppTheme
import com.bhm.support.sdk.utils.ViewUtil


/**
 * 设置扫描配置项页面
 *
 * @author Buhuiming
 * @date 2023年05月24日 15时38分
 */
class OptionSettingActivity : BaseActivity<BaseViewModel, ActivitySettingBinding>(){

    override fun createViewModel() = BaseViewModel (application)

    override fun initData() {
        super.initData()
        AppTheme.setStatusBarColor(this, R.color.purple_500)
    }

    override fun initEvent() {
        super.initEvent()
        viewBinding.btnReSet.setOnClickListener {
            if (ViewUtil.isInvalidClick(it)) {
                return@setOnClickListener
            }
            viewBinding.etScanServiceUuid.setText("")
            viewBinding.etScanDeviceName.setText("")
            viewBinding.etScanDeviceAddress.setText("")
            viewBinding.etScanOutTime.setText(BleOptions.DEFAULT_SCAN_MILLIS_TIMEOUT.toString())
            viewBinding.etScanRetryCount.setText(BleOptions.DEFAULT_SCAN_RETRY_COUNT.toString())
            viewBinding.etScanRetryInterval.setText(BleOptions.DEFAULT_SCAN_RETRY_INTERVAL.toString())
            viewBinding.etConnectOutTime.setText(BleOptions.DEFAULT_CONNECT_MILLIS_TIMEOUT.toString())
            viewBinding.etConnectRetryCount.setText(BleOptions.DEFAULT_CONNECT_RETRY_COUNT.toString())
            viewBinding.etConnectRetryInterval.setText(BleOptions.DEFAULT_CONNECT_RETRY_INTERVAL.toString())
            viewBinding.etOperateMillisTimeOut.setText(BleOptions.DEFAULT_OPERATE_MILLIS_TIMEOUT.toString())
            viewBinding.etWriteInterval.setText(BleOptions.DEFAULT_WRITE_INTERVAL.toString())
            viewBinding.etMaxConnectNum.setText(BleOptions.DEFAULT_MAX_CONNECT_NUM.toString())
            viewBinding.etMTU.setText(BleOptions.DEFAULT_MTU.toString())
            viewBinding.cbContainScanDeviceName.isChecked = BleOptions.CONTAIN_SCAN_DEVICE_NAME
            viewBinding.cbLogger.isChecked = BleOptions.ENABLE_LOG
            viewBinding.cbAutoConnect.isChecked = BleOptions.AUTO_CONNECT
            BleManager.get().init(application)
        }
        viewBinding.btnSave.setOnClickListener { view ->
            if (ViewUtil.isInvalidClick(view)) {
                return@setOnClickListener
            }
            if (viewBinding.etMaxConnectNum.text.toString().toInt() > 7) {
                viewBinding.etMaxConnectNum.setText("7")
            }
            if (viewBinding.etMTU.text.toString().toInt() > 517) {
                viewBinding.etMTU.setText(517.toString())
            }

            val builder = BleOptions.builder()
            val scanServiceUuids = viewBinding.etScanServiceUuid.text.toString().split(",")
            scanServiceUuids.forEach {
                builder.setScanServiceUuid(it)
            }
            val scanDeviceNames = viewBinding.etScanDeviceName.text.toString().split(",")
            scanDeviceNames.forEach {
                builder.setScanDeviceName(it)
            }
            val scanDeviceAddresses = viewBinding.etScanDeviceAddress.text.toString().split(",")
            scanDeviceAddresses.forEach {
                builder.setScanDeviceAddress(it)
            }

            builder
                .isContainScanDeviceName(viewBinding.cbContainScanDeviceName.isChecked)
                .setEnableLog(viewBinding.cbLogger.isChecked)
                .setScanMillisTimeOut(viewBinding.etScanOutTime.text.toString().toLong())
//                //这个机制是：不会因为扫描的次数导致上一次扫描到的数据被清空，也就是onStart和onScanComplete
//                //都只会回调一次，而且扫描到的数据是所有扫描次数的总和
                .setScanRetryCountAndInterval(viewBinding.etScanRetryCount.text.toString().toInt(),
                    viewBinding.etScanRetryInterval.text.toString().toLong())
                .setConnectMillisTimeOut(viewBinding.etConnectOutTime.text.toString().toLong())//实现中
                .setConnectRetryCountAndInterval(viewBinding.etConnectRetryCount.text.toString().toInt(),
                    viewBinding.etConnectRetryInterval.text.toString().toLong())//实现中
                .setAutoConnect(viewBinding.cbAutoConnect.isChecked)//实现中
                .setOperateMillisTimeOut(viewBinding.etOperateMillisTimeOut.text.toString().toLong())//实现中
                .setWriteInterval(viewBinding.etWriteInterval.text.toString().toLong())//实现中
                .setMaxConnectNum(viewBinding.etMaxConnectNum.text.toString().toInt())//实现中
                .setMtu(viewBinding.etMTU.text.toString().toInt())//实现中
                .build()
            BleManager.get().init(application, builder.build())
            finish()
        }
    }

}