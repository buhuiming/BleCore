package com.bhm.demo.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.view.View
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.bhm.ble.device.BleDevice
import com.bhm.ble.log.BleLogger
import com.bhm.demo.BaseActivity
import com.bhm.demo.R
import com.bhm.demo.adapter.DeviceListAdapter
import com.bhm.demo.constants.LOCATION_PERMISSION
import com.bhm.demo.databinding.ActivityMainBinding
import com.bhm.demo.vm.MainViewModel
import com.bhm.support.sdk.core.AppTheme
import com.bhm.support.sdk.utils.ViewUtil
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import leakcanary.LeakCanary

/**
 * 主页面
 * @author Buhuiming
 * @date :2023/5/24 15:39
 */
class MainActivity : BaseActivity<MainViewModel, ActivityMainBinding>() {

    private var listAdapter: DeviceListAdapter? = null

    private var autoOpenDetailsActivity = false

    override fun createViewModel() = MainViewModel(application)

    override fun initData() {
        super.initData()
        AppTheme.setStatusBarColor(this, R.color.black)
        LeakCanary.runCatching {  }
        initList()
        viewModel.initBle()
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun initEvent() {
        super.initEvent()
        lifecycleScope.launch {
            //添加扫描到的设备 刷新列表
            viewModel.listDRStateFlow.collect {
                if (it.deviceName != null && it.deviceAddress != null) {
                    val position = (listAdapter?.itemCount?: 1) - 1
                    listAdapter?.notifyItemInserted(position)
                    viewBinding.recyclerView.smoothScrollToPosition(position)
                }
            }
        }

        lifecycleScope.launch {
            viewModel.scanStopStateFlow.collect {
                viewBinding.pbLoading.visibility = if (it) { View.INVISIBLE } else { View.VISIBLE }
                viewBinding.btnStart.text = if (it) { "开启扫描" } else { "扫描中..." }
                viewBinding.btnStart.isEnabled = it
                viewBinding.btnConnect.isEnabled = it
                viewBinding.btnSetting.isEnabled = it
                viewBinding.btnStop.isEnabled = !it
            }
        }

        lifecycleScope.launch {
            //连接设备后 刷新列表
            viewModel.refreshStateFlow.collect {
                delay(300)
                dismissLoading()
                if (it?.bleDevice == null) {
                    listAdapter?.notifyDataSetChanged()
                    return@collect
                }
                it.bleDevice.let { bleDevice ->
                    val position = listAdapter?.data?.indexOf(bleDevice) ?: -1
                    if (position >= 0) {
                        listAdapter?.notifyItemChanged(position)
                    }
                    val isConnected= viewModel.isConnected(bleDevice)
                    if (it.bleDevice.deviceAddress == viewBinding.etAddress.text.toString()) {
                        viewBinding.btnConnect.isEnabled = !isConnected
                    }
                    if (isConnected && autoOpenDetailsActivity) {
                        openDetails(it.bleDevice)
                    }
                    autoOpenDetailsActivity = false
                }
            }
        }

        listAdapter?.addChildClickViewIds(R.id.btnConnect, R.id.btnOperate)
        listAdapter?.setOnItemChildClickListener { adapter, view, position ->
            if (ViewUtil.isInvalidClick(view)) {
                return@setOnItemChildClickListener
            }
            val bleDevice: BleDevice? = adapter.data[position] as BleDevice?
            if (view.id == R.id.btnConnect) {
                if (viewModel.isConnected(bleDevice)) {
                    showLoading("断开中...")
                    viewModel.disConnect(bleDevice)
                } else {
                    showLoading("连接中...")
                    viewModel.connect(bleDevice)
                }
            } else if (view.id == R.id.btnOperate) {
                openDetails(bleDevice)
            }
        }

        viewBinding.btnConnect.setOnClickListener {
            if (ViewUtil.isInvalidClick(it)) {
                return@setOnClickListener
            }
            requestPermission(
                LOCATION_PERMISSION,
                {
                    BleLogger.d("获取到了权限")
                    val address = viewBinding.etAddress.text.toString()
                    if (address.isEmpty()) {
                        Toast.makeText(application, "请输入设备地址", Toast.LENGTH_SHORT).show()
                        return@requestPermission
                    }
                    if (!Regex("^([0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}$").matches(address)) {
                        Toast.makeText(application, "请输入正确的设备地址", Toast.LENGTH_SHORT).show()
                        return@requestPermission
                    }
                    autoOpenDetailsActivity = true
                    showLoading("连接中...")
//            viewModel.startScanAndConnect(this@MainActivity)
                    viewModel.connect(viewBinding.etAddress.text.toString())

                }, {
                    BleLogger.w("缺少定位权限")
                }
            )
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
            listAdapter?.notifyItemRangeRemoved(0, viewModel.listDRData.size)
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
        val layoutManager = LinearLayoutManager(this)
        layoutManager.orientation = LinearLayoutManager.VERTICAL
        viewBinding.recyclerView.setHasFixedSize(true)
        viewBinding.recyclerView.layoutManager = layoutManager
        viewBinding.recyclerView.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        //解决RecyclerView局部刷新时闪烁
        (viewBinding.recyclerView.itemAnimator as DefaultItemAnimator).supportsChangeAnimations = false
        listAdapter = DeviceListAdapter(viewModel.listDRData)
        viewBinding.recyclerView.adapter = listAdapter
    }

    /**
     * 打开操作页面
     */
    private fun openDetails(bleDevice: BleDevice?) {
        if (viewModel.isConnected(bleDevice)) {
            val intent = Intent(this@MainActivity, DetailOperateActivity::class.java)
            intent.putExtra("data", bleDevice)
            intent.putExtra("disConnectWhileClose", autoOpenDetailsActivity)
            startActivity(intent) { _, resultIntent ->
                if (resultIntent != null) {
                    showLoading("断开中...")
                    //断开需要一定的时间，才可以连接，这里防止没断开完成，马上点击连接
                    lifecycleScope.launch {
                        delay(1200)
                        dismissLoading()
                    }
                }
            }
        } else {
            Toast.makeText(application, "设备未连接", Toast.LENGTH_SHORT).show()
            val index = listAdapter?.data?.indexOf(bleDevice) ?: -1
            if (index >= 0) {
                listAdapter?.notifyItemChanged(index)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.stopScan()
        viewModel.close()
    }
}