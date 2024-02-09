package com.dantsu.thermalprinter.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection
import com.dantsu.thermalprinter.R
import com.dantsu.thermalprinter.databinding.ItemDeviceBinding
import com.dantsu.thermalprinter.model.BTDevicesModel


class BTPairedAdapter(
    val onConnectClick: (model: BTDevicesModel) -> Unit,
    val onPrintClick: (model: BTDevicesModel) -> Unit
) :
    RecyclerView.Adapter<BTPairedAdapter.MyViewHolder>() {

    private var list: ArrayList<BTDevicesModel> = arrayListOf()

    fun setList(list: ArrayList<BTDevicesModel>) {
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
            deviceName.setText(model.bluetoothConnection?.device?.name)
            if (model.connectionStatus) {
                circle.setBackgroundResource(R.drawable.circle_green)
                btConnect.visibility = View.GONE
                btPrint.visibility = View.VISIBLE
            } else {
                circle.setBackgroundResource(R.drawable.circle_red)
                btConnect.visibility = View.VISIBLE
                btPrint.visibility = View.GONE
            }

            btConnect.setOnClickListener { onConnectClick(model) }
            btPrint.setOnClickListener { onPrintClick(model) }
        }
    }
}