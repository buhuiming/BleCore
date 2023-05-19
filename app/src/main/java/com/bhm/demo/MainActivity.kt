package com.bhm.demo

import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.bhm.ble.BleManager
import com.bhm.ble.attribute.BleOptions
import com.bhm.demo.databinding.ActivityMainBinding
import com.bhm.support.sdk.common.BaseVBActivity
import com.bhm.support.sdk.core.AppTheme
import leakcanary.LeakCanary


class MainActivity : BaseVBActivity<MainViewModel, ActivityMainBinding>(){

    private var listAdapter: DeviceListAdapter? = null

    override fun createViewModel() = MainViewModel(application)

    override fun initData() {
        super.initData()
        AppTheme.setStatusBarColor(this, R.color.purple_500)
        LeakCanary.runCatching {  }
        initList()
//        BleManager.init(application)
        val options =
            BleOptions.builder()
                .setScanServiceUuid("0000414b-0000-1000-8000-00805f9b34fb")
                .setScanDeviceName("V8001")
                .setScanDeviceMac("DC:A1:2F:44:NC")
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
                .build()
        BleManager.init(application, options)
    }

    override fun initEvent() {
        super.initEvent()
        listAdapter?.addChildClickViewIds(R.id.btnConnect)
        listAdapter?.setOnItemChildClickListener { _, _, _ ->

        }
    }

    private fun initList() {
        viewBinding.recyclerView.setHasFixedSize(true)
        val mLayoutManager = LinearLayoutManager(this)
        mLayoutManager.orientation = LinearLayoutManager.VERTICAL
        viewBinding.recyclerView.layoutManager = mLayoutManager
        viewBinding.recyclerView.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        //解决RecyclerView局部刷新时闪烁
        (viewBinding.recyclerView.itemAnimator as DefaultItemAnimator).supportsChangeAnimations = false
        listAdapter = DeviceListAdapter(arrayListOf())
        viewBinding.recyclerView.adapter = listAdapter
    }
}