package com.bhm.support.sdk.common

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import com.bhm.support.sdk.utils.ViewUtil

/**
 * @author Buhuiming
 * @description: ViewBinding基类
 * @date :2022/6/28 14:58
 */
abstract class BaseVBFragment <VM : BaseViewModel, B : ViewBinding> : BaseFragment<VM>(){

    lateinit var viewBinding: B

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewBinding = ViewUtil.inflateWithGeneric(this, layoutInflater)
        initView()
        initEvent()
        return viewBinding.root
    }

    protected open fun initView() {}

    protected open fun initEvent() {}
}