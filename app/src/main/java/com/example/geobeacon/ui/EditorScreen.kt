package com.example.geobeacon.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
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
import com.example.geobeacon.GeoBeaconApp
import com.example.geobeacon.R
import com.example.geobeacon.data.DialogData
import com.example.geobeacon.data.StateData
import com.example.geobeacon.data.StateType
import com.example.geobeacon.data.TransitionData
import java.text.DateFormat

@Composable
fun EditorScreen() {
    val context = LocalContext.current
    val application = context.applicationContext as GeoBeaconApp
    val viewModel: EditorViewModel = viewModel(factory = EditorViewModel.Factory(application.editorRepository))

    val dialogs by viewModel.dialogs.collectAsState()
    val selectedDialog by viewModel.dialog.collectAsState()

    val listState = rememberLazyListState()

    AnimatedContent(targetState = selectedDialog, label = "Selected dialog") { dialog ->
        if (dialog == null) {
            DialogList(
                dialogs = dialogs,
                listState = listState,
                clickedDetail = { viewModel.setDialog(it) },
                onNewDialog = { viewModel.newDialog(it) }
            )
        } else {
            DialogDetail(
                viewModel = viewModel,
                dialog = dialog,
                onDelete = { viewModel.deleteDialog(dialog.id) },
                onBack = { viewModel.setDialog(null) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogList(dialogs: List<DialogData>, listState: LazyListState, clickedDetail: (DialogData) -> Unit, onNewDialog: (String) -> Unit) {
    val dateFormatter = remember { DateFormat.getDateInstance(DateFormat.SHORT) }
    val timeFormatter = remember { DateFormat.getTimeInstance(DateFormat.SHORT) }

    val showNewDialog = remember { mutableStateOf(false) }

    Column {
        TopAppBar(
            title = { Text(stringResource(R.string.editor_title)) },
            expandedHeight = 24.dp,
            actions = {
                IconButton(onClick = { showNewDialog.value = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add")
                }
            }
        )
        if (dialogs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.editor_no_dialogs))
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(16.dp).fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                state = listState,
                reverseLayout = true
            ) {
                items(dialogs.size) { i ->
                    DialogItem(
                        dialog = dialogs[i],
                        df = dateFormatter,
                        tf = timeFormatter,
                        onClick = clickedDetail
                    )
                }
            }
        }
    }

    if (showNewDialog.value) {
        CreateNameAlert(
            title = stringResource(R.string.new_dialog),
            onConfirm = { onNewDialog(it) },
            onDismiss = { showNewDialog.value = false }
        )
    }
}

@Composable
fun DialogItem(
    dialog: DialogData,
    df: DateFormat,
    tf: DateFormat,
    onClick: (DialogData) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(dialog) }
    ) {
        Column {
            Text(text = dialog.name, style = MaterialTheme.typography.titleMedium)
            Text(
                text = df.format(dialog.time) + " " + tf.format(dialog.time),
                style = MaterialTheme.typography.bodySmall
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (false) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = "Incomplete conversation",
                    modifier = Modifier.size(16.dp)
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Edit dialog",
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
fun DialogDetail(viewModel: EditorViewModel, dialog: DialogData, onDelete: () -> Unit, onBack: () -> Unit) {
    val states by viewModel.dialogStates.collectAsState()
    val selectedState by viewModel.state.collectAsState()

    val listState = rememberLazyListState()

    if (selectedState == null) {
        StateList(
            states = states,
            dialog = dialog,
            listState = listState,
            viewModel = viewModel,
        )
    } else {
        StateDetail(
            state = selectedState!!,
            viewModel = viewModel,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StateList(states: List<StateData>, dialog: DialogData, listState: LazyListState, viewModel: EditorViewModel) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showNewStateDialog by remember { mutableStateOf(false) }

    Column {
        TopAppBar(
            title = { Text(dialog.name) },
            expandedHeight = 24.dp,
            navigationIcon = {
                IconButton(onClick = { viewModel.setDialog(null) }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(Icons.Default.Delete, tint = Color.Red, contentDescription = "Delete")
                }
            }
        )
        LazyColumn(
            modifier = Modifier.padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            state = listState
        ) {
            items(states.size) { i ->
                StateItem(state = states[i], onClick = { viewModel.setState(states[i]) })
            }
            if (states.isEmpty()) {
                item {
                    Text(stringResource(R.string.editor_no_states))
                }
            }
            item {
                IconButton(onClick = { showNewStateDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add")
                }
            }
        }
    }

    BackHandler { viewModel.setDialog(null) }

    if (showDeleteDialog) {
        DeleteAlert(
            message = stringResource(R.string.delete_dialog_editor_warning),
            onDelete = { viewModel.deleteDialog(dialog.id) },
            onDismiss = { showDeleteDialog = false }
        )
    }

    if (showNewStateDialog) {
        CreateNameAlert(
            title = stringResource(R.string.editor_new_state),
            onConfirm = { viewModel.newState(it) },
            onDismiss = { showNewStateDialog = false }
        )
    }
}

@Composable
fun StateItem(state: StateData, onClick: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column {
            Text(text = state.name, style = MaterialTheme.typography.titleMedium)
            Text(
                text = state.text,
                style = MaterialTheme.typography.bodySmall,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (false) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = "Incomplete conversation",
                    modifier = Modifier.size(16.dp)
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Edit State",
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StateDetail(state: StateData, viewModel: EditorViewModel) {
    var showDeleteState by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    val transitions by viewModel.transitions.collectAsState()

    Column {
        TopAppBar(
            title = { Text(state.name) },
            expandedHeight = 24.dp,
            navigationIcon = {
                IconButton(onClick = { viewModel.setState(null) }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                IconButton(onClick = { showDeleteState = true }) {
                    Icon(Icons.Default.Delete, tint = Color.Red, contentDescription = "Delete")
                }
            }
        )
        LazyColumn(
            modifier = Modifier.padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            state = listState
        ) {
            items(transitions.size) { i ->
                AnswerItem(answer = transitions[i], type = state.type, viewModel = viewModel)
            }
            if (transitions.isEmpty()) {
                item {
                    Text(stringResource(R.string.editor_no_answers))
                }
            }
            item {
                IconButton(onClick = { viewModel.newTransition() }) {
                    Icon(Icons.Default.Add, contentDescription = "Add")
                }
            }
        }
    }

    BackHandler { viewModel.setState(null) }

    if (showDeleteState) {
        DeleteAlert(
            message = stringResource(R.string.delete_state_editor_warning),
            onDelete = { viewModel.deleteState(state.id) },
            onDismiss = { showDeleteState = false }
        )
    }
}

@Composable
fun AnswerItem(answer: TransitionData, type: StateType, viewModel: EditorViewModel) {
    Row {
        TextField(
            value = answer.answer,
            onValueChange = { viewModel.setAnswerText(answer, it) },
            label = { Text(stringResource(R.string.editor_answer)) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun CreateNameAlert(title: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            TextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(stringResource(R.string.name)) }
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(text) },
                enabled = text.isNotBlank()
            ) {
                Text(stringResource(R.string.create))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun DeleteAlert(message: String, onDelete: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.warning)) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onDelete) {
                Text(stringResource(R.string.yes))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.no))
            }
        }
    )
}