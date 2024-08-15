package com.example.config44

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*

class MainActivity : AppCompatActivity() {

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothSocket: BluetoothSocket? = null
    private lateinit var outputStream: OutputStream
    private lateinit var inputStream: InputStream
    private lateinit var editTextSSID: EditText
    private lateinit var editTextPassword: EditText
    private lateinit var editTextIP: EditText
    private lateinit var esp32Device: BluetoothDevice
    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkBluetoothPermission()

        val connectButton = findViewById<Button>(R.id.connect_button)
        val sendButton = findViewById<Button>(R.id.send_button)
        editTextSSID = findViewById(R.id.edit_text_ssid)
        editTextPassword = findViewById(R.id.edit_text_password)
        editTextIP = findViewById(R.id.edit_text_ip)

        connectButton.setOnClickListener { connectBluetooth() }
        sendButton.setOnClickListener { sendData() }
    }

    private fun checkBluetoothPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH), BLUETOOTH_PERMISSION_REQUEST)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == BLUETOOTH_PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                connectBluetooth()
            }
        }
    }

    private fun connectBluetooth() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH), BLUETOOTH_PERMISSION_REQUEST)
            return
        }

        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
        pairedDevices?.forEach { device ->
            if (device.name == "ESP32") { // Ubah nama perangkat sesuai nama ESP32 Anda
                esp32Device = device
                connectToESP32()
                return@forEach
            }
        }

        Toast.makeText(this, "ESP32 device not found", Toast.LENGTH_SHORT).show()
    }

    private fun connectToESP32() {
        Thread {
            try {
                bluetoothSocket = esp32Device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"))
                bluetoothSocket?.connect()
                outputStream = bluetoothSocket!!.outputStream
                inputStream = bluetoothSocket!!.inputStream // Initialize inputStream
                Log.d(TAG, "Terhubung dengan ESP32")
            } catch (e: IOException) {
                Log.e(TAG, "Error connecting to ESP32: ${e.message}")
                // Tangani kesalahan di sini
            }
        }.start()
    }

    private fun sendData() {
        if (!::outputStream.isInitialized) {
            Toast.makeText(this, "Socket connection is not established yet", Toast.LENGTH_SHORT).show()
            return
        }

        val ssid = editTextSSID.text.toString()
        val password = editTextPassword.text.toString()
        val data = "SSID:$ssid;Password:$password;"

        try {
            outputStream.write(data.toByteArray())
            Toast.makeText(this, "Data sent: $data", Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to send data", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            bluetoothSocket?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    companion object {
        private const val BLUETOOTH_PERMISSION_REQUEST = 1001
    }
}
