package com.example.geobeacon.ui

import android.Manifest
import android.annotation.SuppressLint
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
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresPermission
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.geobeacon.GeoBeaconApp
import com.example.geobeacon.R
import com.example.geobeacon.data.AnswerStatus
import kotlinx.coroutines.delay
import java.util.UUID

private val SERVICE_UUID: UUID = UUID.fromString("01ff0100-ba5e-f4ee-5ca1-eb1e5e4b1ce0")
private val CHARACTERISTIC_UUID: UUID = UUID.fromString("01ff0101-ba5e-f4ee-5ca1-eb1e5e4b1ce0")

@SuppressLint("MissingPermission")
@Composable
fun MainScreen() {
    var connectedDevice by rememberSaveable {
        mutableStateOf<BluetoothDevice?>(null)
    }

    AnimatedContent(
        targetState = connectedDevice,
        label = "Selected device",
    ) { device ->
        if (device == null) {
            ScanningScreen {
                connectedDevice = it
            }
        } else {
            ChatScreen(device = device) {
                connectedDevice = null
            }
        }
    }
}

@RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
@Composable
fun ScanningScreen(
    onConnect: (BluetoothDevice) -> Unit
) {
    val tag = "GeoBeacon"
    val scanSettings: ScanSettings = ScanSettings.Builder()
        .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
        .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
        .build()

    var scanning by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(1000)
        scanning = true
    }

    if (scanning) {
        BluetoothScanEffect(
            scanSettings = scanSettings,
            onScanFailed = {
                Log.d(tag, "Scan failed with error code $it")
            },
            onDeviceFound = { scanResult ->
                val scanRecord = scanResult.scanRecord
                if (scanRecord != null) {
                    Log.d(tag, "Device found: ${scanRecord.deviceName}")
                    if (scanRecord.deviceName == "NXP_WU") {
                        onConnect(scanResult.device)
                    }
                }
            },
        )
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Spacer(modifier = Modifier.fillMaxHeight(0.4f))
        Text(text = stringResource(R.string.scanning), style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        CircularProgressIndicator()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
fun ChatScreen(device: BluetoothDevice, onDisconnect: () -> Unit) {
    val context = LocalContext.current
    val application = context.applicationContext as GeoBeaconApp
    val viewModel: ChatViewModel = viewModel(factory = ChatViewModel.Factory(application.chatRepository))
    viewModel.setAddressName(device.address, device.name)
    var showConfirmationDialog by remember { mutableStateOf(false) }

    var state by remember(device) {
        mutableStateOf(DeviceConnectionState.None)
    }
    var characteristic by remember(state.services) {
        mutableStateOf(state.services.find { it.uuid == SERVICE_UUID }?.getCharacteristic(CHARACTERISTIC_UUID))
    }

    val conversation by viewModel.conversation.collectAsState()

    fun disconnect() {
        state = DeviceConnectionState.None
        viewModel.resetConversation()
        onDisconnect()
    }

    val listState = rememberLazyListState()

    val enableAnswer by remember(keys = arrayOf(state.gatt, state.connectionState, characteristic, conversation.finished), calculation = {
        val new = state.gatt != null && characteristic != null &&
                state.connectionState == BluetoothProfile.STATE_CONNECTED &&
                !conversation.finished
        Log.d("GeoBeacon", "Answering enabled: gatt=${state.gatt != null}, connectionState=${state.connectionState}, characteristic=${characteristic != null}")
        mutableStateOf(new)
    })

    var enableReconnect by remember(enableAnswer) {
        mutableStateOf(false)
    }
    var reconnect by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(enableAnswer) {
        if (!enableAnswer) {
            delay(1000)
            state.gatt?.discoverServices()
            delay(1000)
            enableReconnect = true
        }
    }

    if (!conversation.finished) {
        BLEConnectEffect(device = device, reconnect = reconnect, startState = state, onStateChange = {state = it}) {
            val trimmedMessage = it.trim()
            Log.d("GeoBeacon", "Message received: $trimmedMessage")
            when (trimmedMessage) {
                context.getString(R.string.protocol_wrong) -> viewModel.updateLastAnswer(AnswerStatus.ANSWER_WRONG)
                context.getString(R.string.protocol_correct) -> viewModel.updateLastAnswer(AnswerStatus.ANSWER_CORRECT)
                context.getString(R.string.protocol_end) -> viewModel.finishConversation()
                else -> viewModel.addMessage(trimmedMessage)
            }
        }
    }

    LaunchedEffect(key1 = conversation.messages.size, key2 = conversation.messages.lastOrNull()?.answers?.size, key3 = conversation.finished) {
        Log.d("GeoBeacon", "Scrolling down to message index ${conversation.messages.lastIndex}")
        if (conversation.messages.isNotEmpty()) {
            listState.animateScrollToItem(conversation.messages.lastIndex, scrollOffset = 5)
        }
    }

    Column {
        TopAppBar(
            title = { Text(device.name) },
            expandedHeight = 24.dp,
            actions = {
                if (enableReconnect) {
                    IconButton(onClick = { reconnect = !reconnect }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reconnect")
                    }
                }
            }
        )
        if (conversation.messages.isEmpty()) {
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                CircularProgressIndicator(modifier = Modifier.padding(all = 16.dp))
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                state = listState,
            ) {
                items(
                    count = conversation.messages.size,
                    key = { conversation.messages[it].id }
                ) { i ->
                    ChatMessage(
                        message = conversation.messages[i],
                        enableAnswer = enableAnswer,
                        onAnswer = { answer: String ->
                            sendData(state.gatt!!, characteristic!!, answer.trim() + "\n")
                            if (conversation.messages[i].closedQuestion) {
                                viewModel.updateLastAnswer(AnswerStatus.ANSWER_PENDING, answer.toInt() - 1)
                            } else {
                                viewModel.addAnswer(answer)
                            }
                        }
                    )
                }
                if (conversation.finished) {
                    item {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Icon(Icons.Default.Done, contentDescription = "Done")
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = stringResource(R.string.chat_finished),
                                    textAlign = TextAlign.Center
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(
                                onClick = { disconnect() }
                            ) {
                                Text(stringResource(R.string.disconnect))
                            }
                        }
                    }
                }
            }
        }
    }

    BackHandler(enabled = true) {
        if (conversation.finished) {
            disconnect()
        } else {
            showConfirmationDialog = true
        }
    }

    if (showConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmationDialog = false },
            title = { Text(stringResource(R.string.warning)) },
            text = { Text(stringResource(R.string.disconnect_dialog_warning)) },
            confirmButton = {
                TextButton(onClick = {
                    showConfirmationDialog = false
                    disconnect()
                }) {
                    Text(stringResource(R.string.yes))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showConfirmationDialog = false
                }) {
                    Text(stringResource(R.string.no))
                }
            }
        )
    }
}

@RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
@Composable
private fun BluetoothScanEffect(
    scanSettings: ScanSettings,
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    onScanFailed: (Int) -> Unit,
    onDeviceFound: (device: ScanResult) -> Unit,
) {
    val context = LocalContext.current
    val adapter = context.getSystemService(BluetoothManager::class.java).adapter

    if (adapter == null) {
        onScanFailed(ScanCallback.SCAN_FAILED_INTERNAL_ERROR)
        return
    }

    val currentOnDeviceFound by rememberUpdatedState(onDeviceFound)

    DisposableEffect(lifecycleOwner, scanSettings) {
        val leScanCallback: ScanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                super.onScanResult(callbackType, result)
                currentOnDeviceFound(result)
            }

            override fun onScanFailed(errorCode: Int) {
                super.onScanFailed(errorCode)
                onScanFailed(errorCode)
            }
        }

        val observer = LifecycleEventObserver { _, event ->
            // Start scanning once the app is in foreground and stop when in background
            if (event == Lifecycle.Event.ON_START) {
                Log.d("GeoBeacon", "Scanning...")
                adapter.bluetoothLeScanner.startScan(null, scanSettings, leScanCallback)
            } else if (event == Lifecycle.Event.ON_STOP) {
                Log.d("GeoBeacon", "Stopped Scanning...")
                adapter.bluetoothLeScanner.stopScan(leScanCallback)
            }
        }

        // Add the observer to the lifecycle
        lifecycleOwner.lifecycle.addObserver(observer)

        // When the effect leaves the Composition, remove the observer and stop scanning
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            adapter.bluetoothLeScanner.stopScan(leScanCallback)
            Log.d("GeoBeacon", "Stopped Scanning...")
        }
    }
}

@SuppressLint("MissingPermission")
private fun sendData(
    gatt: BluetoothGatt,
    characteristic: BluetoothGattCharacteristic,
    data: String
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        gatt.writeCharacteristic(
            characteristic,
            data.toByteArray(),
            BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE,
        )
    } else {
        characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
        @Suppress("DEPRECATION")
        characteristic.value = data.toByteArray()
        @Suppress("DEPRECATION")
        gatt.writeCharacteristic(characteristic)
    }
}

private data class DeviceConnectionState(
    val gatt: BluetoothGatt?,
    val connectionState: Int,
    val services: List<BluetoothGattService> = emptyList(),
) {
    companion object {
        val None = DeviceConnectionState(null, BluetoothProfile.STATE_DISCONNECTED)
    }
}

@RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
@Composable
private fun BLEConnectEffect(
    device: BluetoothDevice,
    startState: DeviceConnectionState = DeviceConnectionState.None,
    reconnect: Boolean = false,
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    onStateChange: (DeviceConnectionState) -> Unit,
    onMessageReceived: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val currentOnStateChange by rememberUpdatedState(onStateChange)
    val currentOnRead by rememberUpdatedState(onMessageReceived)

    val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    val service = BluetoothGattService(SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY)
    service.addCharacteristic(BluetoothGattCharacteristic(
        CHARACTERISTIC_UUID,
        BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_WRITE,
        BluetoothGattCharacteristic.PERMISSION_READ or BluetoothGattCharacteristic.PERMISSION_WRITE
    ))

    // Keep the current connection state
    var state by remember {
        mutableStateOf(startState)
    }
    var gattServer: BluetoothGattServer? by remember {
        mutableStateOf(null)
    }
    var disconnected by remember {
        mutableStateOf(false)
    }

    DisposableEffect(lifecycleOwner, device.address, reconnect) {
        // This callback will notify us when things change in the GATT connection so we can update
        // our state
        val gattCallback = object : BluetoothGattCallback() {
            @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
            override fun onConnectionStateChange(
                gatt: BluetoothGatt,
                status: Int,
                newState: Int,
            ) {
                super.onConnectionStateChange(gatt, status, newState)
                state = state.copy(gatt = gatt, connectionState = newState)
                currentOnStateChange(state)

                if (status != BluetoothGatt.GATT_SUCCESS) {
                    Log.e("BLEConnectEffect", "An error happened: $status")
                }
                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        gatt.requestMtu(512)
                        gatt.discoverServices()
                    }
                    BluetoothProfile.STATE_DISCONNECTED -> {
                        if (!disconnected) {
                            gatt.connect()
                        }
                    }
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                super.onServicesDiscovered(gatt, status)
                Log.d("BLEConnectEffect", "onServicesDiscovered")
                state = state.copy(services = gatt.services)
                currentOnStateChange(state)
            }
        }

        val gattServerCallback = object : BluetoothGattServerCallback() {
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
                    currentOnRead(value.decodeToString())
                }
            }
        }

        val observer = LifecycleEventObserver { _, event ->
            Log.d("BLEConnectEffect", "Lifecycle event: $event")
            if (event == Lifecycle.Event.ON_START) {
                if (gattServer == null) {
                    gattServer = bluetoothManager.openGattServer(context, gattServerCallback)
                    gattServer?.addService(service)
                }

                if (state.gatt != null) {
                    // If we previously had a GATT connection let's reestablish it
                    state.gatt?.connect()
                } else {
                    // Otherwise create a new GATT connection
                    state = DeviceConnectionState(
                        gatt = device.connectGatt(context, true, gattCallback),
                        connectionState = BluetoothProfile.STATE_DISCONNECTED
                    )
                }
                disconnected = false
            } else if (event == Lifecycle.Event.ON_STOP) {
                // Unless you have a reason to keep connected while in the bg you should disconnect
                state.gatt?.disconnect()
                disconnected = true
            }
        }

        // Add the observer to the lifecycle
        lifecycleOwner.lifecycle.addObserver(observer)

        // When the effect leaves the Composition, remove the observer and close the connection
        onDispose {
            Log.d("BLEConnectEffect", "onDispose")
            lifecycleOwner.lifecycle.removeObserver(observer)
            state.gatt?.close()
            state = DeviceConnectionState.None
            currentOnStateChange(state)
            gattServer?.close()
            gattServer = null
            disconnected = true
        }
    }
}