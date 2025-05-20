package com.xzmitk01.geobeacon.ui

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xzmitk01.geobeacon.GeoBeaconApp
import com.xzmitk01.geobeacon.R
import com.xzmitk01.geobeacon.data.BluetoothConnectionManager
import com.xzmitk01.geobeacon.data.ValidDialogStatus
import kotlinx.coroutines.flow.SharedFlow

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigurationScreen(bluetoothManager: BluetoothConnectionManager, permissionsGranted: Boolean) {
    val context = LocalContext.current
    val application = context.applicationContext as GeoBeaconApp
    val viewModel: ConfigurationViewModel = viewModel(factory = ConfigurationViewModel.Factory(application.editorRepository, bluetoothManager))

    val authorized by bluetoothManager.authorized.collectAsState()
    val deviceName by viewModel.deviceName.collectAsState()
    val ready by bluetoothManager.ready.collectAsState()
    val batteryLevel by bluetoothManager.batteryLevel.collectAsState()

    var showConfirmationDialog by remember { mutableStateOf(false) }

    BluetoothStatus(permissionsGranted) {
        if (ready) {
            Column {
                TopAppBar(
                    title = { Text(deviceName.toString(), overflow = TextOverflow.Ellipsis) },
                    actions = {
                        if (batteryLevel != null) {
                            Box(
                                modifier = Modifier.padding(horizontal = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.baseline_battery_0_bar_24),
                                    contentDescription = "Battery Status",
                                    modifier = Modifier.size(42.dp).rotate(90f)
                                )
                                Text(
                                    text = "$batteryLevel%",
                                    textAlign = TextAlign.Center,
                                    fontSize = 10.sp,
                                    modifier = Modifier.offset(x = (-2).dp)
                                )
                            }
                        }
                    }
                )

                if (authorized) {
                    ConfigurationScreenReady(viewModel)
                } else {
                    AuthorizationScreen(bluetoothManager.authorizationError) {
                        viewModel.authorize(it)
                    }
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
                            viewModel.reconnect()
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

            BackHandler {
                showConfirmationDialog = true
            }
        } else {
            bluetoothManager.startScan()
            ScanningScreen()
        }
    }
}

@Composable
fun AuthorizationScreen(errorFlow: SharedFlow<Int>, onAuthorize: (String) -> Unit) {
    var passwordText by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        errorFlow.collect {
            error = it
            loading = false
        }
    }

    Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.authorize),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            OutlinedTextField(
                value = passwordText,
                onValueChange = {
                    passwordText = it
                    error = 0
                },
                isError = error > 0,
                supportingText = {
                    if (error == 1) {
                        Text(stringResource(R.string.wrong_password))
                    } else if (error == 2) {
                        Text(stringResource(R.string.authorization_disabled))
                    }
                },
                label = { Text(stringResource(R.string.password)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                keyboardActions = KeyboardActions(onDone = {
                    if (!loading) {
                        loading = true
                        onAuthorize(passwordText)
                    }
                }),
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            TextButton(
                shape = MaterialTheme.shapes.small,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                onClick = {
                    if (!loading) {
                        loading = true
                        onAuthorize(passwordText)
                    }
                }
            ) {
                if (loading) {
                    CircularProgressIndicator()
                } else {
                    Text(
                        stringResource(R.string.login),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(9.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ConfigurationScreenReady(viewModel: ConfigurationViewModel) {
    val dialogs by viewModel.dialogs.collectAsState()
    val writeFailureResource by viewModel.writeFailureResource.collectAsState()
    val transferring by viewModel.transferring.collectAsState()

    val nameFieldContent by viewModel.nameFieldContent.collectAsState()
    val nameFieldError by viewModel.nameFieldError.collectAsState()
    val passwordFieldContent by viewModel.passwordFieldContent.collectAsState()
    val passwordFieldError by viewModel.passwordFieldError.collectAsState()
    val passwordRepeatFieldContent by viewModel.passwordRepeatFieldContent.collectAsState()
    val passwordRepeatFieldError by viewModel.passwordRepeatFieldError.collectAsState()
    val selectedDialog by viewModel.selectedDialog.collectAsState()

    if (writeFailureResource != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearWriteFailure() },
            title = { Text(stringResource(R.string.error)) },
            text = { Text(stringResource(R.string.config_error_message) + " " + stringResource(writeFailureResource!!)) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearWriteFailure() }) {
                    Text(stringResource(R.string.confirm))
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (arrayOf(nameFieldError, passwordFieldError, passwordRepeatFieldError).all { !it }
            && passwordRepeatFieldContent == passwordFieldContent
            && (arrayOf(nameFieldContent, passwordFieldContent, passwordRepeatFieldContent).any { it.isNotEmpty() }
                || selectedDialog != null)
        ) {
            FloatingActionButton(
                onClick = {
                    if (!transferring) {
                        viewModel.transferConfig()
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
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            OutlinedTextField(
                value = nameFieldContent,
                onValueChange = { viewModel.setNameFieldContent(it) },
                label = { Text(stringResource(R.string.new_name)) },
                isError = nameFieldError,
                supportingText = { if (nameFieldError) Text(stringResource(R.string.too_long)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider(modifier = Modifier.padding(bottom = 12.dp))

            OutlinedTextField(
                value = passwordFieldContent,
                onValueChange = { viewModel.setPasswordFieldContent(it) },
                label = { Text(stringResource(R.string.new_password)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                isError = passwordFieldError,
                supportingText = { if (passwordFieldError) Text(stringResource(R.string.too_long)) },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = passwordRepeatFieldContent,
                onValueChange = { viewModel.setPasswordRepeatFieldContent(it) },
                label = { Text(stringResource(R.string.repeat_password)) },
                isError = passwordRepeatFieldError,
                supportingText = { if (passwordRepeatFieldError) Text(stringResource(R.string.passwords_do_not_match)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider(modifier = Modifier.padding(bottom = 12.dp))

            InputDropdownMenu(
                label = stringResource(R.string.new_dialog_load),
                options = listOf(null) + dialogs,
                selectedOption = selectedDialog,
                onOptionSelected = { viewModel.selectDialog(it) },
                supportingText = { Text(stringResource(R.string.config_dialog_dropdown_info)) },
                optionLabel = { it?.name ?: "" },
                trailingIcon = { Icon(Icons.Default.Warning, "Warning", tint = Color(255, 165, 0, 255)) },
                iconCondition = { it?.validationStatus == ValidDialogStatus.WARNING }
            )
        }
    }
}