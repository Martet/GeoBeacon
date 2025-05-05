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
import kotlinx.coroutines.flow.combine
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
    private val oldState = MutableStateFlow<StateData?>(null)
    val state: StateFlow<StateData?> = _state.asStateFlow()

    private val _transitions = MutableStateFlow<List<TransitionData>>(emptyList())
    private val oldTransitions = MutableStateFlow<List<TransitionData>>(emptyList())
    val transitions: StateFlow<List<TransitionData>> = _transitions.asStateFlow()

    val isModified = combine(_state, oldState, _transitions, oldTransitions) { state, oldState, transitions, oldTransitions ->
        state != oldState || transitions != oldTransitions
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

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

    fun updateDialogName(name: String) {
        viewModelScope.launch {
            repository.updateDialogName(_dialog.value!!.id, name)
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
            oldState.value = _state.value
            _transitions.value = emptyList()
            oldTransitions.value = _transitions.value
        }
    }

    fun setState(state: StateData?) {
        viewModelScope.launch {
            _state.value = state
            oldState.value = _state.value
            _transitions.value = state?.answers ?: emptyList()
            oldTransitions.value = _transitions.value
        }
    }

    fun saveState() {
        viewModelScope.launch {
            val state = _state.value?.copy(answers = _transitions.value) ?: return@launch
            repository.updateState(state, _dialog.value?.id ?: -1)
            setState(null)
        }
    }

    fun setStateIdentifier(identifier: String) {
        _state.value = _state.value?.copy(name = identifier)
    }

    fun setStateText(text: String) {
        _state.value = _state.value?.copy(text = text)
    }

    fun setStateType(type: StateType) {
        if (type == StateType.MESSAGE && (_state.value?.type == StateType.OPEN_QUESTION || _state.value?.type == StateType.CLOSED_QUESTION)) {
            _transitions.value = listOf(TransitionData())
        } else if ((type == StateType.OPEN_QUESTION || type == StateType.CLOSED_QUESTION) && _state.value?.type == StateType.MESSAGE) {
            _transitions.value = oldTransitions.value
        }
        _state.value = _state.value?.copy(type = type)
    }

    fun setStartingState(state: StateData) {
        viewModelScope.launch {
            repository.setStartingState(state, _dialog.value?.id ?: -1)
        }
    }

    fun setFinishState(state: StateData) {
        viewModelScope.launch {
            repository.setFinishState(state, _dialog.value?.id ?: -1)
        }
    }

    fun newTransition() {
        _transitions.value = _transitions.value + TransitionData()
    }

    fun deleteTransition() {
        _transitions.value = _transitions.value.dropLast(1)
    }

    fun setAnswerText(index: Int, text: String) {
        val newTransitions = _transitions.value.toMutableList()
        newTransitions[index] = newTransitions[index].copy(answer = text)
        _transitions.value = newTransitions
    }

    fun setAnswerState(index: Int, state: StateData) {
        val newTransitions = _transitions.value.toMutableList()
        newTransitions[index] = newTransitions[index].copy(toState = state)
        _transitions.value = newTransitions
    }

    companion object {
        fun Factory(repository: EditorRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                EditorViewModel(repository)
            }
        }
    }
}