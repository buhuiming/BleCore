package com.bhm.support.sdk.utils

import java.text.SimpleDateFormat
import java.util.*

/**
 * @author Buhuiming
 * @date :2022/10/9 13:49
 */
object DateUtil {

    @JvmStatic
    fun getMmDdEEEE() : String {
        val date = Date()
        val datetime = SimpleDateFormat("MM月dd日 EEEE", Locale.getDefault())
        return datetime.format(date)
    }

    @JvmStatic
    fun getOneDate(limit: Int): IntArray {
        return getOneDate(limit, false)
    }

    /** 获取某个时间段
     * @param limit
     * @return
     */
    @JvmStatic
    fun getOneDate(limit: Int, isShiFen: Boolean): IntArray {
        val startDates = IntArray(6)
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, limit)
        startDates[0] = calendar[Calendar.YEAR]
        startDates[1] = calendar[Calendar.MONTH] + 1
        startDates[2] = calendar[Calendar.DAY_OF_MONTH]
        if (isShiFen) {
            startDates[3] = calendar[Calendar.HOUR_OF_DAY]
            startDates[4] = calendar[Calendar.MINUTE]
            startDates[5] = calendar[Calendar.SECOND]
        } else {
            startDates[3] = 0
            startDates[4] = 0
            startDates[5] = 0
        }
        return startDates
    }
}