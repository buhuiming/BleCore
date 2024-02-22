package com.bhm.support.sdk.utils

import android.annotation.SuppressLint
import android.content.Context

/**
 * @author Buhuiming
 * @description: SharedPreferences
 * @date :2022/6/30 14:15
 */
class SPUtil(context: Context) {

    companion object {
        @SuppressLint("StaticFieldLeak")
        private lateinit var instance: SPUtil

        fun getInstance(context: Context): SPUtil {
            if (!Companion::instance.isInitialized) {
                instance = SPUtil(context.applicationContext)
            }
            return instance
        }
    }

    private val sp = context.getSharedPreferences(context.packageName, Context.MODE_PRIVATE)
    private val editor = sp.edit()

    fun getString(key: String): String? = sp.getString(key, null)

    fun getInt(key: String): Int = sp.getInt(key, 0)

    fun getBoolean(key: String): Boolean = sp.getBoolean(key, false)

    fun getFloat(key: String): Float = sp.getFloat(key, 0.toFloat())

    fun putString(key: String, value: String?) {
        editor.putString(key, value).apply()
    }

    fun putInt(key: String, value: Int) {
        editor.putInt(key, value).apply()
    }

    fun putBoolean(key: String, value: Boolean) {
        editor.putBoolean(key, value).apply()
    }

    fun put(key: String, value: Float) {
        editor.putFloat(key, value).apply()
    }

    /**
     * 清空用户数据
     */
    fun clear() {
        editor.clear().apply()
    }
}