package com.dantsu.thermalprinter.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.dantsu.thermalprinter.R
import com.dantsu.thermalprinter.databinding.ItemDeviceBinding
import com.dantsu.thermalprinter.model.PrinterDevicesModel
import com.dantsu.thermalprinter.model.PrinterType


class PrinterDeviceAdapter(val context: Context) :
    RecyclerView.Adapter<PrinterDeviceAdapter.MyViewHolder>() {

    private var list: ArrayList<PrinterDevicesModel> = arrayListOf()
    var onConnectClick: ((model: PrinterDevicesModel, index: Int) -> Unit)? = null
    var onPrintClick: ((model: PrinterDevicesModel, index: Int) -> Unit)? = null
    var onRequestClick: ((model: PrinterDevicesModel, index: Int) -> Unit)? = null

    val green = ContextCompat.getColor(context, android.R.color.holo_green_dark);
    val red = ContextCompat.getColor(context, android.R.color.holo_red_dark);

    fun setList(list: ArrayList<PrinterDevicesModel>) {
        this.list = list
        notifyDataSetChanged()
    }

    fun setClickListener(
        onConnectClick: (model: PrinterDevicesModel, index: Int) -> Unit,
        onPrintClick: (model: PrinterDevicesModel, index: Int) -> Unit,
        onRequestClick: (model: PrinterDevicesModel, index: Int) -> Unit
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
            deviceName.text = model.printerName

            val icon = when (model.printerType) {
                PrinterType.BLUETOOTH -> R.drawable.bluetooth
                PrinterType.USB -> R.drawable.usb
                PrinterType.LAN -> R.drawable.lan
            }
            iconView.setImageResource(icon)
            btPermission.visibility = View.GONE
            loading.visibility = View.GONE

            if (model.connectionStatus) {
                ImageViewCompat.setImageTintList(iconView, ColorStateList.valueOf(green))
                btConnect.visibility = View.GONE
                btPrint.visibility = View.VISIBLE
            } else {
                ImageViewCompat.setImageTintList(iconView, ColorStateList.valueOf(red))
                btConnect.visibility = View.VISIBLE
                btPrint.visibility = View.GONE
            }

            if (model.printerType == PrinterType.USB && !model.hasPermission) {
                btPermission.visibility = View.VISIBLE
                btConnect.visibility = View.GONE
                btPrint.visibility = View.GONE
            }

            if (model.loading) {
                loading.visibility = View.VISIBLE
                btPermission.visibility = View.GONE
                btConnect.visibility = View.GONE
                btPrint.visibility = View.GONE
            }

            btConnect.setOnClickListener { onConnectClick?.invoke(model, position) }
            btPrint.setOnClickListener { onPrintClick?.invoke(model, position) }
            btPermission.setOnClickListener { onRequestClick?.invoke(model, position) }
        }
    }
}