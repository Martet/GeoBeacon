package com.xzmitk01.geobeacon

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState

// Data class to define the screen and its properties
data class Screen(
    val route: String,
    val title: Int,
    val icon: Int
)

// Enum to hold all the screens
enum class Screens(val screen: Screen) {
    History(Screen("history", R.string.history, R.drawable.outline_history_24)),
    Chat(Screen("chat", R.string.chat, R.drawable.outline_chat_24)),
    Settings(Screen("settings", R.string.settings, R.drawable.outline_settings_24))
}

enum class DevScreens(val screen: Screen) {
    Dialog(Screen("dialog", R.string.dialog, R.drawable.outline_chat_24)),
    Config(Screen("config", R.string.config, R.drawable.outline_edit_24)),
    Settings(Screen("settings", R.string.settings, R.drawable.outline_settings_24))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomNavigationBar(navController: NavHostController, devMode: Boolean = false) {
    val navBackStackEntry = navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry.value?.destination?.route

    NavigationBar {
        val screens: List<Screen> = if (devMode) {
            DevScreens.entries.map { it.screen }
        } else {
            Screens.entries.map { it.screen }
        }
        screens.forEach { item ->
            NavigationBarItem(
                icon = {
                    Icon(
                        painter = painterResource(id = item.icon),
                        contentDescription = stringResource(item.title)
                    )
                       },
                label = { Text(text = stringResource(item.title)) },
                selected = currentRoute?.startsWith(item.route) == true,
                onClick = {
                    navController.navigate(item.route) {
                        navController.graph.startDestinationRoute?.let { screenRoute ->
                            popUpTo(screenRoute) {
                                saveState = true
                            }
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}
