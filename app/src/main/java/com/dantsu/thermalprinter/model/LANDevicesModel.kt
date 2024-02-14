package com.dantsu.thermalprinter.model

import com.dantsu.escposprinter.EscPosPrinter
import com.dantsu.escposprinter.connection.tcp.TcpConnection

class LANDevicesModel(
    var tcpConnection: TcpConnection? = null,
    var connectionStatus: Boolean = false,
    var printer: EscPosPrinter? = null,
    var address: String = ""
)