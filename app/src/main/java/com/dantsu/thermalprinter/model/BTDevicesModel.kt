package com.dantsu.thermalprinter.model

import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection

class BTDevicesModel(
    var bluetoothConnection: BluetoothConnection? = null,
    var connectionStatus: Boolean = false
)