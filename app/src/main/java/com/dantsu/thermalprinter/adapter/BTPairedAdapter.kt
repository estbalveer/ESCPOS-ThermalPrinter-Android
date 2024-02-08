package com.dantsu.thermalprinter.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection
import com.dantsu.thermalprinter.databinding.ItemDeviceBinding


class BTPairedAdapter(var list: Array<BluetoothConnection>) :
    RecyclerView.Adapter<BTPairedAdapter.MyViewHolder>() {

    inner class MyViewHolder(binding: ItemDeviceBinding) : ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val binding = ItemDeviceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
    }
}