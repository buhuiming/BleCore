/*
 * Copyright (c) 2022-2032 buhuiming
 * 不能修改和删除上面的版权声明
 * 此代码属于buhuiming编写，在未经允许的情况下不得传播复制
 */
package com.bhm.demo

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.viewbinding.ViewBinding
import com.bhm.network.base.HttpActivity
import com.bhm.network.base.HttpLoadingDialog
import com.bhm.network.core.HttpOptions
import com.bhm.support.sdk.common.BaseViewModel
import com.bhm.support.sdk.core.AppTheme
import com.bhm.support.sdk.core.WeakHandler
import com.bhm.support.sdk.entity.MessageEvent
import com.bhm.support.sdk.utils.ActivityUtil
import com.bhm.support.sdk.utils.ViewUtil
import com.noober.background.BackgroundLibrary
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode


/**
 * demo activity基类
 *
 * @author Buhuiming
 * @date 2023年05月24日 16时06分
 */
abstract class BaseActivity<VM : BaseViewModel, B : ViewBinding> : HttpActivity(), Handler.Callback {

    lateinit var viewModel: VM

    private var activityLauncher: ActivityResultLauncher<Intent>? = null

    private var permissionLauncher: ActivityResultLauncher<Array<String>>? = null

    private var arCallback: ((resultCode: Int, resultIntent: Intent?) -> Unit)? = null

    private var permissionAgree: (() -> Unit)? = null

    private var permissionRefuse: ((refusePermissions: ArrayList<String>) -> Unit)? = null

    lateinit var mainHandler: WeakHandler

    lateinit var viewBinding: B

    lateinit var rootView: View

    private var httpOptions: HttpOptions? = null

    /**
     * 默认启用 edge-to-edge 并自动处理 system bars insets。
     * 如果子类自己处理（例如手动给某些 View 做 padding），可以重写为 false。
     */
    protected open fun handleEdgeToEdgeInsets(): Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        BackgroundLibrary.inject(this)
        super.onCreate(savedInstanceState)
        AppTheme.fitSystemWindow(this)
        ActivityUtil.addActivity(this)
        EventBus.getDefault().register(this)
        init()
        viewBinding = ViewUtil.inflateWithGeneric(this, layoutInflater)
        rootView = viewBinding.root
        setContentView(rootView)
        initBackHandling()
        initEdgeToEdge()
        initData()
        initEvent()
    }

    fun showLoading(msg: String? = "") {
        httpOptions = HttpOptions.create(this)
            .setLoadingDialog(HttpLoadingDialog())
            .setLoadingTitle(msg)
            .setDialogAttribute(
                true,
                cancelable = false,
                dialogDismissInterruptRequest = true
            )
            .build()
        httpOptions?.let {
            it.dialog?.showLoading(it)
        }
    }

    fun dismissLoading() {
        httpOptions?.dialog?.dismissLoading(this)
    }

    protected open fun initData() {}

    protected open fun initEvent() {}

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
        ActivityUtil.removeActivity(this)
        mainHandler.removeCallbacksAndMessages(null)
    }

    /**
     *ViewModel绑定
     */
    private fun init() {
        viewModel = createViewModel(this, createViewModel())
        activityLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result != null) {
                arCallback?.let {
                    it(result.resultCode, result.data)
                }
            }
        }
        permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) {
            val refusePermission: ArrayList<String> = ArrayList()
            it.keys.forEach { res ->
                if (it[res] == false) {
                    refusePermission.add(res)
                }
            }

            if (refusePermission.isNotEmpty()) {
                permissionRefuse?.let {
                    it(refusePermission)
                }
            } else {
                permissionAgree?.let {
                    it()
                }
            }
        }
        mainHandler = WeakHandler(Looper.getMainLooper(), this)
    }

    /**
     * 创建ViewModel
     */
    abstract fun createViewModel(): VM

    /** 是否屏蔽返回键
     * @return
     */
    protected open fun isRefusedBackPress(): Boolean {
        return false
    }

    private fun initBackHandling() {
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (isRefusedBackPress()) return
                    finish()
                }
            }
        )
    }

    private fun initEdgeToEdge() {
        if (!handleEdgeToEdgeInsets()) return

        WindowCompat.setDecorFitsSystemWindows(window, false)
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, insets ->
            val sysBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(sysBars.left, sysBars.top, sysBars.right, sysBars.bottom)
            insets
        }
        ViewCompat.requestApplyInsets(rootView)
    }

    private fun createViewModel(owner: ViewModelStoreOwner, viewModel: VM): VM {
        return ViewModelProvider(owner)[viewModel.javaClass]
    }

    fun startActivity(intent: Intent, arCallback: (resultCode: Int, resultIntent: Intent?) -> Unit) {
        this.arCallback = arCallback
        activityLauncher?.launch(intent)
    }

    fun requestPermission(permissions: Array<String>,
                          agree: () -> Unit,
                          refuse: (refusePermissions: ArrayList<String>) -> Unit
    ) {
        this.permissionAgree = agree
        this.permissionRefuse = refuse
        var allAgree = true
        for (permission in permissions) {
            if( ContextCompat.checkSelfPermission(this, permission) !=
                PackageManager.PERMISSION_GRANTED) {
                allAgree=false
                break
            }
        }
        if (allAgree) {
            permissionAgree?.let {
                it()
            }
            return
        }
        permissionLauncher?.launch(permissions)
    }

    override fun handleMessage(msg: Message): Boolean {
        return false
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    open fun onMessageEvent(event: MessageEvent?) {
        //EventBus Do something
    }
}