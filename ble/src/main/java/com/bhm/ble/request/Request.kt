/*
 * Copyright (c) 2022-2032 buhuiming
 * 不能修改和删除上面的版权声明
 * 此代码属于buhuiming编写，在未经允许的情况下不得传播复制
 */
package com.bhm.ble.request

import com.bhm.ble.BleManager
import com.bhm.ble.data.Constants.DEFAULT_OPERATE_MILLIS_TIMEOUT


/**
 * 所有Request的基类
 * @author Buhuiming
 * @date 2023年05月26日 13时59分
 */
internal open class Request {

    fun getBleManager() = BleManager.get()

    fun getBleOptions() = getBleManager().getOptions()

    fun getOperateTime(): Long {
        var operateTime = getBleOptions()?.operateMillisTimeOut?: DEFAULT_OPERATE_MILLIS_TIMEOUT
        if (operateTime <= 0) {
            operateTime = DEFAULT_OPERATE_MILLIS_TIMEOUT
        }
        return operateTime
    }
}