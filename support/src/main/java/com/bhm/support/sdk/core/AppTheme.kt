package com.bhm.support.sdk.core

import android.app.Activity
import android.content.Context
import android.os.Build
import android.view.View
import android.view.WindowManager
import androidx.core.content.ContextCompat

/**
 * 沉浸式状态栏样式
 */
@Suppress("DEPRECATION", "unused")
object AppTheme {
    /**
     * 将内容提升至状态栏
     */
    fun fitSystemWindow(activity: Activity) {
        activity.window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        setStatusBarColor(activity, android.R.color.transparent)
    }

    /**
     * 将内容提升至状态栏
     */
    fun fitSystemLightWindow(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            activity.window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            setStatusBarColor(activity, android.R.color.transparent)
        }
    }

    /**
     * 设置导航栏颜色
     */
    fun setNavigationBarColor(activity: Activity, colorResId: Int) {
        val color = ContextCompat.getColor(activity, colorResId)
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            activity.window.navigationBarColor = color
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            activity.window.navigationBarDividerColor = color
        }
    }

    /**
     * 设置状态栏颜色
     */
    fun setStatusBarColor(activity: Activity, colorResId: Int) {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            val window = activity.window
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = ContextCompat.getColor(activity, colorResId)
        }
    }

    /**
     * 设置状态栏Light主题
     */
    fun setLightStatusBar(activity: Activity) {
        val window = activity.window
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }
    }

    /**
     * 设置状态栏Dark主题
     */
    fun setDarkStatusBar(activity: Activity) {
        val window = activity.window
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        }
    }

    /**
     * Android官方标准的系统UI全屏样式
     * 重写Activity的onWindowFocusChanged方法
     * 在获取到焦点时调用
     * if (hasFocus) initWindowStyle();
     */
    fun fullScreenStyle(activity: Activity) {
        activity.window
            .decorView.systemUiVisibility =
            (View.SYSTEM_UI_FLAG_LAYOUT_STABLE //防止系统栏隐藏时内容区域大小发生变化
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION //隐藏导航栏
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN //全屏
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION //隐藏底部的 三个 虚拟按键导航栏
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    /**
     * 获取状态栏高度
     */
    fun getStatusBarHeight(context: Context): Int {
        var statusBarHeight = 0
        val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            statusBarHeight = context.resources.getDimensionPixelSize(resourceId)
        }
        return statusBarHeight
    }

    /**
     * 获取导航栏高度
     */
    fun getNavigationBarHeight(context: Context): Int {
        var navigationBarHeight = 0
        val rid = context.resources.getIdentifier("config_showNavigationBar", "bool", "android")
        if (rid != 0) {
            val resourceId =
                context.resources.getIdentifier("navigation_bar_height", "dimen", "android")
            navigationBarHeight = context.resources.getDimensionPixelSize(resourceId)
        }
        return navigationBarHeight
    }

    /**
     * 是否有底部导航栏
     */
    fun isHaveNavigationBar(context: Context): Boolean {
        val rs = context.resources
        val id = rs.getIdentifier("config_showNavigationBar", "bool", "android")
        return rs.getBoolean(id)
    }
}