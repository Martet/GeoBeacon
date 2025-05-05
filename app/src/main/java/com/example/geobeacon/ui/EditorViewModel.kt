package com.example.geobeacon.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.geobeacon.data.DialogData
import com.example.geobeacon.data.StateData
import com.example.geobeacon.data.StateType
import com.example.geobeacon.data.TransitionData
import com.example.geobeacon.data.db.EditorRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class EditorViewModel(private val repository: EditorRepository) : ViewModel() {
    val dialogs = repository.dialogsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf())

    private val _dialog = MutableStateFlow<DialogData?>(null)
    private val _dialogState: StateFlow<DialogData?> = _dialog.asStateFlow()
    @OptIn(ExperimentalCoroutinesApi::class)
    var dialog: StateFlow<DialogData?> = _dialogState
        .flatMapLatest { repository.getDialogFlow(it?.id ?: -1) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    var dialogStates: StateFlow<List<StateData>> = _dialogState
        .flatMapLatest { repository.getDialogStatesFlow(it?.id ?: -1) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _state = MutableStateFlow<StateData?>(null)
    val state: StateFlow<StateData?> = _state.asStateFlow()

    private val _transitions = MutableStateFlow<List<TransitionData>>(emptyList())
    val transitions: StateFlow<List<TransitionData>> = _transitions.asStateFlow()

    fun setDialog(newDialog: DialogData?) {
        _dialog.value = newDialog
    }

    fun newDialog(name: String) {
        viewModelScope.launch {
            val newDialogId = repository.insertDialog(DialogData(name = name))
            _dialog.value = repository.getDialog(newDialogId)
        }
    }

    fun deleteDialog(dialogId: Long) {
        viewModelScope.launch {
            _dialog.value = null
            repository.deleteDialog(dialogId)
        }
    }

    fun deleteState(stateId: Long) {
        viewModelScope.launch {
            _state.value = null
            repository.deleteState(stateId)
        }
    }

    fun newState(name: String, type: StateType = StateType.OPEN_QUESTION) {
        viewModelScope.launch {
            val newStateId = repository.insertState(StateData(name = name, type = type), _dialog.value?.id ?: -1)
            _state.value = repository.getState(newStateId)
            _transitions.value = emptyList()
        }
    }

    fun setState(state: StateData?) {
        viewModelScope.launch {
            _state.value = state
            _transitions.value = state?.answers ?: emptyList()
        }
    }

    fun newTransition() {
        _transitions.value = _transitions.value + TransitionData()
    }

    fun setAnswerText(transitionData: TransitionData, text: String) {
        _transitions.value = _transitions.value.map {
            if (it == transitionData) {
                it.copy(answer = text)
            } else {
                it
            }
        }
    }

    companion object {
        fun Factory(repository: EditorRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                EditorViewModel(repository)
            }
        }
    }
}