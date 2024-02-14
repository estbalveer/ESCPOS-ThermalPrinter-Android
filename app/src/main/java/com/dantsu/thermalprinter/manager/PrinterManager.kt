package com.dantsu.thermalprinter.manager

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Parcelable
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.dantsu.escposprinter.EscPosCharsetEncoding
import com.dantsu.escposprinter.EscPosPrinter
import com.dantsu.escposprinter.connection.DeviceConnection
import com.dantsu.escposprinter.connection.usb.UsbConnection
import com.dantsu.escposprinter.exceptions.EscPosBarcodeException
import com.dantsu.escposprinter.exceptions.EscPosConnectionException
import com.dantsu.escposprinter.exceptions.EscPosEncodingException
import com.dantsu.escposprinter.exceptions.EscPosParserException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PrinterManager {

    private val ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION"
    private var onPermissionGranted: (usbDevice: UsbDevice?) -> Unit = {}

    fun setOnOnPermissionGranted(onPermissionGranted: (usbDevice: UsbDevice?) -> Unit) {
        this.onPermissionGranted = onPermissionGranted
    }

    private val usbReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (ACTION_USB_PERMISSION == action) {
                synchronized(this) {
                    val usbDevice =
                        intent.getParcelableExtra<Parcelable>(UsbManager.EXTRA_DEVICE) as UsbDevice?
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (usbDevice != null) {
                            onPermissionGranted(usbDevice)
                        }
                    }
                }
            }
        }
    }

    fun askUsbPermission(context: Context, usbConnection: UsbConnection) {
        val usbManager = context.getSystemService(AppCompatActivity.USB_SERVICE) as UsbManager
        val permissionIntent = PendingIntent.getBroadcast(
            context,
            0,
            Intent(ACTION_USB_PERMISSION),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
        )
        val filter = IntentFilter(ACTION_USB_PERMISSION)
        context.registerReceiver(usbReceiver, filter)
        usbManager.requestPermission(usbConnection.device, permissionIntent)
    }

    fun connectPrinter(
        deviceConnection: DeviceConnection,
        callback: (printer: EscPosPrinter?) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val printer = EscPosPrinter(
                    deviceConnection,
                    203, 48f, 32,
                    EscPosCharsetEncoding("windows-1252", 16)
                )
                Log.v("Printer", "Printer $printer")
                callback(printer)
                // Your code that may throw EscPosConnectionException, EscPosParserException, EscPosEncodingException, or InterruptedException
            } catch (e: EscPosConnectionException) {
                e.printStackTrace()
                callback(null)
            } catch (e: InterruptedException) {
                callback(null)
                e.printStackTrace()
            } catch (e: Exception) {
                callback(null)
                e.printStackTrace()
            }
        }
    }

    fun printText(printer: EscPosPrinter, textsToPrint: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                printer.printFormattedTextAndCut(textsToPrint)
//            Thread.sleep(500)
            } catch (e: EscPosConnectionException) {
                e.printStackTrace()
            } catch (e: EscPosParserException) {
                e.printStackTrace()
            } catch (e: EscPosEncodingException) {
                e.printStackTrace()
            } catch (e: EscPosBarcodeException) {
                e.printStackTrace()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}