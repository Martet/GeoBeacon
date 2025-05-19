package com.xzmitk01.geobeacon.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.xzmitk01.geobeacon.R
import com.xzmitk01.geobeacon.data.DialogData
import com.xzmitk01.geobeacon.data.StateData
import com.xzmitk01.geobeacon.data.StateType
import com.xzmitk01.geobeacon.data.TransitionData
import com.xzmitk01.geobeacon.data.ValidDialogStatus
import com.xzmitk01.geobeacon.data.ValidationResult
import com.xzmitk01.geobeacon.data.db.EditorRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
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

    private val _stateIdentifierValid: MutableStateFlow<ValidationResult> = MutableStateFlow(ValidationResult(true))
    val stateIdentifierValid = _stateIdentifierValid.asStateFlow()
    private val _stateIdentifier: MutableStateFlow<String> = MutableStateFlow("")
    val stateIdentifier = _stateIdentifier.asStateFlow()
    private val _stateTextValid: MutableStateFlow<ValidationResult> = MutableStateFlow(ValidationResult(true))
    val stateTextValid = _stateTextValid.asStateFlow()
    private val _stateText: MutableStateFlow<String> = MutableStateFlow("")
    val stateText = _stateText.asStateFlow()

    private val _dialogValidationResults = MutableStateFlow<List<ValidationResult>>(emptyList())
    val dialogValidationResults = _dialogValidationResults.asStateFlow()

    private var oldIsModified = false

    val isModified = combine(_state, oldState, _transitions, oldTransitions, _stateIdentifier, _stateText) {
        if (!oldIsModified) delay(300)
        oldIsModified = (it[0] != it[1] || it[2] != it[3] || it[4] != (it[1] as StateData?)?.name || it[5] != (it[1] as StateData?)?.text)
                && _answerTextValid.value.all { it.valid }
                && _stateIdentifierValid.value.valid
                && _stateTextValid.value.valid
        oldIsModified
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
            repository.setDialogValidation(ValidDialogStatus.UNCHECKED, _dialog.value?.id ?: -1)
        }
    }

    fun newState(name: String, type: StateType = StateType.OPEN_QUESTION) {
        viewModelScope.launch {
            val newStateId = repository.insertState(StateData(name = name, type = type), _dialog.value?.id ?: -1)
            _state.value = repository.getState(newStateId)
            oldState.value = _state.value
            _transitions.value = mutableListOf(TransitionData())
            oldTransitions.value = _transitions.value
            _stateIdentifier.value = name
            _stateText.value = ""
            _stateTextValid.value = ValidationResult(true)
            _stateIdentifierValid.value = ValidationResult(true)
            repository.setDialogValidation(ValidDialogStatus.UNCHECKED, _dialog.value?.id ?: -1)
        }
    }

    fun setState(state: StateData?) {
        viewModelScope.launch {
            _state.value = state
            oldState.value = _state.value
            _transitions.value = if (state?.type == StateType.MESSAGE && state.answers.isEmpty())
                mutableListOf(TransitionData())
                else state?.answers?.toMutableList()
                ?: mutableListOf()
            oldTransitions.value = _transitions.value
            _stateIdentifier.value = state?.name ?: ""
            _stateText.value = state?.text ?: ""
        }
    }

    fun saveState() {
        viewModelScope.launch {
            val state = _state.value?.copy(
                name = _stateIdentifier.value,
                text = _stateText.value,
                type = _state.value?.type ?: StateType.OPEN_QUESTION,
                answers = _transitions.value
            ) ?: return@launch
            repository.updateState(state, _dialog.value?.id ?: -1)
            repository.setDialogValidation(ValidDialogStatus.UNCHECKED, _dialog.value?.id ?: -1)
            setState(null)
        }
    }

    fun sizeValidator(text: String, size: Int): Boolean {
        return text.toByteArray(Charsets.UTF_8).size < size
    }

    private val stateIdentifierRegex = Regex("^[a-zA-Z0-9_-]*$")
    fun stateIdentifierValidator(identifier: String): ValidationResult {
        return if (sizeValidator(identifier, 16)) {
            if (identifier.isEmpty()) {
                ValidationResult(false, R.string.editor_invalid_empty)
            } else {
                if (identifier.matches(stateIdentifierRegex)) {
                    if (dialogStates.value.any { it.name == identifier }) {
                        ValidationResult(false, R.string.editor_state_identifier_invalid_unique)
                    } else {
                        ValidationResult(true)
                    }
                } else {
                    ValidationResult(false, R.string.editor_state_identifier_invalid_characters)
                }
            }
        } else {
            ValidationResult(false, R.string.editor_state_identifier_invalid_long)
        }
    }

    fun validateDialog(dialog: DialogData) {
        viewModelScope.launch {
            val fullDialog = repository.getDialogWithStates(dialog.id)
            val results = fullDialog!!.validate()
            if (results.any { !it.valid }) {
                repository.setDialogValidation(ValidDialogStatus.INVALID, dialog.id)
            } else if (results.any { it.warning }) {
                repository.setDialogValidation(ValidDialogStatus.WARNING, dialog.id)
            } else {
                repository.setDialogValidation(ValidDialogStatus.VALID, dialog.id)
            }

            if (results.isEmpty()) {
                _dialogValidationResults.value = listOf(ValidationResult(true))
            } else {
                _dialogValidationResults.value = results
            }
        }
    }

    fun resetValidationResults() {
        _dialogValidationResults.value = emptyList()
    }

    fun setStateIdentifier(identifier: String) {
        _stateIdentifierValid.value = stateIdentifierValidator(identifier)
        _stateIdentifier.value = identifier
    }

    fun setStateText(text: String) {
        _stateTextValid.value = if (sizeValidator(text, 512)) {
            ValidationResult(true)
        } else {
            ValidationResult(false, R.string.editor_state_text_invalid)
        }
        _stateText.value = text.replace("\n", "").replace("\r", "")
    }

    fun setStateType(type: StateType) {
        if (type == StateType.MESSAGE && (_state.value?.type == StateType.OPEN_QUESTION || _state.value?.type == StateType.CLOSED_QUESTION)) {
            _transitions.value = mutableListOf(TransitionData())
        } else if ((type == StateType.OPEN_QUESTION || type == StateType.CLOSED_QUESTION) && _state.value?.type == StateType.MESSAGE) {
            _transitions.value = oldTransitions.value.toMutableList()
        }
        _state.value = _state.value?.copy(type = type)
    }

    fun setStartingState(state: StateData) {
        viewModelScope.launch {
            repository.setStartingState(state, _dialog.value?.id ?: -1)
            repository.setDialogValidation(ValidDialogStatus.UNCHECKED, _dialog.value?.id ?: -1)
        }
    }

    fun setFinishState(state: StateData) {
        viewModelScope.launch {
            repository.setFinishState(state, _dialog.value?.id ?: -1)
            repository.setDialogValidation(ValidDialogStatus.UNCHECKED, _dialog.value?.id ?: -1)
        }
    }

    fun newTransition() {
        _transitions.value = _transitions.value + TransitionData()
    }

    fun deleteTransition() {
        _transitions.value = _transitions.value.dropLast(1)
    }

    private val _answerTextValid: MutableStateFlow<List<ValidationResult>> = MutableStateFlow(List(8) { ValidationResult(true) })
    val answerTextValid = _answerTextValid.asStateFlow()

    fun setAnswerText(index: Int, text: String) {
        val newValidations = _answerTextValid.value.toMutableList()
        newValidations[index] = if (sizeValidator(text, 32)) {
            ValidationResult(true)
        } else {
            ValidationResult(false, R.string.editor_state_text_invalid)
        }
        _answerTextValid.value = newValidations
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