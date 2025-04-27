package com.example.geobeacon.data

import androidx.compose.runtime.Immutable

@Immutable
data class MessageData(
    val question: String,
    val answers: List<MessageAnswer>,
    val id: Long = 0,
    var last: Boolean = false
)

@Immutable
data class MessageAnswer(
    val text: String,
    val status: AnswerStatus,
    val id: Long = 0,
)

enum class AnswerStatus(val value: Int) {
    ANSWER_CORRECT(0),
    ANSWER_WRONG(1),
    ANSWER_PENDING(2);

    companion object {
        fun fromInt(value: Int) = AnswerStatus.entries.find { it.value == value }
    }
}