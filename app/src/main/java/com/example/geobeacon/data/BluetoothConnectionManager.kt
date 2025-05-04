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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class BluetoothConnectionManager(private val context: Context) {
    val WUART_SERVICE_UUID = UUID.fromString("01ff0100-ba5e-f4ee-5ca1-eb1e5e4b1ce0")
    val WUART_CHARACTERISTIC_UUID = UUID.fromString("01ff0101-ba5e-f4ee-5ca1-eb1e5e4b1ce0")

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
    private var gattServer: BluetoothGattServer? = null
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
                if (System.currentTimeMillis() - lastDisconnectTime < 2000) {
                    delay(2000)
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
        gatt?.close()
        gatt = null
        _deviceName.value = null
        _deviceAddress.value = null
        _ready.value = false
        lastDisconnectTime = System.currentTimeMillis()
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun writeCharacteristic(serviceUuid: UUID, charUuid: UUID, data: ByteArray) {
        val characteristic = gatt?.getService(serviceUuid)
            ?.getCharacteristic(charUuid)
            ?: throw IllegalStateException("GATT write: Characteristic not found")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            gatt?.writeCharacteristic(
                characteristic,
                data,
                BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE,
            )
        } else {
            characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            @Suppress("DEPRECATION")
            characteristic.value = data
            @Suppress("DEPRECATION")
            gatt?.writeCharacteristic(characteristic)
        }
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

    private val gattCallback: BluetoothGattCallback = object : BluetoothGattCallback() {
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onConnectionStateChange(_gatt: BluetoothGatt, status: Int, newState: Int) {
            super.onConnectionStateChange(_gatt, status, newState)
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    _gatt.requestMtu(512)
                    _gatt.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    _gatt.close()
                    _ready.value = false
                    CoroutineScope(Dispatchers.IO).launch {
                        delay(500)
                        gatt = device?.connectGatt(context, false, gattCallback)
                    }
                }
            }
        }

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            super.onServicesDiscovered(gatt, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                _ready.value = true
            } else {
                CoroutineScope(Dispatchers.IO).launch {
                    delay(500)
                    gatt.discoverServices()
                }
            }
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray, status: Int) {
            super.onCharacteristicRead(gatt, characteristic, value, status)
            Log.d("GeoBeacon", "Characteristic changed: $value")
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