package com.example.geobeacon.data

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
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

class BluetoothConnectionManager(private val context: Context) {
    val WUART_SERVICE_UUID = UUID.fromString("01ff0100-ba5e-f4ee-5ca1-eb1e5e4b1ce0")
    val WUART_CHARACTERISTIC_UUID = UUID.fromString("01ff0101-ba5e-f4ee-5ca1-eb1e5e4b1ce0")

    val CONFIG_SERVICE_UUID = UUID.fromString("d077defe-750a-fa81-724a-81ca3fe60900")
    val CONFIG_PASSWORD_UUID = UUID.fromString("d077defe-750a-fa81-724a-81ca3fe60901")
    val CONFIG_SET_NAME_UUID = UUID.fromString("d077defe-750a-fa81-724a-81ca3fe60902")
    val CONFIG_SET_PASSWORD_UUID = UUID.fromString("d077defe-750a-fa81-724a-81ca3fe60903")
    val CONFIG_NOTIFICATION_UUID = UUID.fromString("d077defe-750a-fa81-724a-81ca3fe60904")
    val CONFIG_DATA_UUID = UUID.fromString("d077defe-750a-fa81-724a-81ca3fe60905")

    val GAP_SERVICE_UUID = UUID.fromString("00001800-0000-1000-8000-00805f9b34fb")
    val DEVICE_NAME_UUID = UUID.fromString("00002a00-0000-1000-8000-00805f9b34fb")

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
    private var gatt: BluetoothGatt? = null
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
    private var pendingContinuation: CancellableContinuation<Int>? = null
    private var continuationJob: Job? = null
    private val mutex = Mutex()

    private val _authorized = MutableStateFlow(false)
    val authorized: StateFlow<Boolean> = _authorized.asStateFlow()
    private val _authorizationError = MutableSharedFlow<Boolean>(replay = 1)
    val authorizationError: SharedFlow<Boolean> = _authorizationError.asSharedFlow()
    private val _deviceName = MutableStateFlow<String?>(null)
    val deviceName: StateFlow<String?> = _deviceName.asStateFlow()
    private val _deviceAddress = MutableStateFlow<String?>(null)
    val deviceAddress: StateFlow<String?> = _deviceAddress.asStateFlow()
    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready.asStateFlow()
    val messageChannel = Channel<String>(Channel.UNLIMITED)


    @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT])
    fun startScan() {
        if (!scanning) {
            scanning = true
            CoroutineScope(Dispatchers.IO).launch {
                Log.d("GeoBeacon", "Started scanning")
                if (System.currentTimeMillis() - lastDisconnectTime < 5000) {
                    delay(5000)
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

    @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT])
    fun destroy() {
        stopScan()
        gattServer?.close()
        disconnect()
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun connect(device: BluetoothDevice) {
        gatt = device.connectGatt(context, false, gattCallback)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun disconnect() {
        gatt?.disconnect()
        gatt?.close()
        gatt = null
        _deviceName.value = null
        _deviceAddress.value = null
        _ready.value = false
        _authorized.value = false
        lastDisconnectTime = System.currentTimeMillis()
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun authorize(password: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val res = setConfigurationCharacteristic(CONFIG_PASSWORD_UUID, password.toByteArray())
            if (res == BluetoothGatt.GATT_SUCCESS) {
                _authorized.value = true
                _authorizationError.tryEmit(false)
            } else {
                _authorized.value = false
                _authorizationError.tryEmit(true)
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    suspend fun setConfigurationCharacteristic(charUuid: UUID, data: ByteArray): Int {
        return mutex.withLock {
            suspendCancellableCoroutine { continuation ->
                Log.d("GeoBeacon", "Setting characteristic $charUuid to $data")
                val success = writeCharacteristic(CONFIG_SERVICE_UUID, charUuid, data, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
                if (success != BluetoothGatt.GATT_SUCCESS) {
                    Log.d("GeoBeacon", "Characteristic write failed with $success")
                    continuation.resume(success, onCancellation = null)
                    return@suspendCancellableCoroutine
                }

                pendingContinuation = continuation
                continuationJob = CoroutineScope(continuation.context).launch {
                    delay(5000)
                    if (continuation.isActive) {
                        Log.d("GeoBeacon", "Characteristic write timed out")
                        continuation.resume(BluetoothGatt.GATT_FAILURE, onCancellation = null)
                        pendingContinuation = null
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    suspend fun transferDialog(data: ByteArray, packets: Int): Int = suspendCancellableCoroutine { continuation ->
        val characteristic = gatt?.getService(CONFIG_SERVICE_UUID)
            ?.getCharacteristic(CONFIG_NOTIFICATION_UUID)
            ?: throw IllegalStateException("GATT write: Characteristic not found")
        pendingContinuation = continuation

        continuationJob = CoroutineScope(continuation.context).launch {
            for (i in 0 until packets) {
                Log.d("GeoBeacon", "Transferring packet $i/$packets")
                val packet = data.copyOfRange(i * DIALOG_PACKET_SIZE, minOf((i + 1) * DIALOG_PACKET_SIZE, data.size))
                if (writeCharacteristic(CONFIG_SERVICE_UUID, CONFIG_DATA_UUID, data = byteArrayOf(i.toUByte().toByte()) + packet) != BluetoothGatt.GATT_SUCCESS) {
                    Log.d("GeoBeacon", "Dialog write failed")
                    continuation.resume(BluetoothGatt.GATT_FAILURE, onCancellation = null)
                    return@launch
                }
                delay(10)
            }
            delay(100)
            gatt?.readCharacteristic(characteristic)

            delay(5000)
            if (continuation.isActive) {
                Log.d("GeoBeacon", "Dialog write timed out")
                continuation.resume(BluetoothGatt.GATT_FAILURE, onCancellation = null)
                pendingContinuation = null
            }
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun writeCharacteristic(serviceUuid: UUID, charUuid: UUID, data: ByteArray, writeType: Int = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE): Int {
        val characteristic = gatt?.getService(serviceUuid)
            ?.getCharacteristic(charUuid)
            ?: throw IllegalStateException("GATT write: Characteristic not found")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return gatt?.writeCharacteristic(characteristic, data, writeType)
                ?: BluetoothGatt.GATT_FAILURE
        } else {
            characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            @Suppress("DEPRECATION")
            characteristic.value = data
            @Suppress("DEPRECATION")
            return when (gatt?.writeCharacteristic(characteristic)) {
                true -> BluetoothGatt.GATT_SUCCESS
                else -> BluetoothGatt.GATT_FAILURE
            }
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun readName(setGatt: BluetoothGatt? = null) {
        var gatt = gatt
        if (setGatt != null) {
            gatt = setGatt
        }
        val nameCharacteristic = gatt?.getService(GAP_SERVICE_UUID)?.getCharacteristic(DEVICE_NAME_UUID)
        gatt?.readCharacteristic(nameCharacteristic)
    }

    private val scanCallback = object : ScanCallback() {
        @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT])
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            stopScan()
            device = result.device
            _deviceName.value = result.scanRecord?.deviceName
            _deviceAddress.value = result.device.address
            connect(result.device)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
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
                    _authorized.value = false
                    _gatt.close()
                    gatt = null
                    startScan()
                }
            }
        }

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            super.onServicesDiscovered(gatt, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                _ready.value = true
                Log.d("GeoBeacon", "Device short name: ${_deviceName.value}")
                readName(gatt)
            } else {
                CoroutineScope(Dispatchers.IO).launch {
                    delay(500)
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
                if (characteristic.uuid == DEVICE_NAME_UUID) {
                    _deviceName.value = value.toString(Charsets.UTF_8)
                    Log.d("GeoBeacon", "Device long name: ${_deviceName.value}")
                } else if (characteristic.uuid == CONFIG_NOTIFICATION_UUID) {
                    continuationJob?.cancel()
                    pendingContinuation?.resume(value[0].toInt(), onCancellation = null)
                    pendingContinuation = null
                }
            } else {
                Log.d("GeoBeacon", "Characteristic read failed with $status")
                continuationJob?.cancel()
                pendingContinuation?.resume(status, onCancellation = null)
                pendingContinuation = null
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
                pendingContinuation?.resume(status, onCancellation = null)
                pendingContinuation = null
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
                messageChannel.trySend(String(value))
            }
        }
    }
}