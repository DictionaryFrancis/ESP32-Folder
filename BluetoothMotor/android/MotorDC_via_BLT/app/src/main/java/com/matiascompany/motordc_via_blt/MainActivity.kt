package com.matiascompany.motordc_via_blt

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.*
import android.view.Gravity
import android.view.WindowInsets
import android.widget.*
import androidx.annotation.RequiresApi
import java.util.UUID

// Android 12+ (minSdk 31). Uses the Android SDK only; no Compose/XML layout needed.
@SuppressLint("MissingPermission")
class MainActivity : Activity() {
    private val serviceId = UUID.fromString("d8f10001-6a73-4c31-9145-24e3c84f2100")
    private val commandId = UUID.fromString("d8f10002-6a73-4c31-9145-24e3c84f2100")
    private val main = Handler(Looper.getMainLooper())
    private val adapter get() = getSystemService(BluetoothManager::class.java)?.adapter
    private var scanner: BluetoothLeScanner? = null
    private var scanning = false
    private var gatt: BluetoothGatt? = null
    private var command: BluetoothGattCharacteristic? = null
    private var pending: String? = null
    private var writeSequence = 0
    private lateinit var status: TextView
    private lateinit var connect: Button
    private lateinit var on: Button
    private lateinit var off: Button

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(26), dp(40), dp(26), dp(24))
            setBackgroundColor(Color.rgb(16, 24, 39))
        }
        // Keep content clear of status/navigation bars on edge-to-edge Android versions.
        panel.setOnApplyWindowInsetsListener { view, insets ->
            val bars = insets.getInsets(WindowInsets.Type.systemBars())
            view.setPadding(dp(26) + bars.left, dp(32) + bars.top,
                dp(26) + bars.right, dp(24) + bars.bottom)
            insets
        }
        fun label(text: String, size: Float, color: Int) = TextView(this).apply {
            this.text = text; textSize = size; setTextColor(color)
            gravity = Gravity.CENTER
            setPadding(0, dp(12), 0, dp(12))
        }
        panel.addView(label("ESP32 MOTOR", 30f, Color.WHITE).apply {
            setTypeface(null, Typeface.BOLD)
        })
        panel.addView(label("Bluetooth Low Energy controller", 16f, Color.LTGRAY))
        status = label("Disconnected", 20f, Color.rgb(147, 197, 253))
        panel.addView(status)
        fun button(title: String, color: Int, action: () -> Unit): Button {
            return Button(this).apply {
                text = title; textSize = 18f; isAllCaps = false
                setTextColor(Color.WHITE)
                background = GradientDrawable().apply {
                    setColor(color); cornerRadius = dp(16).toFloat()
                }
                layoutParams = LinearLayout.LayoutParams(-1, dp(68)).apply {
                    topMargin = dp(16)
                }
                setOnClickListener { action() }
                panel.addView(this)
            }
        }
        connect = button("Connect to ESP32", Color.rgb(37, 99, 235)) { begin() }
        on = button("Motor ON", Color.rgb(21, 128, 61)) { send("ON") }
        off = button("Motor OFF", Color.rgb(185, 28, 28)) { send("OFF") }
        button("Disconnect", Color.rgb(71, 85, 105)) { close("Disconnected") }
        panel.addView(label("Leaving this screen disconnects the motor.\nKeep the physical power switch accessible.",
            14f, Color.LTGRAY))
        setContentView(ScrollView(this).apply { addView(panel) })
        controls(false)
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun controls(ready: Boolean) {
        on.isEnabled = ready && pending == null
        off.isEnabled = ready && pending == null
        on.alpha = if (on.isEnabled) 1f else 0.35f
        off.alpha = if (off.isEnabled) 1f else 0.35f
    }

    private fun begin() {
        val permissions = arrayOf(Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT)
        if (permissions.any { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }) {
            requestPermissions(permissions, 10)
            return
        }
        val bluetooth = adapter
        if (bluetooth == null) { status.text = "This phone has no Bluetooth adapter"; return }
        if (!bluetooth.isEnabled) {
            startActivity(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            status.text = "Enable Bluetooth, then tap Connect"
            return
        }
        close("Scanning for ESP32-Motor...")
        scanner = bluetooth.bluetoothLeScanner
        if (scanner == null) { status.text = "Bluetooth scanner unavailable"; return }
        connect.isEnabled = false
        scanning = true
        scanner?.startScan(listOf(ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(serviceId)).build()),
            ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(), scan)
        main.postDelayed({
            if (scanning) close("ESP32 not found. Check the BLE sketch and USB power.")
        }, 12000)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>,
                                            grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 10) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) begin()
            else status.text = "Nearby devices permission is required. Allow it in app settings."
        }
    }

    private val scan = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            main.post {
                if (!scanning) return@post
                stopScan()
                status.text = "Connecting to ESP32-Motor..."
                gatt = result.device.connectGatt(this@MainActivity, false, callback,
                    BluetoothDevice.TRANSPORT_LE)
                val attempt = gatt
                main.postDelayed({
                    if (gatt === attempt && command == null) close("Connection timed out. Try again.")
                }, 15000)
            }
        }
        override fun onScanFailed(errorCode: Int) {
            main.post { if (scanning) close("Scan failed ($errorCode). Try again.") }
        }
    }

    private val callback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(device: BluetoothGatt, result: Int, state: Int) {
            main.post {
                if (device !== gatt) return@post
                if (result != BluetoothGatt.GATT_SUCCESS || state == BluetoothProfile.STATE_DISCONNECTED) {
                    close("Disconnected ($result)")
                } else if (state == BluetoothProfile.STATE_CONNECTED) {
                    status.text = "Finding motor controls..."
                    if (!device.discoverServices()) close("Could not discover services")
                }
            }
        }
        override fun onServicesDiscovered(device: BluetoothGatt, result: Int) {
            main.post {
                if (device !== gatt) return@post
                val found = device.getService(serviceId)?.getCharacteristic(commandId)
                if (result != BluetoothGatt.GATT_SUCCESS || found == null ||
                    (found.properties and BluetoothGattCharacteristic.PROPERTY_WRITE) == 0) {
                    close("Motor service missing. Upload the matching ESP32 BLE sketch.")
                    return@post
                }
                command = found
                send("OFF") // Establish OFF before enabling the UI.
            }
        }
        override fun onCharacteristicWrite(device: BluetoothGatt,
                                           characteristic: BluetoothGattCharacteristic, result: Int) {
            main.post {
                if (device !== gatt || characteristic.uuid != commandId) return@post
                val sent = pending ?: return@post
                pending = null
                if (result != BluetoothGatt.GATT_SUCCESS) close("Command failed. Disconnected.")
                else {
                    status.text = "$sent command delivered"
                    controls(true)
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun send(value: String) {
        val device = gatt ?: return
        val target = command ?: return
        if (pending != null) return
        pending = value
        val sequence = ++writeSequence
        controls(false)
        status.text = "Sending $value..."
        val bytes = value.toByteArray(Charsets.UTF_8)
        val started = if (Build.VERSION.SDK_INT >= 33) {
            device.writeCharacteristic(target, bytes,
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT) == BluetoothStatusCodes.SUCCESS
        } else {
            target.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            target.value = bytes
            device.writeCharacteristic(target)
        }
        if (!started) { close("Could not send command. Disconnected."); return }
        main.postDelayed({
            if (gatt === device && pending != null && writeSequence == sequence)
                close("Command timed out. Disconnected.")
        }, 5000)
    }

    private fun stopScan() {
        if (scanning) scanner?.stopScan(scan)
        scanning = false
    }
    private fun close(message: String) {
        stopScan()
        main.removeCallbacksAndMessages(null)
        command = null
        pending = null
        val previous = gatt
        gatt = null
        previous?.disconnect()
        previous?.close()
        controls(false)
        connect.isEnabled = true
        status.text = message
    }
    override fun onStop() {
        super.onStop()
        close("Disconnected. Tap Connect to reconnect.")
    }
}
