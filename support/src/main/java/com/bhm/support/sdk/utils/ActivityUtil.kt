package com.bhm.support.sdk.utils

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import java.util.*

/**
 * @author Buhuiming
 * @description: 管理activity
 * @date :2022/6/28 14:28
 */
object ActivityUtil {

    private val activities: MutableList<Activity> = ArrayList()

    fun addActivity(activity: Activity) {
        activities.add(activity)
    }

    fun removeActivity(activity: Activity) {
        activities.remove(activity)
    }


    fun getActivities(): List<Activity> {
        return activities
    }

    /**
     * 关闭所有的Activity
     *
     * @param exceptClassName 如果不想关闭某一个activity, 将activity的className传入
     */
    fun finishAll(vararg exceptClassName: String?) {
        val names = mutableListOf(*exceptClassName)
        for (activity in activities) {
            if (!activity.isFinishing && !names.contains(activity.javaClass.name)) {
                activity.finish()
            }
        }
    }

    /**
     * 关闭某一个Activity
     * @param exceptClassName 将activity的className传入
     */
    fun finish(vararg exceptClassName: String?) {
        val names = mutableListOf(*exceptClassName)
        for (activity in activities) {
            if (!activity.isFinishing && names.contains(activity.javaClass.name)) {
                activity.finish()
            }
        }
    }

    fun moveTaskToBack(task: Activity) {
        task.moveTaskToBack(true)
    }

    fun moveTaskToFront(task: Activity) {
        getManager(task).moveTaskToFront(task.taskId, ActivityManager.MOVE_TASK_WITH_HOME)
    }

    private fun getManager(task: Activity): ActivityManager {
        return task.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    }

    private fun size(): Int {
        return activities.size - 1
    }
}