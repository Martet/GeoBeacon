package com.example.geobeacon.data

import com.example.geobeacon.R
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Date

data class DialogData(
    val id: Long = 0,
    val name: String,
    val time: Date = Date(),
    val startState: StateData? = null,
    val finishState: StateData? = null,
    val states: List<StateData> = listOf(),
) {
    fun byteSize(): Int {
        return 3 + states.sumOf { it.byteSize() }
    }

    fun createStateMap(): Map<Long, UInt> {
        val stateMap = mutableMapOf<Long, UInt>()
        states.forEachIndexed { i, it ->
            stateMap[it.id] = i.toUInt()
        }
        return stateMap
    }

    fun serialize(): ByteArray {
        val stateIdMap = createStateMap()
        val buffer = ByteBuffer.allocate(byteSize())
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        buffer.put(stateIdMap[startState?.id]!!.toUByte().toByte())
        buffer.put(stateIdMap[finishState?.id]!!.toUByte().toByte())
        buffer.put(states.size.toUByte().toByte())
        states.forEach {
            it.serialize(stateIdMap, buffer)
        }
        return buffer.array()
    }
}

data class StateData(
    val id: Long = 0,
    val name: String,
    val text: String = "",
    val type: StateType,
    val answers: List<TransitionData> = listOf(),
) {
    fun getQuestionByteArray(): ByteArray {
        return if (type == StateType.CLOSED_QUESTION) {
            answers.foldIndexed("$text\n") { i, acc, transition -> acc + "$i) ${transition.answer}\n" }
        } else {
            "$text\n"
        }.toByteArray() + 0.toByte()
    }

    fun byteSize(): Int {
        return 6 + getQuestionByteArray().size + when(type) {
            StateType.MESSAGE -> 0
            StateType.OPEN_QUESTION -> answers.sumOf { it.byteSize() }
            StateType.CLOSED_QUESTION -> answers.size * 4
        }
    }

    fun serialize(stateIdMap: Map<Long, UInt>, buffer: ByteBuffer) {
        buffer.put(stateIdMap[id]!!.toByte())
        val text = getQuestionByteArray()
        buffer.putShort(text.size.toUShort().toShort())
        buffer.put(text)
        if (type == StateType.MESSAGE) {
            buffer.put(stateIdMap[answers[0].toState?.id]!!.toUByte().toByte())
        } else {
            buffer.put(255.toUByte().toByte())
        }
        if (type == StateType.CLOSED_QUESTION) {
            buffer.put(1)
            buffer.put(answers.size.toUByte().toByte())
            answers.forEachIndexed { i, it ->
                buffer.put(2)
                buffer.put((i + 1).toString().toByteArray() + 0.toByte())
                buffer.put(stateIdMap[it.toState?.id]!!.toUByte().toByte())
            }
        } else if (type == StateType.OPEN_QUESTION) {
            buffer.put(0)
            buffer.put(answers.size.toUByte().toByte())
            answers.forEach {
                it.serialize(stateIdMap, buffer)
            }
        } else {
            buffer.put(0)
            buffer.put(0)
        }
    }
}

data class TransitionData(
    val id: Long = 0,
    val answer: String = "",
    val toState: StateData? = null,
) {
    fun byteSize(): Int {
        return 3 + answer.toByteArray().size
    }

    fun serialize(stateIdMap: Map<Long, UInt>, buffer: ByteBuffer) {
        buffer.put((answer.toByteArray().size + 1).toUByte().toByte())
        buffer.put(answer.toByteArray() + 0.toByte())
        buffer.put(stateIdMap[toState?.id]!!.toUByte().toByte())
    }
}

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