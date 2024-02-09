package com.dantsu.thermalprinter.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.dantsu.thermalprinter.R
import com.dantsu.thermalprinter.databinding.ItemDeviceBinding
import com.dantsu.thermalprinter.model.USBDevicesModel


class UsbAdapter : RecyclerView.Adapter<UsbAdapter.MyViewHolder>() {

    private var list: ArrayList<USBDevicesModel> = arrayListOf()
    var onConnectClick: (model: USBDevicesModel) -> Unit = {}
    var onPrintClick: (model: USBDevicesModel) -> Unit = {}
    var onRequestClick: (model: USBDevicesModel) -> Unit = {}

    fun setList(list: ArrayList<USBDevicesModel>) {
        this.list = list
        notifyDataSetChanged()
    }

    fun setClickListener(
        onConnectClick: (model: USBDevicesModel) -> Unit = {},
        onPrintClick: (model: USBDevicesModel) -> Unit = {},
        onRequestClick: (model: USBDevicesModel) -> Unit = {}
    ) {
        this.onConnectClick = onConnectClick
        this.onPrintClick = onPrintClick
        this.onRequestClick = onRequestClick
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
            deviceName.text = model.usbConnection?.device?.deviceName

            if (model.hasPermission) {
                btPermission.visibility = View.GONE

                if (model.connectionStatus) {
                    circle.setBackgroundResource(R.drawable.circle_green)
                    btConnect.visibility = View.GONE
                    btPrint.visibility = View.VISIBLE
                } else {
                    circle.setBackgroundResource(R.drawable.circle_red)
                    btConnect.visibility = View.VISIBLE
                    btPrint.visibility = View.GONE
                }
            } else {
                btPermission.visibility = View.VISIBLE
                btConnect.visibility = View.GONE
                btPrint.visibility = View.GONE
            }

            btConnect.setOnClickListener { onConnectClick(model) }
            btPrint.setOnClickListener { onPrintClick(model) }
            btPermission.setOnClickListener { onRequestClick(model) }
        }
    }
}