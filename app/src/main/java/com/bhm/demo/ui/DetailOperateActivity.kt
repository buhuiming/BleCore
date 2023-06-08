/*
 * Copyright (c) 2022-2032 buhuiming
 * 不能修改和删除上面的版权声明
 * 此代码属于buhuiming编写，在未经允许的情况下不得传播复制
 */
package com.bhm.demo.ui

import android.content.Intent
import android.os.Build
import android.view.KeyEvent
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.bhm.ble.BleManager
import com.bhm.ble.device.BleDevice
import com.bhm.demo.BaseActivity
import com.bhm.demo.R
import com.bhm.demo.adapter.DetailsExpandAdapter
import com.bhm.demo.adapter.LoggerListAdapter
import com.bhm.demo.databinding.ActivityDetailBinding
import com.bhm.demo.entity.CharacteristicNode
import com.bhm.demo.entity.OperateType
import com.bhm.demo.vm.DetailViewModel
import com.bhm.support.sdk.core.AppTheme
import com.bhm.support.sdk.entity.MessageEvent
import com.bhm.support.sdk.utils.ViewUtil
import kotlinx.coroutines.launch


/**
 * 服务，特征 操作页面
 *
 * @author Buhuiming
 * @date 2023年06月01日 09时17分
 */
class DetailOperateActivity : BaseActivity<DetailViewModel, ActivityDetailBinding>() {

    override fun createViewModel() = DetailViewModel(application)

    private var bleDevice: BleDevice? = null

    private var expandAdapter: DetailsExpandAdapter? = null

    private var loggerListAdapter: LoggerListAdapter? = null

    private var disConnectWhileClose = false // 关闭页面后是否断开连接

    private var currentSendNode: CharacteristicNode? = null

    override fun initData() {
        super.initData()
        AppTheme.setStatusBarColor(this, R.color.purple_500)
        bleDevice = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("data", BleDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("data")
        }
        disConnectWhileClose = intent.getBooleanExtra("disConnectWhileClose", false)

        if (bleDevice == null) {
            finish()
            return
        }
        viewBinding.tvName.text = buildString {
            append("设备广播名：")
            append(getBleDevice().deviceName)
            append("\r\n")
            append("地址：${getBleDevice().deviceAddress}")
        }
        initList()
    }

    private fun getBleDevice(): BleDevice {
        return bleDevice!!
    }

    private fun initList() {
        val layoutManager = LinearLayoutManager(this)
        layoutManager.orientation = LinearLayoutManager.VERTICAL
        viewBinding.recyclerView.layoutManager = layoutManager
        viewBinding.recyclerView.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        expandAdapter = DetailsExpandAdapter(viewModel.getListData(getBleDevice())) {
                checkBox, operateType, isChecked, node ->
            when (operateType) {
                is OperateType.Write -> {
                    if (isChecked) {
                        if (viewBinding.btnSend.isEnabled) {
                            checkBox?.isChecked = false
                            Toast.makeText(this, "请取消其他特征值写操作", Toast.LENGTH_SHORT).show()
                            return@DetailsExpandAdapter
                        }
                        viewBinding.btnSend.isEnabled = true
                        viewBinding.etContent.isEnabled = true
                        currentSendNode = node
                    } else {
                        viewBinding.btnSend.isEnabled = false
                        viewBinding.etContent.isEnabled = false
                        currentSendNode = null
                    }
                }
                is OperateType.Read -> {
                    viewModel.readData(getBleDevice(), node.serviceUUID, node.characteristicUUID)
                }
                is OperateType.Notify -> {
                    if (isChecked) {
                        viewModel.notify(getBleDevice(), node.serviceUUID, node.characteristicUUID) {
                            checkBox?.isChecked = false
                        }
                    } else {
                        viewModel.stopNotify(getBleDevice(), node.serviceUUID, node.characteristicUUID)
                    }
                }
                is OperateType.Indicate -> {
                    if (isChecked) {
                        viewModel.indicate(getBleDevice(), node.serviceUUID, node.characteristicUUID) {
                            checkBox?.isChecked = false
                        }
                    } else {
                        viewModel.stopIndicate(getBleDevice(), node.serviceUUID, node.characteristicUUID)
                    }
                }
            }
        }
        viewBinding.recyclerView.adapter = expandAdapter
        if ((expandAdapter?.data?.size?: 0) > 0) {
            expandAdapter?.expand(0)
        }

        val logLayoutManager = LinearLayoutManager(this)
        logLayoutManager.orientation = LinearLayoutManager.VERTICAL
        viewBinding.logRecyclerView.setHasFixedSize(true)
        viewBinding.logRecyclerView.layoutManager = logLayoutManager
        (viewBinding.recyclerView.itemAnimator as DefaultItemAnimator).supportsChangeAnimations = false
        loggerListAdapter = LoggerListAdapter(viewModel.listLogData)
        viewBinding.logRecyclerView.adapter = loggerListAdapter

    }

    override fun initEvent() {
        super.initEvent()
        lifecycleScope.launch {
            viewModel.listLogStateFlow.collect {
                viewModel.listLogData.add(it)
                val position = viewModel.listLogData.size - 1
                loggerListAdapter?.notifyItemInserted(position)
                viewBinding.logRecyclerView.smoothScrollToPosition(position)
            }
        }

        viewBinding.btnClear.setOnClickListener {
            if (ViewUtil.isInvalidClick(it)) {
                return@setOnClickListener
            }
            loggerListAdapter?.notifyItemRangeRemoved(0, viewModel.listLogData.size)
            viewModel.listLogData.clear()
        }

        viewBinding.btnSetMtu.setOnClickListener {
            if (ViewUtil.isInvalidClick(it)) {
                return@setOnClickListener
            }
            viewModel.setMtu(getBleDevice())
        }

        viewBinding.btnReadRssi.setOnClickListener {
            if (ViewUtil.isInvalidClick(it)) {
                return@setOnClickListener
            }
            viewModel.readRssi(getBleDevice())
        }
        viewBinding.btnSend.setOnClickListener {
            if (ViewUtil.isInvalidClick(it)) {
                return@setOnClickListener
            }
            val content = viewBinding.etContent.text.toString()
            if (content.isEmpty()) {
                Toast.makeText(this, "请输入数据", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (currentSendNode == null) {
                Toast.makeText(this, "请选择特征值写操作", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            currentSendNode?.let { node ->
                viewModel.writeData(
                    getBleDevice(),
                    node.serviceUUID,
                    node.characteristicUUID,
                    content
                )
            }
        }
    }

    /**
     * 接收到断开通知
     */
    override fun onMessageEvent(event: MessageEvent?) {
        super.onMessageEvent(event)
        event?.let {
            val device = it.data as BleDevice
            if (getBleDevice() == device) {
                finish()
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (disConnectWhileClose) {
                BleManager.get().release(getBleDevice())
                setResult(0, Intent())
            } else {
                BleManager.get().removeAllCharacterCallback(getBleDevice())
                setResult(0, null)
            }
            finish()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}