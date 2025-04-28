package com.example.geobeacon.ui

import android.icu.text.DateFormat
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.geobeacon.GeoBeaconApp
import com.example.geobeacon.R
import com.example.geobeacon.data.ConversationData
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.nio.file.WatchEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen() {
    val context = LocalContext.current
    val application = context.applicationContext as GeoBeaconApp
    val viewModel: HistoryViewModel = viewModel(factory = HistoryViewModel.Factory(application.repository))

    val conversations: List<ConversationData> by viewModel.conversations.collectAsState()
    val selectedConversation by viewModel.conversation.collectAsState()

    AnimatedContent(targetState = selectedConversation, label = "Selected conversation") { conversation ->
        if (conversation == null) {
            ConversationList(
                conversations = conversations,
                clickedDetail = { viewModel.loadConversation(it) },
                onRefresh = { viewModel.loadConversations() }
            )
        } else {
            ConversationDetail(
                conversation = conversation,
                onDelete = { viewModel.deleteConversation(conversation.id) },
                onBack = { viewModel.setConversation(null) },
                onRefresh = { viewModel.loadConversation(conversation.id) })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationList(conversations: List<ConversationData>, clickedDetail: (Long) -> Unit, onRefresh: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    val dateFormatter = remember { DateFormat.getDateInstance(DateFormat.SHORT) }
    val timeFormatter = remember { DateFormat.getTimeInstance(DateFormat.SHORT) }

    var refreshing by remember { mutableStateOf(false) }

    Column {
        TopAppBar(
            title = { Text(text = stringResource(R.string.history_title)) },
            expandedHeight = 24.dp,
        )
        PullToRefreshBox(
            isRefreshing = refreshing,
            state = rememberPullToRefreshState(),
            onRefresh = {
                coroutineScope.launch {
                    refreshing = true
                    onRefresh()
                    delay(500)
                    refreshing = false
                }
            },
        ) {
            LazyColumn(
                modifier = Modifier.padding(16.dp).fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                state = rememberLazyListState(),
            ) {
                items(conversations.size) { i ->
                    ConversationItem(
                        conversation = conversations[i],
                        df = dateFormatter,
                        tf = timeFormatter,
                        onClick = clickedDetail
                    )
                }
            }
        }
    }
}

@Composable
fun ConversationItem(
    conversation: ConversationData,
    df: DateFormat,
    tf: DateFormat,
    onClick: (Long) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(conversation.id) }
    ) {
        Column {
            Text(text = conversation.name, style = MaterialTheme.typography.titleMedium)
            Text(
                text = df.format(conversation.date) + " " + tf.format(conversation.date),
                style = MaterialTheme.typography.bodySmall
            )
        }
        Row {
            if (!conversation.finished) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = "Incomplete conversation"
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Enter conversation"
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationDetail(conversation: ConversationData, onDelete: () -> Unit, onBack: () -> Unit, onRefresh: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    var showConfirmationDialog by remember { mutableStateOf(false) }

    var refreshing by remember { mutableStateOf(false) }

    Column {
        TopAppBar(
            title = { Text(conversation.name) },
            expandedHeight = 24.dp,
            actions = {
                IconButton(onClick = onRefresh) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Refresh",
                    )
                }
                IconButton(onClick = {
                    showConfirmationDialog = true
                }) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color.Red
                    )
                }
            }
        )
        PullToRefreshBox(
            isRefreshing = refreshing,
            state = rememberPullToRefreshState(),
            onRefresh = {
                coroutineScope.launch {
                    refreshing = true
                    onRefresh()
                    delay(500)
                    refreshing = false
                }
            },
        ) {
            LazyColumn(
                modifier = Modifier.padding(16.dp).fillMaxSize(),
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
    }

    if (showConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmationDialog = false },
            title = { Text(stringResource(R.string.warning)) },
            text = { Text(stringResource(R.string.delete_dialog_warning)) },
            confirmButton = {
                TextButton(onClick = onDelete) {
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

    BackHandler(onBack = onBack)
}