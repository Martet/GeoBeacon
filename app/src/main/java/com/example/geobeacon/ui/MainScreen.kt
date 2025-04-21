package com.example.geobeacon.ui

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.geobeacon.R
import com.example.geobeacon.ui.theme.GeoBeaconTheme
import kotlinx.coroutines.delay

@RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
@Composable
fun MainScreen(modifier: Modifier) {
    val navController = rememberNavController()

    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = "scanning"
    ) {
        composable("scanning") {
            ScanningScreen(
                onConnect = { navController.navigate("chatting") }
            )
        }
        composable("chatting") {
            ChatScreen(onDisconnect = {
                navController.navigate("scanning") {
                    popUpTo("chatting") { inclusive = true }
                }
            })
        }
    }
}

@RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
@Composable
fun ScanningScreen(
    onConnect: () -> Unit
) {
    val scanSettings: ScanSettings = ScanSettings.Builder()
        .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
        .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
        .build()

    BluetoothScanEffect(
        scanSettings = scanSettings,
        onScanFailed = { },
        onDeviceFound = { scanResult ->
            val scanRecord = scanResult.scanRecord
            if (scanRecord != null) {
                if (scanRecord.deviceName == "NXP_WU") {
                    onConnect()
                }
            }
        },
    )

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

@Composable
fun ChatScreen(onDisconnect: () -> Unit) {
    var showConfirmationDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = true) {
        showConfirmationDialog = true
    }

    if (showConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmationDialog = false },
            title = { Text(stringResource(R.string.warning)) },
            text = { Text(stringResource(R.string.disconnect_dialog_warning)) },
            confirmButton = {
                TextButton(onClick = {
                    showConfirmationDialog = false
                    onDisconnect()
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
    Text(text = "Chat Screen")
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
                adapter.bluetoothLeScanner.startScan(null, scanSettings, leScanCallback)
            } else if (event == Lifecycle.Event.ON_STOP) {
                adapter.bluetoothLeScanner.stopScan(leScanCallback)
            }
        }

        // Add the observer to the lifecycle
        lifecycleOwner.lifecycle.addObserver(observer)

        // When the effect leaves the Composition, remove the observer and stop scanning
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            adapter.bluetoothLeScanner.stopScan(leScanCallback)
        }
    }
}