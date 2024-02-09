package com.dantsu.thermalprinter.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection
import com.dantsu.thermalprinter.databinding.ItemDeviceBinding


class BTPairedAdapter(
    val onConnectClick: (model: BluetoothConnection) -> Unit,
    val onPrintClick: (model: BluetoothConnection) -> Unit
) :
    RecyclerView.Adapter<BTPairedAdapter.MyViewHolder>() {

    private var list: ArrayList<BluetoothConnection> = arrayListOf()

    fun setList(list: ArrayList<BluetoothConnection>) {
        this.list = list
        notifyDataSetChanged()
    }

    inner class MyViewHolder(val binding: ItemDeviceBinding) : ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val binding = ItemDeviceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    @SuppressLint("MissingPermission")
    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val model = list[position]
        with(holder.binding) {
            deviceName.setText(model.device.name)

            btConnect.setOnClickListener { onConnectClick(model) }
            btPrint.setOnClickListener { onPrintClick(model) }
        }
    }
}