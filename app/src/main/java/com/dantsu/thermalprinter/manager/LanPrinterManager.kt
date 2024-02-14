package com.dantsu.thermalprinter.manager

import android.util.Log
import com.dantsu.escposprinter.EscPosCharsetEncoding
import com.dantsu.escposprinter.EscPosPrinter
import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection
import com.dantsu.escposprinter.connection.tcp.TcpConnection
import com.dantsu.escposprinter.exceptions.EscPosBarcodeException
import com.dantsu.escposprinter.exceptions.EscPosConnectionException
import com.dantsu.escposprinter.exceptions.EscPosEncodingException
import com.dantsu.escposprinter.exceptions.EscPosParserException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LanPrinterManager {

    fun connectPrinter(
        deviceConnection: TcpConnection,
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