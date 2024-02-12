package com.dantsu.thermalprinter.model

import com.dantsu.escposprinter.EscPosPrinter
import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection

class LANDevicesModel(
    var bluetoothConnection: BluetoothConnection? = null,
    var connectionStatus: Boolean = false,
    var printer: EscPosPrinter? = null
)