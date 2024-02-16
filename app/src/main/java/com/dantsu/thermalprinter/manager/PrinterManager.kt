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
import android.util.DisplayMetrics
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
import com.dantsu.escposprinter.textparser.PrinterTextParserImg
import com.dantsu.thermalprinter.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PrinterManager {

    companion object {
        val CONNECTION_SUCCESS = 1
        val CONNECTION_FAIL = 2

        val PRINT_SUCCESS = 1
        val PRINT_FAIL = 2
    }

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
        callback: (printer: EscPosPrinter?, status: Int) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val printer = EscPosPrinter(
                    deviceConnection,
                    203, 72f, 48,
                    EscPosCharsetEncoding("windows-1252", 16)
                )
                Log.v("Printer", "Printer $printer")
                callback(printer, CONNECTION_SUCCESS)
                // Your code that may throw EscPosConnectionException, EscPosParserException, EscPosEncodingException, or InterruptedException
            } catch (e: EscPosConnectionException) {
                e.printStackTrace()
                callback(null, CONNECTION_FAIL)
            } catch (e: InterruptedException) {
                callback(null, CONNECTION_FAIL)
                e.printStackTrace()
            } catch (e: Exception) {
                callback(null, CONNECTION_FAIL)
                e.printStackTrace()
            }
        }
    }

    fun printText(
        context: Context,
        printer: EscPosPrinter,
        callback: (status: Int) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val drawable = context.applicationContext.resources
                    .getDrawableForDensity(
                        R.drawable.receipt_logo,
                        DisplayMetrics.DENSITY_MEDIUM
                    )

                val image = PrinterTextParserImg.bitmapToHexadecimalString(
                    printer,
                    drawable
                )

                printer.printFormattedTextAndCut(orderReceipt().toString())
                callback(PRINT_SUCCESS)
            } catch (e: EscPosConnectionException) {
                callback(PRINT_FAIL)
                e.printStackTrace()
            } catch (e: EscPosParserException) {
                callback(PRINT_FAIL)
                e.printStackTrace()
            } catch (e: EscPosEncodingException) {
                callback(PRINT_FAIL)
                e.printStackTrace()
            } catch (e: EscPosBarcodeException) {
                callback(PRINT_FAIL)
                e.printStackTrace()
            } catch (e: InterruptedException) {
                callback(PRINT_FAIL)
                e.printStackTrace()
            } catch (e: Exception) {
                callback(PRINT_FAIL)
                e.printStackTrace()
            }
        }
    }

    private fun orderReceipt(): PrinterText {
        return PrinterText().apply {
            text("FoodZone, Vijay Nagar")
            text("Indore")
            text("Tel: 1234567654")
            text("Email: balveer.dhanoriya@encoresky.com")
            text("Date & Time: 2024-02-15 16:21:40")
            text("Invoice No: 39583939")
            newLine()
            text("ORDER RECEIPT")
            divider()
            row2("Qty Item", "Price")
            divider()
            row2("2 Cheese Burger ($22.03)", "$44.06")
            row2("1 Veggie Burger ($22.03)", "$415.06")
            divider()
            row2("Total Items", "2")
            row2("SubTotal", "$59.06")
            row2("Srv Charges (10.0%)", "$5.91")
            row2("GST (10.0%) (Inclusive)", "$4.81")
            row2("Rounding", "$0.00")
            row2("Cash Received", "$0.00")
            row2("Change", "$0.00")
            row2("Total", "$64.97")
            row2("Payment Method", "VISA")
            divider()
            text("Thank You!")
            text("Powered by Warely POS")
            newLine()
            newLine()
        }
    }

    private fun kitchenReceipt(): PrinterText {
        return PrinterText().apply {
            text("### KITCHEN RECEIPT ###")
            text("Invoice No: 202402164526")
            newLine()
            divider()
            text("COMMON")
            divider()
            newLine()
            text("--- DINE-IN ---", fontSize = FontSize.BIG)
            newLine()
            text("2 Grill Chicken", fontSize = FontSize.BIG, align = Alignment.LEFT)
            newLine()
            text("1 Tag Number #40", fontSize = FontSize.BIG, align = Alignment.LEFT)
            newLine()
            text("1 Mix Veg", fontSize = FontSize.BIG, align = Alignment.LEFT)
            newLine()
            divider()
            row2("Check No: 0621", "2024-02-16 14:01:25")
            newLine()
            text("Powered by Warely POS")
            newLine()
            newLine()
        }
    }
}