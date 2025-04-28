package com.example.geobeacon.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.geobeacon.GeoBeaconApp
import com.example.geobeacon.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryDetailScreen(conversationId: Long, onBack: () -> Unit) {
    val context = LocalContext.current
    val application = context.applicationContext as GeoBeaconApp
    val viewModel: HistoryDetailViewModel = viewModel(
        factory = HistoryDetailViewModel.Factory(application.repository, conversationId)
    )
    var showConfirmationDialog by remember { mutableStateOf(false) }

    val conversation = viewModel.conversation.collectAsState().value

    Column {
        TopAppBar(
            title = { Text(conversation.name) },
            expandedHeight = 24.dp,
            actions = {
                IconButton(onClick = {
                    showConfirmationDialog = true
                }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                }
            }
        )
        LazyColumn(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            state = rememberLazyListState()
        ) {
            items(conversation.messages.size) { i ->
                ChatMessage(
                    message = conversation.messages[i],
                    onAnswer = {}
                )
            }
        }
    }

    if (showConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmationDialog = false },
            title = { Text(stringResource(R.string.warning)) },
            text = { Text(stringResource(R.string.delete_dialog_warning)) },
            confirmButton = {
                TextButton(onClick = {
                    showConfirmationDialog = false
                    viewModel.deleteConversation()
                    onBack()
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