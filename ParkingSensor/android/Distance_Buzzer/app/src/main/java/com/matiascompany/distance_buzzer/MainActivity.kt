package com.matiascompany.distance_buzzer

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.os.*
import android.view.*
import android.widget.*
import androidx.annotation.RequiresApi
import java.util.UUID
import java.util.Locale

@SuppressLint("MissingPermission")
class MainActivity : Activity() {
    private val serviceId = UUID.fromString("4fa10001-6a73-4c31-9145-24e3c84f2100")
    private val distanceId = UUID.fromString("4fa10002-6a73-4c31-9145-24e3c84f2100")
    private val cccdId = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    private val main = Handler(Looper.getMainLooper())
    private val adapter get() = getSystemService(BluetoothManager::class.java)?.adapter
    private var scanner: BluetoothLeScanner? = null
    private var scanning = false
    private var gatt: BluetoothGatt? = null
    private var subscribed = false
    private var lastPacket = 0L
    private lateinit var status: TextView
    private lateinit var distance: TextView
    private lateinit var proximity: TextView
    private lateinit var connect: Button
    private lateinit var arc: ProximityView

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setBackgroundColor(Color.rgb(15, 23, 42))
            setPadding(dp(24), dp(32), dp(24), dp(24))
        }
        panel.setOnApplyWindowInsetsListener { view, insets ->
            val bars = insets.getInsets(WindowInsets.Type.systemBars())
            view.setPadding(dp(24) + bars.left, dp(24) + bars.top,
                dp(24) + bars.right, dp(24) + bars.bottom)
            insets
        }
        fun label(text: String, size: Float, color: Int) = TextView(this).apply {
            this.text = text; textSize = size; setTextColor(color)
            gravity = Gravity.CENTER; setPadding(0, dp(8), 0, dp(8))
        }
        panel.addView(label("PARKING SENSOR", 28f, Color.WHITE).apply {
            setTypeface(null, Typeface.BOLD)
        })
        panel.addView(label("Live distance · Bluetooth LE", 16f, Color.LTGRAY))
        status = label("Disconnected", 17f, Color.rgb(147, 197, 253)); panel.addView(status)
        arc = ProximityView(this)
        panel.addView(arc, LinearLayout.LayoutParams(-1, dp(170)))
        distance = label("— cm", 56f, Color.WHITE); panel.addView(distance)
        proximity = label("Connect to start", 22f, Color.LTGRAY); panel.addView(proximity)
        fun button(text: String, color: Int, action: () -> Unit): Button {
            return Button(this).apply {
                this.text = text; textSize = 18f; isAllCaps = false
                setTextColor(Color.WHITE)
                background = GradientDrawable().apply { setColor(color); cornerRadius = dp(16).toFloat() }
                layoutParams = LinearLayout.LayoutParams(-1, dp(62)).apply { topMargin = dp(16) }
                setOnClickListener { action() }; panel.addView(this)
            }
        }
        connect = button("Connect to sensor", Color.rgb(37, 99, 235)) { begin() }
        button("Disconnect", Color.rgb(71, 85, 105)) { close("Disconnected") }
        panel.addView(label("Green: over 60 cm · Yellow: 30–60 cm\nOrange: 10–30 cm · Red: under 10 cm", 14f, Color.LTGRAY))
        panel.addView(label("Fixed sensor: proximity indicator, not a map.\nNo reading does not mean the path is clear.", 13f, Color.LTGRAY))
        setContentView(ScrollView(this).apply { addView(panel) })
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun unknown(message: String) {
        distance.text = "— cm"; proximity.text = message
        proximity.setTextColor(Color.LTGRAY); arc.colour = Color.rgb(71, 85, 105)
    }
    private fun begin() {
        val required = arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        if (required.any { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }) {
            requestPermissions(required, 10); return
        }
        val bluetooth = adapter
        if (bluetooth == null) { status.text = "BLE adapter unavailable"; return }
        if (!bluetooth.isEnabled) {
            startActivity(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            status.text = "Enable Bluetooth, then tap Connect"; return
        }
        close("Scanning for ESP32-Parking...")
        scanner = bluetooth.bluetoothLeScanner
        if (scanner == null) { status.text = "Scanner unavailable"; return }
        scanning = true; connect.isEnabled = false
        scanner?.startScan(listOf(ScanFilter.Builder().setServiceUuid(ParcelUuid(serviceId)).build()),
            ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(), scan)
        main.postDelayed({ if (scanning) close("Sensor not found. Check the new ESP32 sketch.") }, 12000)
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, results: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, results)
        if (requestCode == 10) {
            if (results.isNotEmpty() && results.all { it == PackageManager.PERMISSION_GRANTED }) begin()
            else status.text = "Allow Nearby devices in app settings to connect."
        }
    }
    private val scan = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) { main.post {
            if (!scanning) return@post
            stopScan(); status.text = "Connecting..."
            gatt = result.device.connectGatt(this@MainActivity, false, callback, BluetoothDevice.TRANSPORT_LE)
            val attempt = gatt
            main.postDelayed({ if (gatt === attempt && !subscribed) close("Connection timed out") }, 15000)
        } }
        override fun onScanFailed(errorCode: Int) { main.post {
            if (scanning) close("Scan failed ($errorCode)")
        } }
    }
    private val callback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(device: BluetoothGatt, result: Int, state: Int) { main.post {
            if (device !== gatt) return@post
            if (result != BluetoothGatt.GATT_SUCCESS || state == BluetoothProfile.STATE_DISCONNECTED)
                close("Disconnected ($result)")
            else if (state == BluetoothProfile.STATE_CONNECTED) {
                status.text = "Finding distance service..."
                if (!device.discoverServices()) close("Service discovery failed")
            }
        } }
        @Suppress("DEPRECATION")
        override fun onServicesDiscovered(device: BluetoothGatt, result: Int) { main.post {
            if (device !== gatt) return@post
            val characteristic = device.getService(serviceId)?.getCharacteristic(distanceId)
            val descriptor = characteristic?.getDescriptor(cccdId)
            if (result != BluetoothGatt.GATT_SUCCESS || characteristic == null || descriptor == null ||
                (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY) == 0) {
                close("Distance service missing. Upload parking_sensor_ble."); return@post
            }
            if (!device.setCharacteristicNotification(characteristic, true)) {
                close("Could not enable updates"); return@post
            }
            val started = if (Build.VERSION.SDK_INT >= 33) {
                device.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE) == BluetoothStatusCodes.SUCCESS
            } else {
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                device.writeDescriptor(descriptor)
            }
            if (!started) close("Could not subscribe to distance updates")
        } }
        override fun onDescriptorWrite(device: BluetoothGatt, descriptor: BluetoothGattDescriptor, result: Int) { main.post {
            if (device !== gatt || descriptor.uuid != cccdId) return@post
            if (result != BluetoothGatt.GATT_SUCCESS) { close("Subscription failed"); return@post }
            subscribed = true; lastPacket = SystemClock.elapsedRealtime()
            status.text = "Connected · waiting for measurements"
            main.post(watchdog)
        } }
        override fun onCharacteristicChanged(device: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
            packet(device, characteristic.uuid, value.copyOf())
        }
        @Deprecated("Legacy callback for Android 12")
        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(device: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            if (Build.VERSION.SDK_INT < 33) packet(device, characteristic.uuid, characteristic.value?.copyOf() ?: return)
        }
    }
    private fun packet(device: BluetoothGatt, uuid: UUID, value: ByteArray) { main.post {
        if (device !== gatt || uuid != distanceId || !subscribed) return@post
        lastPacket = SystemClock.elapsedRealtime()
        status.text = "Connected · live"
        val cm = value.toString(Charsets.UTF_8).trim().toFloatOrNull()
        if (cm == null || !cm.isFinite() || cm < 2f || cm > 400f) {
            unknown("No valid reading"); return@post
        }
        distance.text = String.format(Locale.US, "%.1f cm", cm)
        val (text, colour) = when {
            cm < 10f -> "STOP · very close" to Color.rgb(248, 113, 113)
            cm < 30f -> "Close" to Color.rgb(251, 146, 60)
            cm <= 60f -> "Approaching" to Color.rgb(250, 204, 21)
            else -> "Beyond warning range" to Color.rgb(74, 222, 128)
        }
        proximity.text = text; proximity.setTextColor(colour); arc.colour = colour
    } }
    private val watchdog = object : Runnable {
        override fun run() {
            if (!subscribed) return
            if (SystemClock.elapsedRealtime() - lastPacket > 2000) {
                unknown("Measurements unavailable"); status.text = "Connected · no recent data"
            }
            main.postDelayed(this, 500)
        }
    }
    private fun stopScan() { if (scanning) scanner?.stopScan(scan); scanning = false }
    private fun close(message: String) {
        stopScan(); main.removeCallbacksAndMessages(null); subscribed = false
        val previous = gatt; gatt = null
        previous?.disconnect(); previous?.close()
        connect.isEnabled = true; status.text = message; unknown("No live measurement")
    }
    override fun onStop() { super.onStop(); close("Disconnected · tap Connect to reconnect") }
}

class ProximityView(context: Context) : View(context) {
    var colour: Int = Color.rgb(71, 85, 105)
        set(value) { field = value; invalidate() }
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height * 0.87f
        val radius = minOf(width * 0.43f, height * 0.75f)
        paint.style = Paint.Style.STROKE; paint.strokeCap = Paint.Cap.ROUND
        paint.strokeWidth = resources.displayMetrics.density * 9
        for (fraction in listOf(0.45f, 0.7f, 0.95f)) {
            val r = radius * fraction
            paint.color = colour
            canvas.drawArc(cx-r, cy-r, cx+r, cy+r, 195f, 150f, false, paint)
        }
        paint.style = Paint.Style.FILL; paint.color = Color.WHITE
        canvas.drawRoundRect(cx-25, cy-5, cx+25, cy+12, 7f, 7f, paint)
    }
}
