package com.bhm.demo

import android.bluetooth.BluetoothDevice
import android.view.LayoutInflater
import android.view.ViewGroup
import com.bhm.demo.databinding.LayoutRecyclerItemBinding
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder


/**
 * 设备列表
 *
 * @author Buhuiming
 * @date 2023年05月18日 11时06分
 */
class DeviceListAdapter(data: MutableList<BluetoothDevice>?
) : BaseQuickAdapter<BluetoothDevice, DeviceListAdapter.VH>(0, data) {

    class VH(
        parent: ViewGroup,
        val binding: LayoutRecyclerItemBinding = LayoutRecyclerItemBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        ),
    ) : BaseViewHolder(binding.root)

    override fun onCreateDefViewHolder(parent: ViewGroup, viewType: Int): VH {
        return VH(parent)
    }

    override fun convert(holder: VH, item: BluetoothDevice) {
        holder.binding.tvName.text = ""
    }
}