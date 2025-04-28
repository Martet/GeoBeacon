package com.example.geobeacon.ui

import android.icu.text.DateFormat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.geobeacon.GeoBeaconApp
import com.example.geobeacon.R
import com.example.geobeacon.data.ConversationData
import java.nio.file.WatchEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(clickedDetail: (Long) -> Unit) {
    val context = LocalContext.current
    val application = context.applicationContext as GeoBeaconApp
    val viewModel: HistoryViewModel = viewModel(factory = HistoryViewModel.Factory(application.repository))

    val conversations: List<ConversationData> = viewModel.conversations.collectAsState().value
    val dateFormatter = DateFormat.getDateInstance(DateFormat.SHORT)
    val timeFormatter = DateFormat.getTimeInstance(DateFormat.SHORT)

    Column {
        TopAppBar(
            title = { Text(text = stringResource(R.string.history_title)) },
            expandedHeight = 24.dp,
        )
        LazyColumn(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            state = rememberLazyListState()
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
        modifier = Modifier.fillMaxWidth().clickable { onClick(conversation.id) }
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