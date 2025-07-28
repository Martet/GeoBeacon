package com.xzmitk01.geobeacon.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.xzmitk01.geobeacon.R
import com.xzmitk01.geobeacon.ui.viewModel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val settings by viewModel.settings.collectAsState()

    if (settings == null) {
        Box(modifier = Modifier.fillMaxSize()) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
    } else {
        Column {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
            )
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                SettingsItem {
                    Text(stringResource(R.string.settings_developer))
                    Switch(
                        checked = settings!!.devMode,
                        onCheckedChange = { viewModel.switchDevMode(it) }
                    )
                }
                HorizontalDivider()
                SettingsItem {
                    Text(stringResource(R.string.settings_respect_theme))
                    Switch(
                        checked = settings!!.respectSystemTheme,
                        onCheckedChange = { viewModel.switchRespectSystemTheme(it) }
                    )
                }
                SettingsItem {
                    Text(stringResource(R.string.settings_dark_mode))
                    Switch(
                        checked = settings!!.darkMode,
                        onCheckedChange = { viewModel.switchDarkMode(it) },
                        enabled = !settings!!.respectSystemTheme
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsItem(content: @Composable () -> Unit) {
    Row(
        modifier = Modifier.height(24.dp).fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        content()
    }
}