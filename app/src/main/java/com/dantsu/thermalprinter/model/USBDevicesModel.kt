package com.dantsu.thermalprinter.model

import com.dantsu.escposprinter.EscPosPrinter
import com.dantsu.escposprinter.connection.usb.UsbConnection

class USBDevicesModel(
    var usbConnection: UsbConnection? = null,
    var connectionStatus: Boolean = false,
    var printer: EscPosPrinter? = null,
    var hasPermission: Boolean = false
)