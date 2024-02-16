package com.dantsu.thermalprinter

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections
import com.dantsu.escposprinter.connection.tcp.TcpConnection
import com.dantsu.escposprinter.connection.usb.UsbPrintersConnections
import com.dantsu.thermalprinter.adapter.PrinterDeviceAdapter
import com.dantsu.thermalprinter.databinding.ActivityMainBinding
import com.dantsu.thermalprinter.databinding.DialogAddBinding
import com.dantsu.thermalprinter.manager.BTReceiver
import com.dantsu.thermalprinter.manager.PrinterManager
import com.dantsu.thermalprinter.manager.USBReceiver
import com.dantsu.thermalprinter.model.PrinterDevicesModel
import com.dantsu.thermalprinter.model.PrinterType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    companion object {
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var printerDeviceAdapter: PrinterDeviceAdapter
    private var printerDeviceList: ArrayList<PrinterDevicesModel> = arrayListOf()
    private lateinit var appPreference: AppPreference

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


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        appPreference = AppPreference(this)

        initViews()
        checkBluetoothPermissions()
        initUsb()
        initLan()
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

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun initViews() {
        // bluetooth related work
        val printerManager = PrinterManager()

        printerDeviceAdapter = PrinterDeviceAdapter(this)
        binding.rvDeviceList.adapter = printerDeviceAdapter
        printerDeviceAdapter.setList(printerDeviceList)

        printerDeviceAdapter.setClickListener(
            onRequestClick = { model, index ->
                model.usbConnection?.let { it1 -> printerManager.askUsbPermission(this, it1) }
            },
            onConnectClick = { model, index ->

                model.loading = true
                printerDeviceAdapter.notifyItemChanged(index)

                val connection = when (model.printerType) {
                    PrinterType.BLUETOOTH -> model.bluetoothConnection
                    PrinterType.USB -> model.usbConnection
                    PrinterType.LAN -> model.tcpConnection
                }
                printerManager.connectPrinter(connection!!) { printer, status ->
                    lifecycleScope.launch(Dispatchers.Main) {

                        if (status == PrinterManager.CONNECTION_SUCCESS) {
                            model.printer = printer
                            model.connectionStatus = printer != null
                        } else {
                            showToast("Error in connection")
                        }
                        model.loading = false
                        printerDeviceAdapter.notifyItemChanged(index)

                    }
                }
            },
            onPrintClick = { model, index ->
                model.loading = true
                printerDeviceAdapter.notifyItemChanged(index)
                printerManager.printText(this, model.printer!!) { status ->
                    lifecycleScope.launch(Dispatchers.Main) {
                        if (status != PrinterManager.PRINT_SUCCESS) {
                            showToast("Error in connection")
                        }
                        model.loading = false
                        printerDeviceAdapter.notifyItemChanged(index)
                    }
                }
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

    private fun initLan() {
        val list = appPreference.getIpList().map { ip ->
            PrinterDevicesModel(
                printerType = PrinterType.LAN,
                ip,
                tcpConnection = TcpConnection(
                    ip,
                    9100
                ),
                address = ip
            )
        }

        printerDeviceList.addAll(list)
        printerDeviceAdapter.notifyDataSetChanged()
    }

    private fun addLanDevice(ip: String) {
        val model = PrinterDevicesModel(
            printerType = PrinterType.LAN,
            ip,
            tcpConnection = TcpConnection(
                ip,
                9100
            ),
            address = ip
        )
        printerDeviceList.add(model)
        printerDeviceAdapter.notifyDataSetChanged()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val menuView = menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val dialog = AlertDialog.Builder(this).create()
        val dialogBinding = DialogAddBinding.inflate(layoutInflater)
        dialog.setView(dialogBinding.root)

        dialogBinding.btAdd.setOnClickListener {
            val ipRegex = """^([0-9]{1,3}\.){3}[0-9]{1,3}$""".toRegex()
            val ip = dialogBinding.etAddress.text.toString()

            if (printerDeviceList.any { it.address == ip }) {
                Toast.makeText(this, "Address already exist", Toast.LENGTH_LONG).show()
            } else if (ip.matches(ipRegex) && ip.split('.').none { it.toInt() > 255 }) {
                addLanDevice(ip)
                appPreference.saveIp(ip)
                dialog.dismiss()
            } else {
                Toast.makeText(this, "Invalid address", Toast.LENGTH_LONG).show()
            }
        }

        dialog.show()

        return true
    }
}
