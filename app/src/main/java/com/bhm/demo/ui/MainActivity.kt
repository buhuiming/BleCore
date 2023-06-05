package com.bhm.demo.ui

import android.content.Intent
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.bhm.ble.callback.BleRssiCallback
import com.bhm.ble.control.BleTask
import com.bhm.ble.control.BleTaskQueue
import com.bhm.ble.data.BleDevice
import com.bhm.ble.utils.BleLogger
import com.bhm.demo.BaseActivity
import com.bhm.demo.R
import com.bhm.demo.adapter.DeviceListAdapter
import com.bhm.demo.databinding.ActivityMainBinding
import com.bhm.demo.vm.MainViewModel
import com.bhm.support.sdk.core.AppTheme
import com.bhm.support.sdk.utils.ViewUtil
import kotlinx.coroutines.*
import leakcanary.LeakCanary
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * 主页面
 * @author Buhuiming
 * @date :2023/5/24 15:39
 */
class MainActivity : BaseActivity<MainViewModel, ActivityMainBinding>(){

    private var listAdapter: DeviceListAdapter? = null

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
                viewBinding.btnSetting.isEnabled = it
                viewBinding.btnStop.isEnabled = !it
            }
        }

        lifecycleScope.launch {
            viewModel.refreshStateFlow.collect {
                delay(300)
                dismissLoading()
                it?.bleDevice?.let { bleDevice ->
                    val position = listAdapter?.data?.indexOf(bleDevice) ?: -1
                    if (position >= 0) {
                        listAdapter?.notifyItemChanged(position)
                    }
                    BleLogger.i("item isConnected: ${viewModel.isConnected(bleDevice)}")
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
                val intent = Intent(this@MainActivity, DetailOperateActivity::class.java)
                intent.putExtra("data", bleDevice)
                startActivity(intent)
            }
        }

        viewBinding.btnSetting.setOnClickListener {
            if (ViewUtil.isInvalidClick(it)) {
                return@setOnClickListener
            }
//            startActivity(Intent(this@MainActivity, OptionSettingActivity::class.java))
            job1 = null
            job2 = null
            job3 = null
            BleTaskQueue.get().addTask(testTask1)
            BleTaskQueue.get().addTask(testTask2)
            BleTaskQueue.get().addTask(testTask3)
        }

        viewBinding.btnStart.setOnClickListener {
            if (ViewUtil.isInvalidClick(it)) {
                return@setOnClickListener
            }
//            listAdapter?.notifyItemRangeRemoved(0, viewModel.listDRData.size)
//            viewModel.listDRData.clear()
//            viewModel.startScan(this@MainActivity)
//            jobs[0].cancel("手动测试取消")
            BleTaskQueue.get().removeRunningTask()
        }

        viewBinding.btnStop.isEnabled = true
        viewBinding.btnStop.setOnClickListener {
            if (ViewUtil.isInvalidClick(it)) {
                return@setOnClickListener
            }
//            viewModel.stopScan()
            BleTaskQueue.get().removeTask(testTask2)
        }
    }

    private var job1: Job? = null

    private var job2: Job? = null

    private var job3: Job? = null

    private val testTask1 = BleTask(
        durationTimeMillis = 3000L,
        callInMainThread = false,
        autoDoNextTask = true,
        block = {
            testJob(1)
        }, interrupt = {
            job1?.cancel("手动测试取消")
        }
    )

    private val testTask2 = BleTask(
        durationTimeMillis = 7000L,
        callInMainThread = false,
        autoDoNextTask = true,
        block = {
            testJob(2)
        }, interrupt = {
            job2?.cancel("手动测试取消")
        }
    )

    private val testTask3 = BleTask(
        durationTimeMillis = 15000L,
        callInMainThread = true,
        autoDoNextTask = true,
        block = {
            testJob(3)
        }, interrupt = {
            job3?.cancel("手动测试取消")
        }
    )

    private suspend fun testJob(i: Int) {
        suspendCoroutine { continuation ->
            val callback = BleRssiCallback()
            val job = CoroutineScope(Dispatchers.IO).launch {
                repeat(5) {
                    BleLogger.e("${i}执行子任务：$it")
                    if (i == 2 && it == 3) {
//                        continuation.resumeWithException(Throwable("测试取消"))
                        cancel(CancellationException("自动测试取消"))
                    }
                    delay(1000)
                }
                callback.callRssiSuccess(100)
                BleLogger.e("testJob${i}任务完成")
            }
            job.invokeOnCompletion {
                BleLogger.e("${it?.message}")
                continuation.resume(it == null)
            }
            when(i) {
                1 -> job1 = job
                2 -> job2 = job
                3 -> job3 = job
            }
        }

//        repeat(5) {
//            BleLogger.e("${i}执行子任务：$it")
//            if (i == 2 && it == 3) {
////                        continuation.resumeWithException(Throwable("测试取消"))
//                return
//            }
//            delay(1000)
//        }
//        BleLogger.e("testJob${i}任务完成")
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

    override fun onDestroy() {
        super.onDestroy()
        viewModel.stopScan()
        viewModel.release()
        BleTaskQueue.get().clear()
    }
}