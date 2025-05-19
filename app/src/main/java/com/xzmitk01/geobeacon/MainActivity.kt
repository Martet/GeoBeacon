package com.xzmitk01.geobeacon

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.app.ActivityCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.xzmitk01.geobeacon.data.BluetoothConnectionManager
import com.xzmitk01.geobeacon.data.db.SettingsEntity
import com.xzmitk01.geobeacon.ui.ConfigurationScreen
import com.xzmitk01.geobeacon.ui.EditorScreen
import com.xzmitk01.geobeacon.ui.HistoryScreen
import com.xzmitk01.geobeacon.ui.MainScreen
import com.xzmitk01.geobeacon.ui.SettingsScreen
import com.xzmitk01.geobeacon.ui.SettingsViewModel
import com.xzmitk01.geobeacon.ui.theme.GeoBeaconTheme

class MainActivity : ComponentActivity() {
    val permissionsGranted = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val requestPermissionLauncher = registerForActivityResult(RequestMultiplePermissions()) {
            permissionsGranted.value = it.all { it.value }
        }

        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val application = context.applicationContext as GeoBeaconApp
            val settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory(application.settingsRepository))
            val settings by settingsViewModel.settings.collectAsState()

            val permissions = remember { getPermissions() }
            var permissionsGrantedState by remember { permissionsGranted }

            permissionsGrantedState = permissions.all {
                context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED
            }

            settings?.let { settings ->
                val darkTheme = if (settings.respectSystemTheme) isSystemInDarkTheme() else settings.darkMode

                GeoBeaconTheme(darkTheme = darkTheme) {
                    WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = !darkTheme

                    var showRationale by remember { mutableStateOf(false) }
                    var rationaleConfirmed by remember { mutableStateOf(false) }

                    when {
                        permissionsGrantedState -> {}

                        permissions.any {
                            ActivityCompat.shouldShowRequestPermissionRationale(this, it)
                        } -> showRationale = true

                        else -> requestPermissionLauncher.launch(permissions)
                    }

                    MainApp(settingsViewModel, settings, application.bluetoothManager, permissionsGrantedState)

                    if (showRationale && !rationaleConfirmed) {
                        AlertDialog(
                            onDismissRequest = {
                                rationaleConfirmed = true
                                showRationale = false
                            },
                            title = { Text(stringResource(R.string.permissions_required)) },
                            text = { Text(stringResource(R.string.permissions_required_message)) },
                            confirmButton = {
                                TextButton(onClick = {
                                    rationaleConfirmed = true
                                    showRationale = false
                                    requestPermissionLauncher.launch(permissions)
                                }) {
                                    Text(stringResource(R.string.confirm))
                                }
                            },
                            dismissButton = {
                                TextButton(
                                    onClick = {
                                        rationaleConfirmed = true
                                        showRationale = false
                                    }
                                ) {
                                    Text(stringResource(R.string.cancel))
                                }
                            }
                        )
                    }
                }
            } ?: run {
                Box(modifier = Modifier.fillMaxSize()) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(
    settingsViewModel: SettingsViewModel,
    settings: SettingsEntity,
    bluetoothManager: BluetoothConnectionManager,
    permissionsGranted: Boolean
) {
    val navController = rememberNavController()
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            BottomNavigationBar(navController = navController, devMode = settings.devMode)
        }
    ) { padding ->
        Log.d("GeoBeacon", "Padding: $padding")
        NavHost(
            navController,
            startDestination = when (settings.devMode) {
                true -> DevScreens.Config.screen.route
                false -> Screens.Chat.screen.route
            },
            modifier = Modifier
                .padding(bottom = padding.calculateBottomPadding())
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            if (settings.devMode) {
                composable(DevScreens.Dialog.screen.route) {
                    EditorScreen()
                }
                composable(DevScreens.Config.screen.route) {
                    ConfigurationScreen(bluetoothManager, permissionsGranted)
                }
                composable(DevScreens.Settings.screen.route) {
                    SettingsScreen(settingsViewModel)
                }
            } else {
                composable(Screens.History.screen.route) {
                    HistoryScreen()
                }
                composable(Screens.Chat.screen.route) {
                    MainScreen(bluetoothManager, permissionsGranted)
                }
                composable(Screens.Settings.screen.route) {
                    SettingsScreen(settingsViewModel)
                }
            }
        }
    }
}

fun getPermissions(): Array<String> {
    val permissions = mutableSetOf<String>()

    // For Android 11 and below, we need to request location permissions
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        permissions.add(Manifest.permission.BLUETOOTH_SCAN)
    } else {
        permissions.add(Manifest.permission.BLUETOOTH)
        permissions.add(Manifest.permission.BLUETOOTH_ADMIN)
    }

    return permissions.toTypedArray()
}