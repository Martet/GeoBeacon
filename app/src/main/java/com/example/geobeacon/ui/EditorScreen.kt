package com.example.geobeacon.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import com.example.geobeacon.data.ValidationResult
import com.example.geobeacon.data.toStringResource
import kotlinx.coroutines.launch
import java.text.DateFormat

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun EditorScreen() {
    val context = LocalContext.current
    val application = context.applicationContext as GeoBeaconApp
    val viewModel: EditorViewModel = viewModel(factory = EditorViewModel.Factory(application.editorRepository))

    val dialogs by viewModel.dialogs.collectAsState()
    val selectedDialog by viewModel.dialog.collectAsState()

    val listState = rememberLazyListState()

    AnimatedSlideIn(selectedDialog) {
        if (it == null) {
            DialogList(
                dialogs = dialogs,
                listState = listState,
                clickedDetail = { viewModel.setDialog(it) },
                onNewDialog = { viewModel.newDialog(it) }
            )
        } else {
            DialogDetail(
                viewModel = viewModel,
                dialog = it,
            )
        }
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
        CreateInputAlert(
            titleResource = R.string.new_dialog,
            labelResource = R.string.name,
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
fun DialogDetail(viewModel: EditorViewModel, dialog: DialogData) {
    val states by viewModel.dialogStates.collectAsState()
    val selectedState by viewModel.state.collectAsState()

    val listState = rememberLazyListState()

    AnimatedSlideIn(selectedState) {
        if (it == null) {
            StateList(
                states = states,
                dialog = dialog,
                listState = listState,
                viewModel = viewModel,
            )
        } else {
            StateDetail(
                state = it,
                viewModel = viewModel,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StateList(states: List<StateData>, dialog: DialogData, listState: LazyListState, viewModel: EditorViewModel) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showNewStateDialog by remember { mutableStateOf(false) }

    var dialogName by remember { mutableStateOf(dialog.name) }

    Column {
        TopAppBar(
            title = { Text(stringResource(R.string.dialog) + ": " + dialog.name, overflow = TextOverflow.Ellipsis) },
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
            verticalArrangement = Arrangement.spacedBy(18.dp),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            OutlinedTextField(
                value = dialogName,
                onValueChange = { dialogName = it },
                singleLine = true,
                trailingIcon = {
                    if (dialogName != dialog.name) {
                        IconButton(onClick = { viewModel.updateDialogName(dialogName) }) {
                            Icon(painterResource(R.drawable.baseline_save_24), contentDescription = "Save")
                        }
                    }
                },
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

                if (states.size <= 16) {
                    FloatingActionButton(
                        onClick = { showNewStateDialog = true },
                        modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add")
                    }
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
        CreateInputAlert(
            titleResource = R.string.editor_new_state,
            labelResource = R.string.editor_state_identifier,
            validator = { viewModel.stateIdentifierValidator(it) },
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
    val coroutineScope = rememberCoroutineScope()

    var showDeleteState by remember { mutableStateOf(false) }
    var showBackState by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    val states by viewModel.dialogStates.collectAsState()
    val transitions by viewModel.transitions.collectAsState()
    val modified by viewModel.isModified.collectAsState()
    val identifierValid by viewModel.stateIdentifierValid.collectAsState()
    val textValid by viewModel.stateTextValid.collectAsState()
    val identifierText by viewModel.stateIdentifier.collectAsState()
    val stateText by viewModel.stateText.collectAsState()

    Column {
        TopAppBar(
            title = { Text(stringResource(R.string.state) + ": " + state.name, overflow = TextOverflow.Ellipsis) },
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

        Box(modifier = Modifier.padding(16.dp).fillMaxSize()) {
            this@Column.AnimatedVisibility(
                modified,
                modifier = Modifier.align(Alignment.BottomEnd),
                enter = fadeIn() + slideInVertically(initialOffsetY = {it / 2}),
                exit = fadeOut() + slideOutVertically()
            ) {
                ExtendedFloatingActionButton(
                    text = { Text(stringResource(R.string.save)) },
                    onClick = { viewModel.saveState() },
                    modifier = Modifier.padding(16.dp),
                    icon = {
                        Icon(painterResource(R.drawable.baseline_save_24), contentDescription = "Save changes")
                    }
                )
            }

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                OutlinedTextField(
                    value = identifierText,
                    onValueChange = { viewModel.setStateIdentifier(it) },
                    singleLine = true,
                    isError = !identifierValid.valid,
                    supportingText = { if (!identifierValid.valid) Text(stringResource(identifierValid.errorStringResource!!)) },
                    label = { Text(stringResource(R.string.editor_state_identifier)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = stateText,
                    onValueChange = { viewModel.setStateText(it) },
                    singleLine = true,
                    isError = !textValid.valid,
                    supportingText = { if (!textValid.valid) Text(stringResource(textValid.errorStringResource!!)) },
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

                HorizontalDivider(modifier = Modifier.padding(top = 16.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    state = listState,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    if (state.type == StateType.MESSAGE) {
                        item {
                            InputDropdownMenu(
                                label = stringResource(R.string.editor_state),
                                options = states,
                                selectedOption = transitions.firstOrNull()?.toState,
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
                                if (transitions.size < 8){
                                    IconButton(
                                        onClick = {
                                            viewModel.newTransition()
                                            coroutineScope.launch {
                                                listState.animateScrollToItem(transitions.size - 1)
                                            }
                                        }) {
                                        Icon(Icons.Default.Add, contentDescription = "Add")
                                    }
                                }
                                if (transitions.isNotEmpty()) {
                                    IconButton(
                                        onClick = { viewModel.deleteTransition() },
                                        modifier = if(transitions.size < 8) Modifier.padding(start = 16.dp) else Modifier
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
    val answerTextValid by viewModel.answerTextValid.collectAsState()

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
            isError = !answerTextValid[index].valid,
            supportingText = { if (!answerTextValid[index].valid) Text(stringResource(answerTextValid[index].errorStringResource!!)) },
            modifier = Modifier.weight(1f)
        )
        InputDropdownMenu(
            label = stringResource(R.string.editor_state),
            options = states,
            selectedOption = answer.toState,
            onOptionSelected = { viewModel.setAnswerState(index, it) },
            supportingText = { if (!answerTextValid[index].valid) Text(" ") },
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
    modifier: Modifier = Modifier,
    supportingText: @Composable (() -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedText = if (selectedOption == null) " " else optionLabel(selectedOption)
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
            supportingText = supportingText,
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
                if (options.indexOf(item) < options.size - 1) {
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
fun CreateInputAlert(
    titleResource: Int,
    labelResource: Int,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    validator: (String) -> ValidationResult = { ValidationResult(true) }
) {
    var text by remember { mutableStateOf("") }
    var validState by remember(text) { mutableStateOf(validator(text)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(titleResource)) },
        text = {
            OutlinedTextField(
                shape = MaterialTheme.shapes.small,
                singleLine = true,
                value = text,
                onValueChange = { text = it },
                isError = !validState.valid,
                supportingText = { if (!validState.valid) Text(stringResource(validState.errorStringResource!!)) },
                label = { Text(stringResource(labelResource)) }
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(text) },
                enabled = text.isNotBlank() && validState.valid
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

@Composable
fun <T> AnimatedSlideIn(state: T?, content: @Composable (AnimatedContentScope.(state: T?) -> Unit)) {
    AnimatedContent(
        state,
        transitionSpec = {
            if (targetState != null && initialState == null) {
                slideInHorizontally(tween(300)) { fullWidth -> fullWidth }.togetherWith(slideOutHorizontally(tween(100)) { fullWidth -> -fullWidth })
            } else if (targetState == null && initialState != null) {
                slideInHorizontally(tween(300)) { fullWidth -> -fullWidth }.togetherWith(slideOutHorizontally(tween(100)) { fullWidth -> fullWidth })
            } else {
                fadeIn(tween(0)) togetherWith fadeOut(tween(0))
            }
        },
        content = content
    )
}