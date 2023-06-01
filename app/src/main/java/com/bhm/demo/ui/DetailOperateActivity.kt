/*
 * Copyright (c) 2022-2032 buhuiming
 * 不能修改和删除上面的版权声明
 * 此代码属于buhuiming编写，在未经允许的情况下不得传播复制
 */
package com.bhm.demo.ui

import android.os.Build
import com.bhm.ble.data.BleDevice
import com.bhm.demo.BaseActivity
import com.bhm.demo.R
import com.bhm.demo.databinding.ActivityDetailBinding
import com.bhm.demo.vm.DetailViewModel
import com.bhm.support.sdk.core.AppTheme
import com.bhm.support.sdk.entity.MessageEvent


/**
 * 服务，特征
 *
 * @author Buhuiming
 * @date 2023年06月01日 09时17分
 */
class DetailOperateActivity : BaseActivity<DetailViewModel, ActivityDetailBinding>(){

    override fun createViewModel() = DetailViewModel(application)

    private var bleDevice: BleDevice? = null

    override fun initData() {
        super.initData()
        AppTheme.setStatusBarColor(this, R.color.purple_500)
        bleDevice = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("data", BleDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("data")
        }

        if (bleDevice == null) {
            finish()
            return
        }
        viewBinding.tvName.text = buildString {
            append(bleDevice?.deviceName)
            append("(${bleDevice?.deviceAddress})")
        }
    }

    /**
     * 接收到断开通知
     */
    override fun onMessageEvent(event: MessageEvent?) {
        super.onMessageEvent(event)
        event?.let {
            val device = event.data as BleDevice
            if (bleDevice == device) {
                finish()
            }
        }
    }
}