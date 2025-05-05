package com.example.geobeacon.ui

import android.util.Log
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.geobeacon.GeoBeaconApp
import com.example.geobeacon.R
import com.example.geobeacon.data.DialogData
import com.example.geobeacon.data.StateData
import com.example.geobeacon.data.StateType
import com.example.geobeacon.data.TransitionData
import com.example.geobeacon.data.toStringResource
import kotlinx.coroutines.delay
import java.text.DateFormat

@Composable
fun EditorScreen() {
    val context = LocalContext.current
    val application = context.applicationContext as GeoBeaconApp
    val viewModel: EditorViewModel = viewModel(factory = EditorViewModel.Factory(application.editorRepository))

    val dialogs by viewModel.dialogs.collectAsState()
    val selectedDialog by viewModel.dialog.collectAsState()

    val listState = rememberLazyListState()

    if (selectedDialog == null) {
        DialogList(
            dialogs = dialogs,
            listState = listState,
            clickedDetail = { viewModel.setDialog(it) },
            onNewDialog = { viewModel.newDialog(it) }
        )
    } else {
        DialogDetail(
            viewModel = viewModel,
            dialog = selectedDialog!!,
            onDelete = { viewModel.deleteDialog(selectedDialog!!.id) },
            onBack = { viewModel.setDialog(null) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogList(dialogs: List<DialogData>, listState: LazyListState, clickedDetail: (DialogData) -> Unit, onNewDialog: (String) -> Unit) {
    val dateFormatter = remember { DateFormat.getDateInstance(DateFormat.SHORT) }
    val timeFormatter = remember { DateFormat.getTimeInstance(DateFormat.SHORT) }

    var showNewDialog by remember { mutableStateOf(false) }

    Column {
        TopAppBar(
            title = { Text(stringResource(R.string.editor_title)) },
            expandedHeight = 24.dp,
        )
        Box(modifier = Modifier.padding(16.dp).fillMaxSize()) {
            if (dialogs.isEmpty()) {
                Text(
                    stringResource(R.string.editor_no_dialogs),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
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

            FloatingActionButton(
                onClick = { showNewDialog = true },
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }
    }

    if (showNewDialog) {
        CreateNameAlert(
            title = stringResource(R.string.new_dialog),
            onConfirm = { onNewDialog(it) },
            onDismiss = { showNewDialog = false }
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

    var dialogName by remember { mutableStateOf(dialog.name) }

    LaunchedEffect(dialogName) {
        delay(300)
        viewModel.updateDialogName(dialogName)
    }

    Column {
        TopAppBar(
            title = { Text(stringResource(R.string.dialog) + ": " + dialog.name) },
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

        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            OutlinedTextField(
                value = dialogName,
                onValueChange = { dialogName = it },
                singleLine = true,
                label = { Text(stringResource(R.string.name)) },
                modifier = Modifier.fillMaxWidth()
            )
            InputDropdownMenu(
                label = stringResource(R.string.editor_state_starting),
                options = states,
                selectedOption = dialog.startState,
                onOptionSelected = { viewModel.setStartingState(it) },
                optionLabel = { it.name },
            )
            InputDropdownMenu(
                label = stringResource(R.string.editor_state_finishing),
                options = states,
                selectedOption = dialog.finishState,
                onOptionSelected = { viewModel.setFinishState(it) },
                optionLabel = { it.name },
            )

            HorizontalDivider()

            Box(modifier = Modifier.fillMaxSize()) {
                if (states.isEmpty()) {
                    Text(
                        stringResource(R.string.editor_no_states),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        state = listState
                    ) {
                        items(states.size) { i ->
                            StateItem(state = states[i], onClick = { viewModel.setState(states[i]) })
                        }
                    }
                }

                FloatingActionButton(
                    onClick = { showNewStateDialog = true },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
                ) {
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
    var showBackState by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    val states by viewModel.dialogStates.collectAsState()
    val transitions by viewModel.transitions.collectAsState()
    val modified by viewModel.isModified.collectAsState()

    Column {
        TopAppBar(
            title = { Text(stringResource(R.string.state) + ": " + state.name) },
            expandedHeight = 24.dp,
            navigationIcon = {
                IconButton(onClick = { showBackState = true }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                IconButton(onClick = { showDeleteState = true }) {
                    Icon(Icons.Default.Delete, tint = Color.Red, contentDescription = "Delete")
                }
            }
        )

        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.padding(16.dp).fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = state.name,
                    onValueChange = { viewModel.setStateIdentifier(it) },
                    singleLine = true,
                    label = { Text(stringResource(R.string.editor_state_identifier)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = state.text,
                    onValueChange = { viewModel.setStateText(it) },
                    singleLine = true,
                    label = {
                        if (state.type == StateType.MESSAGE) {
                            Text(stringResource(R.string.editor_state_text))
                        } else {
                            Text(stringResource(R.string.editor_state_question))
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                InputDropdownMenu(
                    label = stringResource(R.string.editor_state_type),
                    options = StateType.entries,
                    selectedOption = state.type,
                    onOptionSelected = { viewModel.setStateType(it) },
                    optionLabel = { stringResource(it.toStringResource()) },
                    modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider()

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    state = listState,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (state.type == StateType.MESSAGE) {
                        item {
                            InputDropdownMenu(
                                label = stringResource(R.string.editor_state),
                                options = states,
                                selectedOption = transitions.first().toState,
                                onOptionSelected = { viewModel.setAnswerState(0, it) },
                                optionLabel = { it.name },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    } else {
                        items(transitions.size) { i ->
                            AnswerItem(answer = transitions[i], index = i, viewModel = viewModel)
                        }
                        if (transitions.isEmpty()) {
                            item {
                                Text(stringResource(R.string.editor_no_answers))
                            }
                        }
                        item {
                            Row {
                                IconButton(onClick = { viewModel.newTransition() }) {
                                    Icon(Icons.Default.Add, contentDescription = "Add")
                                }
                                if (transitions.isNotEmpty()) {
                                    IconButton(
                                        onClick = { viewModel.deleteTransition() },
                                        modifier = Modifier.padding(start = 16.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            tint = Color.Red,
                                            contentDescription = "Delete"
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (modified) {
                ExtendedFloatingActionButton(
                    text = { Text(stringResource(R.string.save)) },
                    onClick = { viewModel.saveState() },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                    icon = {
                        Icon(painterResource(R.drawable.baseline_save_24), contentDescription = "Save changes")
                    }
                )
            }
        }
    }

    BackHandler { showBackState = true }

    if (showDeleteState) {
        DeleteAlert(
            message = stringResource(R.string.delete_state_editor_warning),
            onDelete = { viewModel.deleteState(state.id) },
            onDismiss = { showDeleteState = false }
        )
    }

    if (showBackState) {
        if (!modified){
            viewModel.setState(null)
        } else {
            DeleteAlert(
                message = stringResource(R.string.unsaved_state_editor_warning),
                onDelete = { viewModel.setState(null) },
                onDismiss = { showBackState = false }
            )
        }
    }
}

@Composable
fun AnswerItem(answer: TransitionData, index: Int, viewModel: EditorViewModel) {
    val states by viewModel.dialogStates.collectAsState()

    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = answer.answer,
            onValueChange = { viewModel.setAnswerText(index, it) },
            label = { Text(stringResource(R.string.editor_answer)) },
            singleLine = true,
            modifier = Modifier.weight(1f)
        )
        InputDropdownMenu(
            label = stringResource(R.string.editor_state),
            options = states,
            selectedOption = answer.toState,
            onOptionSelected = { viewModel.setAnswerState(index, it) },
            optionLabel = { it.name },
            modifier = Modifier.weight(1f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> InputDropdownMenu(
    label: String,
    options: List<T>,
    selectedOption: T?,
    onOptionSelected: (T) -> Unit,
    optionLabel: @Composable (T) -> String,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedText = if (selectedOption == null) " " else optionLabel(selectedOption)
    Log.d("DropdownMenu", "$options")
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedText,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.exposedDropdownSize()
        ) {
            options.forEach { item ->
                DropdownMenuItem(
                    text = { Text(optionLabel(item)) },
                    onClick = {
                        onOptionSelected(item)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun CreateNameAlert(title: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                shape = MaterialTheme.shapes.small,
                singleLine = true,
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