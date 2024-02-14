package com.dantsu.thermalprinter

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.bluetooth.BluetoothDevice
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.dantsu.escposprinter.connection.DeviceConnection
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections
import com.dantsu.escposprinter.connection.tcp.TcpConnection
import com.dantsu.escposprinter.connection.usb.UsbPrintersConnections
import com.dantsu.escposprinter.textparser.PrinterTextParserImg
import com.dantsu.thermalprinter.adapter.PrinterDeviceAdapter
import com.dantsu.thermalprinter.async.AsyncEscPosPrint.OnPrintFinished
import com.dantsu.thermalprinter.async.AsyncEscPosPrinter
import com.dantsu.thermalprinter.async.AsyncTcpEscPosPrint
import com.dantsu.thermalprinter.databinding.ActivityMainBinding
import com.dantsu.thermalprinter.manager.BTReceiver
import com.dantsu.thermalprinter.manager.PrinterManager
import com.dantsu.thermalprinter.manager.USBReceiver
import com.dantsu.thermalprinter.model.PrinterDevicesModel
import com.dantsu.thermalprinter.model.PrinterType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date

class MainActivity : AppCompatActivity() {
    companion object {
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var printerDeviceAdapter: PrinterDeviceAdapter
    private var printerDeviceList: ArrayList<PrinterDevicesModel> = arrayListOf()

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
                [C]<u><font size='big'>ORDER N°045</font></u>
                [C]<u><font size='big'>ORDER N°045</font></u>
                [C]<u><font size='big'>ORDER N°045</font></u>
                [C]<u><font size='big'>ORDER N°045</font></u>
                """

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btAdd.setOnClickListener { view: View? -> addLanDevice() }

        initViews()
        checkBluetoothPermissions()
        initUsb()
    }

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
        val printerManager = PrinterManager()

        printerDeviceAdapter = PrinterDeviceAdapter(this)
        binding.rvDeviceList.adapter = printerDeviceAdapter
        printerDeviceAdapter.setList(printerDeviceList)

        printerDeviceAdapter.setClickListener(
            onRequestClick = {
                it.usbConnection?.let { it1 -> printerManager.askUsbPermission(this, it1) }
            },
            onConnectClick = { model ->
                val connection = when (model.printerType) {
                    PrinterType.BLUETOOTH -> model.bluetoothConnection
                    PrinterType.USB -> model.usbConnection
                    PrinterType.LAN -> model.tcpConnection
                }
                printerManager.connectPrinter(connection!!) {
                    lifecycleScope.launch(Dispatchers.Main) {
                        model.printer = it
                        model.connectionStatus = it != null
                        printerDeviceAdapter.notifyDataSetChanged()
                    }

                }

//                lanPrinterManager.connectPrinter(it1) {
//                    lifecycleScope.launch(Dispatchers.Main) {
//                        model.printer = it
//                        model.connectionStatus = it != null
//                        lanAdapter.notifyDataSetChanged()
//                    }
//                }
            },
            onPrintClick = {
                printerManager.printText(it.printer!!, printText.trimIndent())

            }
        )

        // usb related wor
        printerManager.setOnOnPermissionGranted { usbDevice ->
            printerDeviceList.find { it.usbConnection?.device?.deviceId == usbDevice?.deviceId }
                ?.let {
                    it.hasPermission = true
                    printerDeviceAdapter.notifyDataSetChanged()
                }
        }
    }

    @SuppressLint("MissingPermission")
    private fun initBluetooth() {
        val bluetoothDevicesList = BluetoothPrintersConnections().list ?: emptyArray()
        val btDeviceList = ArrayList(bluetoothDevicesList.map {
            PrinterDevicesModel(
                PrinterType.BLUETOOTH,
                it.device.name,
                bluetoothConnection = it,
                connectionStatus = false
            )
        })
        printerDeviceList.addAll(btDeviceList)
        printerDeviceAdapter.notifyDataSetChanged()

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
                    printerDeviceAdapter.notifyDataSetChanged()
                }
        }
        registerReceiver(bluetoothReceiver, filter)
    }

    private fun setUsbAdapter() {
        val usbConnection = UsbPrintersConnections(this).list
        val usbManager = this.getSystemService(USB_SERVICE) as UsbManager
        if (usbConnection != null) {
            val usbDeviceList = ArrayList(usbConnection.map {
                PrinterDevicesModel(
                    printerType = PrinterType.USB,
                    it.device.deviceName,
                    usbConnection = it,
                    connectionStatus = false,
                    hasPermission = usbManager.hasPermission(it.device)
                )
            }
            )
            printerDeviceList.addAll(usbDeviceList)
            printerDeviceAdapter.notifyDataSetChanged()
        }
    }

    private fun initUsb() {
        setUsbAdapter()

        val filter = IntentFilter()
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)

        val usbReceiver = USBReceiver {
            setUsbAdapter()
        }
        registerReceiver(usbReceiver, filter)
    }

    private fun addLanDevice() {
        val model = PrinterDevicesModel(
            printerType = PrinterType.LAN,
            binding.edittextTcpIp.text.toString(),
            tcpConnection = TcpConnection(
                binding.edittextTcpIp.text.toString(),
                binding.edittextTcpPort.text.toString().toInt()
            ),
            address = binding.edittextTcpIp.text.toString()
        )
        printerDeviceList.add(model)
        printerDeviceAdapter.notifyDataSetChanged()
    }
}
