package com.dantsu.thermalprinter

import android.util.Log
import com.dantsu.escposprinter.EscPosCharsetEncoding
import com.dantsu.escposprinter.EscPosPrinter
import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection
import com.dantsu.escposprinter.exceptions.EscPosBarcodeException
import com.dantsu.escposprinter.exceptions.EscPosConnectionException
import com.dantsu.escposprinter.exceptions.EscPosEncodingException
import com.dantsu.escposprinter.exceptions.EscPosParserException

class BTPrinterManager {

    private var printer: EscPosPrinter? = null
    private var onConnectionChange: (status: Boolean) -> Unit = {}

    fun connectPrinter(deviceConnection: BluetoothConnection) {
        try {
            printer = EscPosPrinter(
                deviceConnection,
                203, 48f, 32,
                EscPosCharsetEncoding("windows-1252", 16)
            )
            Log.v("Printer", "Printer $printer")
            onConnectionChange(printer != null)

            // Your code that may throw EscPosConnectionException, EscPosParserException, EscPosEncodingException, or InterruptedException
        } catch (e: EscPosConnectionException) {
            e.printStackTrace()
            onConnectionChange(false)
        } catch (e: InterruptedException) {
            onConnectionChange(false)
            e.printStackTrace()
        } catch (e: Exception) {
            onConnectionChange(false)
            e.printStackTrace()
        }
    }

    fun addConnectionListener(onConnectionChange: (status: Boolean) -> Unit = {}) {
        this.onConnectionChange = onConnectionChange
    }

    fun printText(textsToPrint: String) {
        if (printer == null) {
            Log.v("Printer", "Printer is null")
        } else {
            try {
                printer!!.printFormattedTextAndCut(textsToPrint)
                Thread.sleep(500)
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