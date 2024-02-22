package com.bhm.support.sdk.common

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import androidx.multidex.MultiDex
import com.bhm.support.sdk.constants.DEBUGGER
import timber.log.Timber

/**
 * @author Buhuiming
 * @description: Application基类
 * @date :2022/6/28 14:14
 */
open class BaseApplication : Application(){

    companion object {
        @SuppressLint("StaticFieldLeak")
        private lateinit var context: Context
        fun getContext(): Context = context
    }

    override fun onCreate() {
        super.onCreate()
        context = this
        if (isDebugger()) {
            Timber.plant(Timber.DebugTree())
        }
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        MultiDex.install(base)
    }

    open fun isDebugger() = DEBUGGER
}