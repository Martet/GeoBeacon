package com.xzmitk01.geobeacon.ui

import android.icu.text.DateFormat
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xzmitk01.geobeacon.GeoBeaconApp
import com.xzmitk01.geobeacon.R
import com.xzmitk01.geobeacon.data.ConversationData
import com.xzmitk01.geobeacon.ui.viewModel.HistoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen() {
    val context = LocalContext.current
    val application = context.applicationContext as GeoBeaconApp
    val viewModel: HistoryViewModel = viewModel(factory = HistoryViewModel.Factory(application.chatRepository))

    val conversations by viewModel.conversations.collectAsState()
    val selectedConversation by viewModel.conversation.collectAsState()

    val listState = rememberLazyListState()

    AnimatedSlideIn(selectedConversation) { conversation ->
        if (conversation == null) {
            ConversationList(
                conversations = conversations,
                listState = listState,
                clickedDetail = { viewModel.setConversation(it) },
            )
        } else {
            ConversationDetail(
                conversation = conversation,
                onDelete = { viewModel.deleteConversation(conversation.id) },
                onBack = { viewModel.setConversation(null) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationList(conversations: List<ConversationData>, listState: LazyListState, clickedDetail: (ConversationData) -> Unit) {
    val dateFormatter = remember { DateFormat.getDateInstance(DateFormat.SHORT) }
    val timeFormatter = remember { DateFormat.getTimeInstance(DateFormat.SHORT) }

    Column {
        TopAppBar(
            title = { Text(text = stringResource(R.string.history_title)) },
        )
        if (conversations.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.history_no_conversations))
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(16.dp).fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                state = listState,
                reverseLayout = true
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
    onClick: (ConversationData) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(conversation) }
    ) {
        Column {
            Text(text = conversation.name, style = MaterialTheme.typography.titleMedium)
            Text(
                text = df.format(conversation.date) + " " + tf.format(conversation.date),
                style = MaterialTheme.typography.bodySmall
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!conversation.finished) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = "Incomplete conversation",
                    modifier = Modifier.size(16.dp)
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Enter conversation",
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationDetail(conversation: ConversationData, onDelete: () -> Unit, onBack: () -> Unit) {
    var showConfirmationDialog by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    Column {
        TopAppBar(
            title = { Text(conversation.name, overflow = TextOverflow.Ellipsis) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
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
        LazyColumn(
            modifier = Modifier.padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            state = listState
        ) {
            items(conversation.messages.size) { i ->
                ChatMessage(
                    message = conversation.messages[i],
                    enableAnswer = false,
                    onAnswer = {}
                )
            }
            if (!conversation.finished) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = "Ongoing conversation",
                            tint = Color.Gray,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(stringResource(R.string.history_incomplete_conversation))
                    }
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