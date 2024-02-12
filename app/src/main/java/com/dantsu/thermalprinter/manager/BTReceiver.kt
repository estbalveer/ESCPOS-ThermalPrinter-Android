package com.dantsu.thermalprinter.manager

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BTReceiver(val deviceDisconnected: (device: BluetoothDevice) -> Unit) : BroadcastReceiver() {
    @SuppressLint("MissingPermission")
    override fun onReceive(p0: Context?, p1: Intent?) {
        val action = p1?.action
        val connectedDevice = p1?.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
        val deviceName = connectedDevice?.name

        when (action) {
            BluetoothDevice.ACTION_ACL_CONNECTED -> {
                Log.v("BTReceiver", "Connected $deviceName")
            }

            BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                Log.v("BTReceiver", "Disconnected $deviceName")
                if (connectedDevice != null) {
                    deviceDisconnected(connectedDevice)
                }
            }
        }
    }
}