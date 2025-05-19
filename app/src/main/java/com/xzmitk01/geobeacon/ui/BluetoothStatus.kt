package com.xzmitk01.geobeacon.ui

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.xzmitk01.geobeacon.R

@Composable
fun BluetoothStatus(
    permissionsGranted: Boolean,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current

    val packageManager = context.packageManager
    val hasBLE = remember {
        packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)
    }

    val bluetoothManager = remember {
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    }
    val bluetoothAdapter = remember { bluetoothManager?.adapter }
    var isBluetoothEnabled by remember {
        mutableStateOf(bluetoothAdapter?.isEnabled == true)
    }

    DisposableEffect(context, bluetoothAdapter) {
        if (bluetoothAdapter == null) {
            return@DisposableEffect onDispose {}
        }

        val bluetoothStateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                    isBluetoothEnabled = when (state) {
                        BluetoothAdapter.STATE_ON -> true
                        BluetoothAdapter.STATE_OFF -> false
                        else -> isBluetoothEnabled // Keep current state for intermediate states
                    }
                }
            }
        }

        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        context.registerReceiver(bluetoothStateReceiver, filter)

        // Initial check in case state changed before receiver was registered
        isBluetoothEnabled = bluetoothAdapter.isEnabled

        onDispose {
            context.unregisterReceiver(bluetoothStateReceiver)
        }
    }

    when {
        !permissionsGranted -> BluetoothErrorScreen(R.string.permissions_not_granted)
        bluetoothAdapter == null || !hasBLE -> BluetoothErrorScreen(R.string.ble_unavailable)
        !isBluetoothEnabled -> BluetoothErrorScreen(R.string.ble_diabled)
        else -> content()
    }
}

@Composable
fun BluetoothErrorScreen(messageResource: Int) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.fillMaxHeight(0.4f))
        Text(
            text = stringResource(messageResource),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
    }
}