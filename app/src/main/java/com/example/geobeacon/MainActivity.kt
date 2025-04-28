package com.example.geobeacon

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
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
import com.example.geobeacon.data.db.SettingsEntity
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
                    MainApp(settingsViewModel, settings)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
@Composable
fun MainApp(settingsViewModel: SettingsViewModel, settings: SettingsEntity) {
    val navController = rememberNavController()
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            BottomNavigationBar(navController = navController)
        }
    ) { padding ->
        NavHost(
            navController,
            startDestination = Screens.Chat.screen.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Screens.History.screen.route) {
                HistoryScreen()
            }
            composable(Screens.Chat.screen.route) {
                MainScreen()
            }
            composable(Screens.Settings.screen.route) {
                SettingsScreen(settingsViewModel)
            }
        }
    }
}