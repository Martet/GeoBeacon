package com.example.geobeacon.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.util.TableInfo
import com.example.geobeacon.GeoBeaconApp
import com.example.geobeacon.R
import com.example.geobeacon.data.BluetoothConnectionManager
import com.example.geobeacon.data.DialogData

@Composable
fun ConfigurationScreen(bluetoothManager: BluetoothConnectionManager) {
    val context = LocalContext.current
    val application = context.applicationContext as GeoBeaconApp
    val viewModel: ConfigurationViewModel = viewModel(factory = ConfigurationViewModel.Factory(application.editorRepository, bluetoothManager))

    val ready by viewModel.ready.collectAsState()

    if (ready) {
        ConfigurationScreenReady(viewModel)
    } else {
        ScanningScreen()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigurationScreenReady(viewModel: ConfigurationViewModel) {
    val deviceName by viewModel.deviceName.collectAsState()
    val dialogs by viewModel.dialogs.collectAsState()
    val writeFailureResource by viewModel.writeFailureResource.collectAsState()
    val transferring by viewModel.transferring.collectAsState()

    var nameFieldContent by remember(deviceName) { mutableStateOf(deviceName?.toString() ?: "") }
    var passwordFieldContent by remember { mutableStateOf("") }
    var passwordRepeatFieldContent by remember { mutableStateOf("") }
    var selectedDialog by remember { mutableStateOf<DialogData?>(null) }

    Column {
        TopAppBar(
            title = { Text(deviceName.toString(), overflow = TextOverflow.Ellipsis) },
            expandedHeight = 24.dp,
        )

        Box(modifier = Modifier.fillMaxSize()) {
            if (selectedDialog != null || passwordFieldContent != "" || passwordRepeatFieldContent != "" || nameFieldContent != deviceName) {
                FloatingActionButton(
                    onClick = {
                        if (!transferring) {
                            viewModel.transferConfig(
                                nameFieldContent,
                                passwordFieldContent,
                                passwordRepeatFieldContent,
                                selectedDialog
                            )
                        }
                    },
                    modifier = Modifier.padding(32.dp).align(Alignment.BottomEnd)
                ) {
                    if (transferring) {
                        CircularProgressIndicator()
                    } else {
                        Icon(
                            painterResource(R.drawable.baseline_save_24),
                            contentDescription = "Save"
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.padding(16.dp).fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                OutlinedTextField(
                    value = nameFieldContent,
                    onValueChange = { nameFieldContent = it },
                    label = { Text(stringResource(R.string.name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider()

                OutlinedTextField(
                    value = passwordFieldContent,
                    onValueChange = { passwordFieldContent = it },
                    label = { Text(stringResource(R.string.password)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = passwordRepeatFieldContent,
                    onValueChange = { passwordRepeatFieldContent = it },
                    label = { Text(stringResource(R.string.repeat_password)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider()

                InputDropdownMenu(
                    label = stringResource(R.string.new_dialog_load),
                    options = dialogs + null,
                    selectedOption = selectedDialog,
                    onOptionSelected = { selectedDialog = it },
                    optionLabel = { it?.name ?: "" },
                )
            }
        }
    }
}