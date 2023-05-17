/*
 * Copyright (c) 2022-2032 上海微创卜算子医疗科技有限公司
 * 不能修改和删除上面的版权声明
 * 此代码属于上海微创卜算子医疗科技有限公司编写，在未经允许的情况下不得传播复制
 */
package com.bhm.demo

import com.bhm.demo.databinding.ActivityMainBinding
import com.bhm.support.sdk.common.BaseVBActivity
import com.bhm.support.sdk.common.BaseViewModel
import com.bhm.support.sdk.core.AppTheme
import leakcanary.LeakCanary


class MainActivity : BaseVBActivity<BaseViewModel, ActivityMainBinding>(){

    override fun createViewModel() = BaseViewModel(application)

    override fun initData() {
        super.initData()
        AppTheme.setStatusBarColor(this, R.color.purple_500)
        LeakCanary.runCatching {  }
    }
}