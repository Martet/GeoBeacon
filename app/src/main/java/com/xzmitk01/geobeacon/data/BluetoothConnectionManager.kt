package com.xzmitk01.geobeacon.data

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class BluetoothConnectionManager(private val context: Context) {
    val WUART_SERVICE_UUID = UUID.fromString("01ff0100-ba5e-f4ee-5ca1-eb1e5e4b1ce0")
    val WUART_CHARACTERISTIC_UUID = UUID.fromString("01ff0101-ba5e-f4ee-5ca1-eb1e5e4b1ce0")

    val CONFIG_SERVICE_UUID = UUID.fromString("d077defe-750a-fa81-724a-81ca3fe60900")
    val CONFIG_PASSWORD_UUID = UUID.fromString("d077defe-750a-fa81-724a-81ca3fe60901")
    val CONFIG_SET_NAME_UUID = UUID.fromString("d077defe-750a-fa81-724a-81ca3fe60902")
    val CONFIG_SET_PASSWORD_UUID = UUID.fromString("d077defe-750a-fa81-724a-81ca3fe60903")
    val CONFIG_NOTIFICATION_UUID = UUID.fromString("d077defe-750a-fa81-724a-81ca3fe60904")
    val CONFIG_DATA_UUID = UUID.fromString("d077defe-750a-fa81-724a-81ca3fe60905")
    val CONFIG_STATS_UUID = UUID.fromString("d077defe-750a-fa81-724a-81ca3fe60906")

    val GAP_SERVICE_UUID = UUID.fromString("00001800-0000-1000-8000-00805f9b34fb")
    val DEVICE_NAME_UUID = UUID.fromString("00002a00-0000-1000-8000-00805f9b34fb")

    val BATTERY_SERVICE_UUID = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb")
    val BATTERY_LEVEL_UUID = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb")

    val MAX_NAME_SIZE = 64
    val MAX_PASSWORD_SIZE = 64
    val DIALOG_PACKET_SIZE = 216

    private val scanSettings = ScanSettings.Builder()
        .setCallbackType(ScanSettings.CALLBACK_TYPE_FIRST_MATCH)
        .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
        .build()
    private val scanFilters = listOf(
        ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(WUART_SERVICE_UUID))
            .build()
    )

    private val bluetoothManager = context.getSystemService(BluetoothManager::class.java)
    private val bluetoothAdapter = bluetoothManager?.adapter
    private val bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner
    private var gattClient: BluetoothGatt? = null
    var gattServer: BluetoothGattServer? = null
    private val wuartService = BluetoothGattService(WUART_SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY).also {
        it.addCharacteristic(BluetoothGattCharacteristic(
            WUART_CHARACTERISTIC_UUID,
            BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
            BluetoothGattCharacteristic.PERMISSION_WRITE
        ))
    }
    private var lastDisconnectTime: Long = 0
    private var scanning = false
    private var device: BluetoothDevice? = null
    private var configContinuation: Continuation<Int>? = null
    private var readContinuation: Continuation<ByteArray?>? = null
    private var continuationJob: Job? = null
    private val mutex = Mutex()

    private val _authorized = MutableStateFlow(false)
    val authorized: StateFlow<Boolean> = _authorized.asStateFlow()
    private val _authorizationError = MutableSharedFlow<Int>(replay = 1)
    val authorizationError: SharedFlow<Int> = _authorizationError.asSharedFlow()
    private val _deviceName = MutableStateFlow<String?>(null)
    val deviceName: StateFlow<String?> = _deviceName.asStateFlow()
    private val _deviceAddress = MutableStateFlow<String?>(null)
    val deviceAddress: StateFlow<String?> = _deviceAddress.asStateFlow()
    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready.asStateFlow()
    private val _messageFlow = MutableSharedFlow<String>(replay = 10, extraBufferCapacity = 3)
    val messageFlow: SharedFlow<String> = _messageFlow.asSharedFlow()
    private val _batteryLevel = MutableStateFlow<Int?>(null)
    val batteryLevel: StateFlow<Int?> = _batteryLevel.asStateFlow()
    private val _stats = MutableStateFlow<StatsData?>(null)
    val stats: StateFlow<StatsData?> = _stats.asStateFlow()

    @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT])
    fun startScan() {
        if (!scanning) {
            scanning = true
            CoroutineScope(Dispatchers.IO).launch {
                Log.d("GeoBeacon", "Started scanning")
                val lastDisconnectDelay = System.currentTimeMillis() - lastDisconnectTime
                if (lastDisconnectDelay < 5000) {
                    delay(lastDisconnectDelay)
                }
                if (gattServer == null) {
                    startServer()
                }
                bluetoothLeScanner?.startScan(scanFilters, scanSettings, scanCallback)
            }
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun stopScan() {
        bluetoothLeScanner?.flushPendingScanResults(scanCallback)
        bluetoothLeScanner?.stopScan(scanCallback)
        scanning = false
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun startServer() {
        gattServer = bluetoothManager?.openGattServer(context, gattServerCallback)
        gattServer?.addService(wuartService)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun connect(device: BluetoothDevice) {
        gattClient = device.connectGatt(context, false, gattCallback)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun disconnect() {
        gattClient?.disconnect()
        gattClient?.close()
        gattClient = null
        _ready.value = false
        _deviceName.value = null
        _deviceAddress.value = null
        _authorized.value = false
        _batteryLevel.value = null
        _stats.value = null
        lastDisconnectTime = System.currentTimeMillis()
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun authorize(password: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val res = setConfigurationCharacteristic(CONFIG_PASSWORD_UUID, password.toByteArray())
            when (res) {
                BluetoothGatt.GATT_SUCCESS -> {
                    _authorized.value = true
                    _authorizationError.tryEmit(0)

                    readCharacteristic(BATTERY_SERVICE_UUID, BATTERY_LEVEL_UUID)
                    readCharacteristic(CONFIG_SERVICE_UUID, CONFIG_STATS_UUID)
                }
                BluetoothGatt.GATT_INSUFFICIENT_AUTHORIZATION -> {
                    _authorized.value = false
                    _authorizationError.tryEmit(1)
                }
                else -> {
                    _authorized.value = false
                    _authorizationError.tryEmit(2)
                }
            }
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    suspend fun setConfigurationCharacteristic(charUuid: UUID, data: ByteArray): Int {
        return mutex.withLock {
            suspendCoroutine { continuation ->
                Log.d("GeoBeacon", "Setting characteristic $charUuid to $data")
                val success = writeCharacteristic(CONFIG_SERVICE_UUID, charUuid, data, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
                if (success != BluetoothGatt.GATT_SUCCESS) {
                    Log.d("GeoBeacon", "Characteristic write failed with $success")
                    continuation.resume(success)
                    return@suspendCoroutine
                }

                configContinuation = continuation
                continuationJob = CoroutineScope(continuation.context).launch {
                    delay(5000)
                    if (configContinuation != null) {
                        Log.d("GeoBeacon", "Characteristic write timed out")
                        continuation.resume(BluetoothGatt.GATT_FAILURE)
                        configContinuation = null
                    }
                }
            }
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    suspend fun transferDialog(data: ByteArray, packets: Int): Int = suspendCoroutine { continuation ->
        val characteristic = gattClient?.getService(CONFIG_SERVICE_UUID)
            ?.getCharacteristic(CONFIG_NOTIFICATION_UUID)
            ?: throw IllegalStateException("GATT write: Characteristic not found")

        continuationJob = CoroutineScope(continuation.context).launch {
            for (i in 0 until packets) {
                Log.d("GeoBeacon", "Transferring packet $i/$packets")
                val packet = data.copyOfRange(i * DIALOG_PACKET_SIZE, minOf((i + 1) * DIALOG_PACKET_SIZE, data.size))
                if (writeCharacteristic(CONFIG_SERVICE_UUID, CONFIG_DATA_UUID, data = byteArrayOf(i.toUByte().toByte()) + packet) != BluetoothGatt.GATT_SUCCESS) {
                    Log.d("GeoBeacon", "Dialog write failed")
                    continuation.resume(BluetoothGatt.GATT_FAILURE)
                    return@launch
                }
                delay(10)
            }
            configContinuation = continuation
            delay(100)
            gattClient?.readCharacteristic(characteristic)

            delay(5000)
            if (configContinuation != null) {
                Log.d("GeoBeacon", "Dialog write timed out")
                continuation.resume(BluetoothGatt.GATT_FAILURE)
                configContinuation = null
            }
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun writeCharacteristic(serviceUuid: UUID, charUuid: UUID, data: ByteArray, writeType: Int = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE): Int {
        val characteristic = gattClient?.getService(serviceUuid)
            ?.getCharacteristic(charUuid)
            ?: throw IllegalStateException("GATT write: Characteristic not found")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return gattClient?.writeCharacteristic(characteristic, data, writeType)
                ?: BluetoothGatt.GATT_FAILURE
        } else {
            characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            @Suppress("DEPRECATION")
            characteristic.value = data
            @Suppress("DEPRECATION")
            return when (gattClient?.writeCharacteristic(characteristic)) {
                true -> BluetoothGatt.GATT_SUCCESS
                else -> BluetoothGatt.GATT_FAILURE
            }
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    suspend fun readCharacteristic(serviceUuid: UUID, charUuid: UUID): ByteArray? {
        return mutex.withLock {
            suspendCoroutine { continuation ->
                val char = gattClient?.getService(serviceUuid)?.getCharacteristic(charUuid)
                if (gattClient?.readCharacteristic(char) == false) {
                    continuation.resume(null)
                }
                readContinuation = continuation
            }
        }
    }

    private val scanCallback = object : ScanCallback() {
        @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT])
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            stopScan()
            device = result.device
            _deviceAddress.value = result.device.address
            connect(result.device)
        }
    }

    private val gattCallback: BluetoothGattCallback = object : BluetoothGattCallback() {
        @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT])
        override fun onConnectionStateChange(_gatt: BluetoothGatt, status: Int, newState: Int) {
            super.onConnectionStateChange(_gatt, status, newState)
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    _gatt.requestMtu(512)
                    _gatt.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    _ready.value = false
                    _deviceName.value = null
                    _deviceAddress.value = null
                    _authorized.value = false
                    _gatt.close()
                    gattClient = null
                    _stats.value = null
                    _batteryLevel.value = null
                    configContinuation?.resume(BluetoothGatt.GATT_FAILURE)
                    configContinuation = null
                    continuationJob?.cancel()
                    readContinuation?.resume(null)
                    readContinuation = null
                    startScan()
                }
            }
        }

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            super.onServicesDiscovered(gatt, status)
            CoroutineScope(Dispatchers.IO).launch {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.d("GeoBeacon", "Device short name: ${_deviceName.value}")
                    readCharacteristic(GAP_SERVICE_UUID, DEVICE_NAME_UUID)
                } else {
                    delay(250)
                    gatt.discoverServices()
                }
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int
        ) {
            super.onCharacteristicRead(gatt, characteristic, value, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                when (characteristic.uuid) {
                    DEVICE_NAME_UUID -> {
                        _deviceName.value = value.toString(Charsets.UTF_8)
                        _ready.value = true
                        Log.d("GeoBeacon", "Device long name: ${_deviceName.value}")
                        readContinuation?.resume(value)
                        readContinuation = null
                    }
                    CONFIG_NOTIFICATION_UUID -> {
                        continuationJob?.cancel()
                        configContinuation?.resume(value[0].toInt())
                        configContinuation = null
                    }
                    BATTERY_LEVEL_UUID -> {
                        Log.d("GeoBeacon", "Battery level: ${value[0]}")
                        _batteryLevel.value = value[0].toInt()
                        readContinuation?.resume(value)
                        readContinuation = null
                    }
                    CONFIG_STATS_UUID -> {
                        Log.d("GeoBeacon", "Stats: ${String(value)}")
                        _stats.value = StatsData(value)
                        readContinuation?.resume(value)
                        readContinuation = null
                    }
                }
            } else {
                Log.d("GeoBeacon", "Characteristic read failed with $status")
                continuationJob?.cancel()
                configContinuation?.resume(status)
                configContinuation = null
                readContinuation?.resume(null)
                readContinuation = null
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            super.onCharacteristicWrite(gatt, characteristic, status)
            if (
                listOf(CONFIG_SET_NAME_UUID, CONFIG_SET_PASSWORD_UUID, CONFIG_NOTIFICATION_UUID, CONFIG_PASSWORD_UUID)
                .contains(characteristic?.uuid)
            ) {
                Log.d("GeoBeacon", "Characteristic write status: $status")
                continuationJob?.cancel()
                configContinuation?.resume(status)
                configContinuation = null
            }
        }
    }

    private val gattServerCallback = object : BluetoothGattServerCallback() {
        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice?,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic?,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray?
        ) {
            super.onCharacteristicWriteRequest(
                device,
                requestId,
                characteristic,
                preparedWrite,
                responseNeeded,
                offset,
                value
            )
            if (value != null) {
                _messageFlow.tryEmit(String(value))
            }
        }
    }
}