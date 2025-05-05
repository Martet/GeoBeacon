package com.example.geobeacon

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.geobeacon.data.BluetoothConnectionManager
import com.example.geobeacon.data.db.SettingsEntity
import com.example.geobeacon.ui.EditorScreen
import com.example.geobeacon.ui.HistoryScreen
import com.example.geobeacon.ui.MainScreen
import com.example.geobeacon.ui.SettingsScreen
import com.example.geobeacon.ui.SettingsViewModel
import com.example.geobeacon.ui.theme.GeoBeaconTheme

val permissions = arrayOf(
    Manifest.permission.BLUETOOTH_SCAN,
    Manifest.permission.BLUETOOTH_CONNECT
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val application = context.applicationContext as GeoBeaconApp
            val settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory(application.settingsRepository))
            val settings by settingsViewModel.settings.collectAsState()

            GeoBeaconTheme(
                darkTheme = if (settings.respectSystemTheme) isSystemInDarkTheme() else settings.darkMode
            ) {
                if (permissions.all {
                        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
                    }) {
                    if (application.bluetoothManager.gattServer == null){
                        application.bluetoothManager.startServer()
                    }
                    MainApp(settingsViewModel, settings, application.bluetoothManager)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(settingsViewModel: SettingsViewModel, settings: SettingsEntity, bluetoothManager: BluetoothConnectionManager) {
    val navController = rememberNavController()
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            BottomNavigationBar(navController = navController, devMode = settings.devMode)
        }
    ) { padding ->
        NavHost(
            navController,
            startDestination = when (settings.devMode) {
                true -> DevScreens.Config.screen.route
                false -> Screens.Chat.screen.route
            },
            modifier = Modifier.padding(padding)
        ) {
            if (settings.devMode) {
                composable(DevScreens.Dialog.screen.route) {
                    EditorScreen()
                }
                composable(DevScreens.Config.screen.route) {
                    Text("Config")
                }
                composable(DevScreens.Settings.screen.route) {
                    SettingsScreen(settingsViewModel)
                }
            } else {
                composable(Screens.History.screen.route) {
                    HistoryScreen()
                }
                composable(Screens.Chat.screen.route) {
                    MainScreen(bluetoothManager)
                }
                composable(Screens.Settings.screen.route) {
                    SettingsScreen(settingsViewModel)
                }
            }
        }
    }
}