package com.bhm.support.sdk.core

import android.os.Handler
import android.os.Looper
import android.os.Message
import java.lang.ref.WeakReference

/**
 * @author Buhuiming
 * @description: 管理handler，防止内存泄露
 * @date :2022/6/28 16:02
 */
open class WeakHandler(looper: Looper?, cb: Callback?) : Handler(looper!!) {

    private var activityWeakReference: WeakReference<Callback?>

    init {
        activityWeakReference = WeakReference<Callback?>(cb)
    }

    fun setCallback(cb: Callback?) {
        activityWeakReference = WeakReference<Callback?>(cb)
    }

    override fun handleMessage(msg: Message) {
        val cb = activityWeakReference.get()
        cb?.handleMessage(msg)
    }
}