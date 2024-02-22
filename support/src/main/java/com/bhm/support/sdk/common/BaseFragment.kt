@file:Suppress("UNCHECKED_CAST")

package com.bhm.support.sdk.common

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.Message
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.bhm.support.sdk.core.WeakHandler

/**
 * @author Buhuiming
 * @description: Fragment基类
 * @date :2022/6/28 14:09
 */
abstract class BaseFragment<VM : BaseViewModel> : Fragment(), Handler.Callback {

    lateinit var viewModel: VM

    lateinit var activity: BaseActivity<BaseViewModel>

    lateinit var mainHandler: WeakHandler

    var isFirstLoad = true // 是否第一次加载

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity = context as BaseActivity<BaseViewModel>
        init()
    }

    override fun onResume() {
        super.onResume()
        if (isFirstLoad) {
            // 将数据加载逻辑放到onResume()方法中
            lazyLoad()
            isFirstLoad = false
            return
        }

        if (!setLoadDataOnce()) {
            //每次可见都加载
            lazyLoad()
        }
    }

    fun setLoadDataOnce(): Boolean = true

    fun lazyLoad() {

    }

    override fun onDestroyView() {
        super.onDestroyView()
        isFirstLoad = true
    }

    override fun onDestroy() {
        super.onDestroy()
        mainHandler.removeCallbacksAndMessages(null)
    }

    /**
     * ViewModel绑定
     */
    private fun init() {
        viewModel = createViewModel(this, createViewModel())
        mainHandler = WeakHandler(Looper.getMainLooper(), this)
    }

    /**
     * 创建ViewModel
     */
    abstract fun createViewModel(): VM

    private fun createViewModel(owner: ViewModelStoreOwner, viewModel: VM): VM {
        return ViewModelProvider(owner).get(viewModel.javaClass)
    }

    override fun handleMessage(msg: Message): Boolean {
        return false
    }
}