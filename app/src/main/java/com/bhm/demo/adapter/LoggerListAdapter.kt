package com.bhm.demo.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import com.bhm.demo.databinding.LayoutRecyclerLogBinding
import com.bhm.demo.entity.LogEntity
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import java.text.SimpleDateFormat
import java.util.*
import java.util.logging.Level


/**
 * 日志输出列表
 *
 * @author Buhuiming
 * @date 2023年6月1日 15时51分
 */
class LoggerListAdapter(data: MutableList<LogEntity>?
) : BaseQuickAdapter<LogEntity, LoggerListAdapter.VH>(0, data) {

    class VH(
        parent: ViewGroup,
        val binding: LayoutRecyclerLogBinding = LayoutRecyclerLogBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        ),
    ) : BaseViewHolder(binding.root)

    override fun onCreateDefViewHolder(parent: ViewGroup, viewType: Int): VH {
        return VH(parent)
    }

    override fun convert(holder: VH, item: LogEntity) {
        val df = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CANADA)
        holder.binding.tvTime.text = buildString {
            append(df.format(System.currentTimeMillis()))
            append(": ")
        }
        holder.binding.tvName.text = item.msg
        when (item.level) {
            Level.INFO -> holder.binding.tvName.setTextColor(Color.BLUE)
            Level.WARNING -> holder.binding.tvName.setTextColor(Color.parseColor("#FF9800"))
            Level.OFF -> holder.binding.tvName.setTextColor(Color.RED)
            Level.FINE -> holder.binding.tvName.setTextColor(Color.BLACK)
            else -> holder.binding.tvName.setTextColor(Color.GRAY)
        }
    }
}