package com.bhm.demo

import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.bhm.demo.adapter.DeviceListAdapter
import com.bhm.demo.databinding.ActivityMainBinding
import com.bhm.demo.vm.MainViewModel
import com.bhm.support.sdk.common.BaseVBActivity
import com.bhm.support.sdk.core.AppTheme
import kotlinx.coroutines.launch
import leakcanary.LeakCanary


class MainActivity : BaseVBActivity<MainViewModel, ActivityMainBinding>(){

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
//                    leftListAdapter?.addData(it)
                    val position = (leftListAdapter?.itemCount?: 1) - 1
                    leftListAdapter?.notifyItemInserted(position)
                    viewBinding.recyclerViewLeft.smoothScrollToPosition(position)
                }
            }
        }
        lifecycleScope.launch {
            viewModel.listDRStateFlow.collect {
                if (it.deviceName != null && it.deviceAddress != null) {
//                    rightListAdapter?.addData(it)
                    val position = (rightListAdapter?.itemCount?: 1) - 1
                    rightListAdapter?.notifyItemInserted(position)
                    viewBinding.recyclerViewRight.smoothScrollToPosition(position)
                }
            }
        }
        leftListAdapter?.addChildClickViewIds(R.id.btnConnect)
        leftListAdapter?.setOnItemChildClickListener { _, _, _ ->

        }
        rightListAdapter?.addChildClickViewIds(R.id.btnConnect)
        rightListAdapter?.setOnItemChildClickListener { _, _, _ ->

        }
        lifecycleScope.launch {
            viewModel.scanStopStateFlow.collect {
                viewBinding.pbLoading.visibility = if (it) { View.INVISIBLE } else { View.VISIBLE }
                viewBinding.btnStart.text = if (it) { "开启扫描" } else { "扫描中..." }
                viewBinding.btnStart.isEnabled = it
                viewBinding.btnStop.isEnabled = !it
            }
        }
        viewBinding.btnStart.setOnClickListener {
            leftListAdapter?.notifyItemRangeRemoved(0, viewModel.listData.size)
            viewModel.listData.clear()
            rightListAdapter?.notifyItemRangeRemoved(0, viewModel.listDRData.size)
            viewModel.listDRData.clear()
            viewModel.startScan(this@MainActivity)
        }

        viewBinding.btnStop.setOnClickListener {
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
}