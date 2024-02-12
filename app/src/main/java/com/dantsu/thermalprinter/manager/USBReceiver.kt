package com.dantsu.thermalprinter.manager

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class USBReceiver(
    val usbChanged: () -> Unit,
) : BroadcastReceiver() {
    override fun onReceive(p0: Context?, p1: Intent?) {
//        Toast.makeText(p0, "Usb changed ", Toast.LENGTH_LONG).show()
        usbChanged()
    }
}