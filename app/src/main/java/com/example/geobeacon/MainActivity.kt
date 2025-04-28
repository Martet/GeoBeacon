package com.example.geobeacon

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.navigation.NavArgument
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.example.geobeacon.ui.HistoryDetailScreen
import com.example.geobeacon.ui.HistoryScreen
import com.example.geobeacon.ui.MainScreen
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
            GeoBeaconTheme {
                if (permissions.all {
                        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
                    }) {
                    MainApp()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
@Composable
fun MainApp() {
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
            navigation(
                route = "history_route",
                startDestination = Screens.History.screen.route,
            ) {
                composable(Screens.History.screen.route) {
                    HistoryScreen(clickedDetail = { id ->
                        navController.navigate(Screens.History.screen.route + "/$id")
                    })
                }
                composable(
                    route = Screens.HistoryDetail.screen.route,
                    arguments = listOf(navArgument("conversationId") { type = NavType.LongType })
                ) {
                    val conversationId = it.arguments?.getLong("conversationId") ?: return@composable
                    HistoryDetailScreen(conversationId, onBack = {
                        navController.popBackStack() }
                    )
                }
            }
            composable(Screens.Chat.screen.route) {
                MainScreen()
            }
            composable(Screens.Settings.screen.route) {
                Text("Settings")
            }
        }
    }
}