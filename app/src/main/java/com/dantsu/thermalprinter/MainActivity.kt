package com.dantsu.thermalprinter

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.DialogInterface
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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.dantsu.escposprinter.connection.DeviceConnection
import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections
import com.dantsu.escposprinter.connection.tcp.TcpConnection
import com.dantsu.escposprinter.connection.usb.UsbConnection
import com.dantsu.escposprinter.connection.usb.UsbPrintersConnections
import com.dantsu.escposprinter.textparser.PrinterTextParserImg
import com.dantsu.thermalprinter.adapter.BTPairedAdapter
import com.dantsu.thermalprinter.async.AsyncBluetoothEscPosPrint
import com.dantsu.thermalprinter.async.AsyncEscPosPrint.OnPrintFinished
import com.dantsu.thermalprinter.async.AsyncEscPosPrinter
import com.dantsu.thermalprinter.async.AsyncTcpEscPosPrint
import com.dantsu.thermalprinter.async.AsyncUsbEscPosPrint
import com.dantsu.thermalprinter.databinding.ActivityMainBinding
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Collections
import java.util.Date

class MainActivity : AppCompatActivity() {
    private var binding: ActivityMainBinding? = null
    private var selectedDevice: BluetoothConnection? = null

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
        setContentView(binding!!.root)
        binding!!.buttonUsb.setOnClickListener { view: View? -> printUsb() }
        binding!!.buttonTcp.setOnClickListener { view: View? -> printTcp() }

        checkBluetoothPermissions()
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

    private fun initBluetooth() {
        val bluetoothDevicesList = BluetoothPrintersConnections().list
        val bTPrinterManager = BTPrinterManager()
        if (bluetoothDevicesList != null) {
            val adapter = BTPairedAdapter({
//                printBluetooth(it)
                bTPrinterManager.connectPrinter(it)
            }, {
                bTPrinterManager.printText(
                    printText.trimIndent()
                )
            })

            adapter.setList(ArrayList(bluetoothDevicesList.asList()))
            binding!!.rvBluetoothList.adapter = adapter
        }
    }


    private fun printBluetooth(device: BluetoothConnection) {
        AsyncBluetoothEscPosPrint(
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
            .execute(getAsyncEscPosPrinter(device))
    }

    private val usbReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (ACTION_USB_PERMISSION == action) {
                synchronized(this) {
                    val usbManager = getSystemService(USB_SERVICE) as UsbManager
                    val usbDevice =
                        intent.getParcelableExtra<Parcelable>(UsbManager.EXTRA_DEVICE) as UsbDevice?
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (usbManager != null && usbDevice != null) {
                            AsyncUsbEscPosPrint(
                                context,
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
                    }
                }
            }
        }
    }

    fun printUsb() {
        val usbConnection = UsbPrintersConnections.selectFirstConnected(this)
        val usbManager = this.getSystemService(USB_SERVICE) as UsbManager
        if (usbConnection == null || usbManager == null) {
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

    companion object {
        const val PERMISSION_BLUETOOTH = 1
        const val PERMISSION_BLUETOOTH_ADMIN = 2
        const val PERMISSION_BLUETOOTH_CONNECT = 3
        const val PERMISSION_BLUETOOTH_SCAN = 4

        /*==============================================================================================
    ===========================================USB PART=============================================
    ==============================================================================================*/
        private const val ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION"
    }
}
