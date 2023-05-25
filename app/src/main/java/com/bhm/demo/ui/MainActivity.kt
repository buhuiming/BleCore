package com.bhm.demo.ui

import android.content.Intent
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.bhm.ble.data.BleDevice
import com.bhm.demo.BaseActivity
import com.bhm.demo.R
import com.bhm.demo.adapter.DeviceListAdapter
import com.bhm.demo.databinding.ActivityMainBinding
import com.bhm.demo.vm.MainViewModel
import com.bhm.support.sdk.core.AppTheme
import com.bhm.support.sdk.utils.ViewUtil
import kotlinx.coroutines.launch
import leakcanary.LeakCanary

/**
 * 主页面
 * @author Buhuiming
 * @date :2023/5/24 15:39
 */
class MainActivity : BaseActivity<MainViewModel, ActivityMainBinding>(){

    private var leftListAdapter: DeviceListAdapter? = null

    private var rightListAdapter: DeviceListAdapter? = null

    override fun createViewModel() = MainViewModel(application)

    override fun initData() {
        super.initData()
        AppTheme.setStatusBarColor(this, R.color.purple_500)
        LeakCanary.runCatching {  }
        initList()
        viewModel.initBle()
    }

    override fun initEvent() {
        super.initEvent()
        lifecycleScope.launch {
            viewModel.listStateFlow.collect {
                if (it.deviceName != null && it.deviceAddress != null) {
                    val position = (leftListAdapter?.itemCount?: 1) - 1
                    leftListAdapter?.notifyItemInserted(position)
                    viewBinding.recyclerViewLeft.smoothScrollToPosition(position)
                }
            }
        }

        lifecycleScope.launch {
            viewModel.listDRStateFlow.collect {
                if (it.deviceName != null && it.deviceAddress != null) {
                    val position = (rightListAdapter?.itemCount?: 1) - 1
                    rightListAdapter?.notifyItemInserted(position)
                    viewBinding.recyclerViewRight.smoothScrollToPosition(position)
                }
            }
        }

        lifecycleScope.launch {
            viewModel.scanStopStateFlow.collect {
                viewBinding.pbLoading.visibility = if (it) { View.INVISIBLE } else { View.VISIBLE }
                viewBinding.btnStart.text = if (it) { "开启扫描" } else { "扫描中..." }
                viewBinding.btnStart.isEnabled = it
                viewBinding.btnSetting.isEnabled = it
                viewBinding.btnStop.isEnabled = !it
            }
        }

        leftListAdapter?.addChildClickViewIds(R.id.btnConnect)
        leftListAdapter?.setOnItemChildClickListener { adapter, view, position ->
            if (ViewUtil.isInvalidClick(view)) {
                return@setOnItemChildClickListener
            }
            val bleDevice: BleDevice? = adapter.data[position] as BleDevice?
            viewModel.connect(bleDevice)
        }

        rightListAdapter?.addChildClickViewIds(R.id.btnConnect)
        rightListAdapter?.setOnItemChildClickListener { adapter, view, position ->
            if (ViewUtil.isInvalidClick(view)) {
                return@setOnItemChildClickListener
            }
            val bleDevice: BleDevice? = adapter.data[position] as BleDevice?
            viewModel.disConnect(bleDevice)
        }

        viewBinding.btnSetting.setOnClickListener {
            if (ViewUtil.isInvalidClick(it)) {
                return@setOnClickListener
            }
            startActivity(Intent(this@MainActivity, OptionSettingActivity::class.java))
        }

        viewBinding.btnStart.setOnClickListener {
            if (ViewUtil.isInvalidClick(it)) {
                return@setOnClickListener
            }
            leftListAdapter?.notifyItemRangeRemoved(0, viewModel.listData.size)
            viewModel.listData.clear()
            rightListAdapter?.notifyItemRangeRemoved(0, viewModel.listDRData.size)
            viewModel.listDRData.clear()
            viewModel.startScan(this@MainActivity)
        }

        viewBinding.btnStop.setOnClickListener {
            if (ViewUtil.isInvalidClick(it)) {
                return@setOnClickListener
            }
            viewModel.stopScan()
        }
    }

    private fun initList() {
        val leftLayoutManager = LinearLayoutManager(this)
        leftLayoutManager.orientation = LinearLayoutManager.VERTICAL
        viewBinding.recyclerViewLeft.setHasFixedSize(true)
        viewBinding.recyclerViewLeft.layoutManager = leftLayoutManager
        viewBinding.recyclerViewLeft.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        //解决RecyclerView局部刷新时闪烁
        (viewBinding.recyclerViewLeft.itemAnimator as DefaultItemAnimator).supportsChangeAnimations = false
        leftListAdapter = DeviceListAdapter(viewModel.listData)
        viewBinding.recyclerViewLeft.adapter = leftListAdapter

        val rightLayoutManager = LinearLayoutManager(this)
        rightLayoutManager.orientation = LinearLayoutManager.VERTICAL
        viewBinding.recyclerViewRight.setHasFixedSize(true)
        viewBinding.recyclerViewRight.layoutManager = rightLayoutManager
        viewBinding.recyclerViewRight.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        //解决RecyclerView局部刷新时闪烁
        (viewBinding.recyclerViewRight.itemAnimator as DefaultItemAnimator).supportsChangeAnimations = false
        rightListAdapter = DeviceListAdapter(viewModel.listDRData)
        viewBinding.recyclerViewRight.adapter = rightListAdapter
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.stopScan()
    }
}