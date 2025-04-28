package com.example.geobeacon.data

import androidx.compose.runtime.Immutable
import java.util.Date

@Immutable
data class ConversationData(
    val name: String = "",
    val date: Date = Date(),
    val finished: Boolean = false,
    val messages: List<MessageData> = emptyList(),
    val id: Long = 0,
)

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