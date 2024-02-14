package com.dantsu.thermalprinter.model

import com.dantsu.escposprinter.EscPosPrinter
import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection
import com.dantsu.escposprinter.connection.tcp.TcpConnection
import com.dantsu.escposprinter.connection.usb.UsbConnection

enum class PrinterType {
    BLUETOOTH,
    USB,
    LAN
}

class PrinterDevicesModel(
    var printerType: PrinterType,
    var printerName: String,
    var connectionStatus: Boolean = false,
    var printer: EscPosPrinter? = null,

    var bluetoothConnection: BluetoothConnection? = null,

    var usbConnection: UsbConnection? = null,
    var hasPermission: Boolean = false,

    var tcpConnection: TcpConnection? = null,
    var address: String = "",
)