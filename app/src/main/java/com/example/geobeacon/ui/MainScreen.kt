package com.example.geobeacon.ui

import android.Manifest
import android.annotation.SuppressLint
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.geobeacon.GeoBeaconApp
import com.example.geobeacon.R
import com.example.geobeacon.data.BluetoothConnectionManager
import kotlinx.coroutines.delay

@SuppressLint("MissingPermission")
@Composable
fun MainScreen(bluetoothManager: BluetoothConnectionManager) {
    val context = LocalContext.current
    val application = context.applicationContext as GeoBeaconApp
    val viewModel: ChatViewModel = viewModel(factory = ChatViewModel.Factory(application.chatRepository, bluetoothManager))
    val connectedDevice by viewModel.deviceName.collectAsState()

    if (connectedDevice == null) {
        ScanningScreen()
    } else {
        ChatScreen(viewModel)
    }
}

@Composable
fun ScanningScreen() {
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

@OptIn(ExperimentalMaterial3Api::class)
@RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
@Composable
fun ChatScreen(viewModel: ChatViewModel) {
    var showConfirmationDialog by remember { mutableStateOf(false) }

    val conversation by viewModel.conversation.collectAsState()
    val enableAnswer by viewModel.enableAnswer.collectAsState()
    val deviceName by viewModel.deviceName.collectAsState()

    val listState = rememberLazyListState()

    var enableReconnect by remember(enableAnswer) {
        mutableStateOf(false)
    }

    LaunchedEffect(enableAnswer) {
        if (!enableAnswer) {
            delay(1000)
            enableReconnect = true
        }
    }

    LaunchedEffect(key1 = conversation.messages.size, key2 = conversation.messages.lastOrNull()?.answers?.size, key3 = conversation.finished) {
        Log.d("GeoBeacon", "Scrolling down to message index ${conversation.messages.lastIndex}")
        if (conversation.messages.isNotEmpty()) {
            listState.animateScrollToItem(conversation.messages.lastIndex, scrollOffset = 5)
        }
    }

    Column {
        TopAppBar(
            title = { Text(deviceName.toString()) },
            expandedHeight = 24.dp,
            actions = {
                if (enableReconnect) {
                    IconButton(onClick = { viewModel.resetConversation() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reconnect")
                    }
                }
            }
        )
        if (conversation.messages.isEmpty()) {
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                CircularProgressIndicator(modifier = Modifier.padding(all = 16.dp))
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                state = listState,
            ) {
                items(
                    count = conversation.messages.size,
                    key = { conversation.messages[it].id }
                ) { i ->
                    ChatMessage(
                        message = conversation.messages[i],
                        enableAnswer = enableAnswer && !conversation.finished,
                        onAnswer = { viewModel.addAnswer(it.trim()) }
                    )
                }
                if (conversation.finished) {
                    item {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Icon(Icons.Default.Done, contentDescription = "Done")
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = stringResource(R.string.chat_finished),
                                    textAlign = TextAlign.Center
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(
                                onClick = { viewModel.resetConversation() }
                            ) {
                                Text(stringResource(R.string.disconnect))
                            }
                        }
                    }
                }
            }
        }
    }

    BackHandler {
        if (conversation.finished) {
            viewModel.resetConversation()
        } else {
            showConfirmationDialog = true
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
                    viewModel.resetConversation()
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
}