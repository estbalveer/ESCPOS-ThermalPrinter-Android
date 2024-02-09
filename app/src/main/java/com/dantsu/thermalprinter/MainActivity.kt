package com.dantsu.thermalprinter

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.PendingIntent
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.dantsu.escposprinter.connection.DeviceConnection
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections
import com.dantsu.escposprinter.connection.tcp.TcpConnection
import com.dantsu.escposprinter.connection.usb.UsbConnection
import com.dantsu.escposprinter.connection.usb.UsbPrintersConnections
import com.dantsu.escposprinter.textparser.PrinterTextParserImg
import com.dantsu.thermalprinter.adapter.BTPairedAdapter
import com.dantsu.thermalprinter.adapter.UsbAdapter
import com.dantsu.thermalprinter.async.AsyncEscPosPrint.OnPrintFinished
import com.dantsu.thermalprinter.async.AsyncEscPosPrinter
import com.dantsu.thermalprinter.async.AsyncTcpEscPosPrint
import com.dantsu.thermalprinter.async.AsyncUsbEscPosPrint
import com.dantsu.thermalprinter.databinding.ActivityMainBinding
import com.dantsu.thermalprinter.model.BTDevicesModel
import com.dantsu.thermalprinter.model.USBDevicesModel
import java.text.SimpleDateFormat
import java.util.Date

class MainActivity : AppCompatActivity() {
    companion object {
        private const val ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION"
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var bTPairedAdapter: BTPairedAdapter
    private var btDeviceList: ArrayList<BTDevicesModel> = arrayListOf()

    private lateinit var usbAdapter: UsbAdapter
    private var usbDeviceList: ArrayList<USBDevicesModel> = arrayListOf()

    private val permissions = when {
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S -> arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN
        )

        else -> arrayOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN
        )
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions.all { it.value }) {
                onBluetoothPermissionsGranted()
            } else {
                // Handle the case where not all permissions were granted
            }
        }


    private val printText = """
                [L]
                [C]<u><font size='big'>ORDER N°045</font></u>
                """

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.buttonTcp.setOnClickListener { view: View? -> printTcp() }

        initViews()
        checkBluetoothPermissions()
        initUsb()
    }

    /*==============================================================================================
    ======================================BLUETOOTH PART============================================
    ==============================================================================================*/

    private fun checkBluetoothPermissions() {
        if (permissions.any {
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }) {
            requestPermissionLauncher.launch(permissions)
        } else {
            onBluetoothPermissionsGranted()
        }
    }

    private fun onBluetoothPermissionsGranted() {
        initBluetooth()
    }

    private fun initViews() {
        // bluetooth related work
        val bTPrinterManager = BTPrinterManager()
        bTPairedAdapter = BTPairedAdapter()
        binding.rvBluetoothList.adapter = bTPairedAdapter

        bTPairedAdapter.setClickListener(
            onConnectClick = { model ->
                bTPrinterManager.connectPrinter(model.bluetoothConnection!!) {
                    model.printer = it
                    model.connectionStatus = true
                    bTPairedAdapter.notifyDataSetChanged()
                }
            },
            onPrintClick = {
                bTPrinterManager.printText(it.printer!!, printText.trimIndent())

            }
        )

        // usb related work
//        val usbAdapter = BTPrinterManager()
        usbAdapter = UsbAdapter()
        binding.rvUSBList.adapter = usbAdapter

        usbAdapter.setClickListener(
            onRequestClick = {
                askUsbPermission()
            },
            onConnectClick = { model ->
                connectUsbPrinter(model.usbConnection!!.device)
                // get callback and change status
            },
            onPrintClick = {
//                bTPrinterManager.printText(it.printer!!, printText.trimIndent())

            }
        )
    }

    private fun initBluetooth() {
        val bluetoothDevicesList = BluetoothPrintersConnections().list ?: emptyArray()
        btDeviceList = ArrayList(bluetoothDevicesList.map { BTDevicesModel(it, false) })
        bTPairedAdapter.setList(btDeviceList)

        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        }
        val bluetoothReceiver = BTReceiver { bluetoothDevice ->
            btDeviceList.find { it.bluetoothConnection?.device?.address == bluetoothDevice.address }
                ?.let {
                    it.printer?.disconnectPrinter()
                    it.printer = null
                    it.connectionStatus = false
                    bTPairedAdapter.notifyDataSetChanged()
                }
        }
        registerReceiver(bluetoothReceiver, filter)
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }


    /*==============================================================================================
    ===========================================USB PART=============================================
    ==============================================================================================*/

    private fun initUsb() {
        val usbConnection = UsbPrintersConnections(this).list
        val usbManager = this.getSystemService(USB_SERVICE) as UsbManager
        if (usbConnection != null) {
            usbDeviceList = ArrayList(usbConnection.map {
                USBDevicesModel(it, false, hasPermission = usbManager.hasPermission(it.device))
            }
            )
            usbAdapter.setList(usbDeviceList)
        }
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
                            usbDeviceList.find { it.usbConnection?.device?.deviceId == usbDevice.deviceId }
                                ?.let {
                                    it.hasPermission = true
                                    usbAdapter.notifyDataSetChanged()
                                }

//                            connectUsbPrinter(usbDevice)
                        }
                    }
                }
            }
        }
    }

    private fun connectUsbPrinter(usbDevice: UsbDevice) {
        val usbManager = getSystemService(USB_SERVICE) as UsbManager
        AsyncUsbEscPosPrint(
            this,
            object : OnPrintFinished() {
                override fun onError(
                    asyncEscPosPrinter: AsyncEscPosPrinter,
                    codeException: Int
                ) {
                    Log.e(
                        "Async.OnPrintFinished",
                        "AsyncEscPosPrint.OnPrintFinished : An error occurred !"
                    )
                }

                override fun onSuccess(asyncEscPosPrinter: AsyncEscPosPrinter) {
                    Log.i(
                        "Async.OnPrintFinished",
                        "AsyncEscPosPrint.OnPrintFinished : Print is finished !"
                    )
                }
            }
        )
            .execute(
                getAsyncEscPosPrinter(
                    UsbConnection(
                        usbManager,
                        usbDevice
                    )
                )
            )
    }

    private fun askUsbPermission() {
        val usbConnection = UsbPrintersConnections.selectFirstConnected(this)
        val usbManager = this.getSystemService(USB_SERVICE) as UsbManager
        if (usbConnection == null) {
            AlertDialog.Builder(this)
                .setTitle("USB Connection")
                .setMessage("No USB printer found.")
                .show()
            return
        }
        val permissionIntent = PendingIntent.getBroadcast(
            this,
            0,
            Intent(ACTION_USB_PERMISSION),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
        )
        val filter = IntentFilter(ACTION_USB_PERMISSION)
        registerReceiver(usbReceiver, filter)
        usbManager.requestPermission(usbConnection.device, permissionIntent)
    }

    /*==============================================================================================
    =========================================TCP PART===============================================
    ==============================================================================================*/
    fun printTcp() {
        try {
            AsyncTcpEscPosPrint(
                this,
                object : OnPrintFinished() {
                    override fun onError(
                        asyncEscPosPrinter: AsyncEscPosPrinter,
                        codeException: Int
                    ) {
                        Log.e(
                            "Async.OnPrintFinished",
                            "AsyncEscPosPrint.OnPrintFinished : An error occurred !"
                        )
                    }

                    override fun onSuccess(asyncEscPosPrinter: AsyncEscPosPrinter) {
                        Log.i(
                            "Async.OnPrintFinished",
                            "AsyncEscPosPrint.OnPrintFinished : Print is finished !"
                        )
                    }
                }
            )
                .execute(
                    getAsyncEscPosPrinter(
                        TcpConnection(
                            binding!!.edittextTcpIp.text.toString(),
                            binding!!.edittextTcpPort.text.toString().toInt()
                        )
                    )
                )
        } catch (e: NumberFormatException) {
            AlertDialog.Builder(this)
                .setTitle("Invalid TCP port address")
                .setMessage("Port field must be an integer.")
                .show()
            e.printStackTrace()
        }
    }
    /*==============================================================================================
    ===================================ESC/POS PRINTER PART=========================================
    ==============================================================================================*/
    /**
     * Asynchronous printing
     */
    @SuppressLint("SimpleDateFormat")
    fun getAsyncEscPosPrinter(printerConnection: DeviceConnection?): AsyncEscPosPrinter {
        val format = SimpleDateFormat("'on' yyyy-MM-dd 'at' HH:mm:ss")
        val printer = AsyncEscPosPrinter(printerConnection, 203, 48f, 32)
        return printer.addTextToPrint(
            """
                [C]<img>${
                PrinterTextParserImg.bitmapToHexadecimalString(
                    printer,
                    this.applicationContext.resources.getDrawableForDensity(
                        R.drawable.logo,
                        DisplayMetrics.DENSITY_MEDIUM
                    )
                )
            }</img>
                [L]
                [C]<u><font size='big'>ORDER N°045</font></u>
                [L]
                [C]<u type='double'>${format.format(Date())}</u>
                [C]
                [C]================================
                [L]
                [L]<b>BEAUTIFUL SHIRT</b>[R]9.99€
                [L]  + Size : S
                [L]
                [L]<b>AWESOME HAT</b>[R]24.99€
                [L]  + Size : 57/58
                [L]
                [C]--------------------------------
                [R]TOTAL PRICE :[R]34.98€
                [R]TAX :[R]4.23€
                [L]
                [C]================================
                [L]
                [L]<u><font color='bg-black' size='tall'>Customer :</font></u>
                [L]Raymond DUPONT
                [L]5 rue des girafes
                [L]31547 PERPETES
                [L]Tel : +33801201456
                
                [C]<barcode type='ean13' height='10'>831254784551</barcode>
                [L]
                [C]<qrcode size='20'>https://dantsu.com/</qrcode>
                
                """.trimIndent()
        )
    }
}
