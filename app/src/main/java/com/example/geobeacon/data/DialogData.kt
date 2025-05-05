package com.example.geobeacon.data

import com.example.geobeacon.R
import java.util.Date

data class DialogData(
    val id: Long = 0,
    val name: String,
    val time: Date = Date(),
    val startState: StateData? = null,
    val finishState: StateData? = null,
    val states: List<StateData> = listOf(),
)

data class StateData(
    val id: Long = 0,
    val name: String,
    val text: String = "",
    val type: StateType,
    val answers: List<TransitionData> = listOf(),
)

data class TransitionData(
    val id: Long = 0,
    val answer: String = "",
    val toState: StateData? = null,
)

enum class StateType(val value: Int) {
    OPEN_QUESTION(0),
    CLOSED_QUESTION(1),
    MESSAGE(2);

    companion object {
        fun fromInt(value: Int) = StateType.entries.find { it.value == value }
    }
}

fun StateType.toStringResource(): Int {
    return when (this.value) {
        StateType.OPEN_QUESTION.value -> R.string.editor_state_type_open_question
        StateType.CLOSED_QUESTION.value -> R.string.editor_state_type_closed_question
        StateType.MESSAGE.value -> R.string.editor_state_type_message
        else -> R.string.editor_state_type_unknown
    }
}