package com.xzmitk01.geobeacon.data

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.xzmitk01.geobeacon.R
import org.jgrapht.Graph
import org.jgrapht.graph.DefaultDirectedGraph
import org.jgrapht.graph.DefaultEdge
import org.jgrapht.traverse.DepthFirstIterator
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
    val validationStatus: ValidDialogStatus = ValidDialogStatus.UNCHECKED
) {
    private fun byteSize(): Int {
        return 3 + states.sumOf { it.byteSize() }
    }

    private fun createStateMap(): Map<Long, UInt> {
        val stateMap = mutableMapOf<Long, UInt>()
        states.forEachIndexed { i, it ->
            stateMap[it.id] = i.toUInt()
        }
        return stateMap
    }

    fun validate(): List<ValidationResult> {
        val results = mutableListOf<ValidationResult>()
        if (states.isEmpty()) {
            results.add(ValidationResult(false, R.string.editor_dialog_invalid_empty))
            return results
        }

        if (startState == null) {
            results.add(ValidationResult(false, R.string.editor_dialog_invalid_start))
        }
        if (finishState == null) {
            results.add(ValidationResult(false, R.string.editor_dialog_invalid_finish))
        }
        if (startState != null && finishState != null) {
            states.forEach {
                results.addAll(it.validate(it.id == finishState.id))
            }
            if (results.any { !it.valid }) {
                return results
            }

            val graph: Graph<String, DefaultEdge> = DefaultDirectedGraph(DefaultEdge::class.java)
            states.forEach { state ->
                graph.addVertex(state.name)
                state.answers.forEach { answer ->
                    if (answer.toState != null) {
                        graph.addVertex(answer.toState.name)
                        graph.addEdge(state.name, answer.toState.name)
                    }
                }
            }

            val reachable = mutableSetOf<String>()
            val iterator = DepthFirstIterator(graph, startState.name)
            while (iterator.hasNext()) {
                reachable += iterator.next()
            }
            val unreachable = graph.vertexSet().filterNot { it in reachable }
            if (finishState.name in unreachable) {
                results.add(ValidationResult(false, R.string.editor_dialog_invalid_path))
                return results
            }
            if (unreachable.isNotEmpty()) {
                results.add(
                    ValidationResult(
                        true,
                        R.string.editor_dialog_invalid_unreachable,
                        true,
                        unreachable.joinToString()
                    )
                )
            }

            val reversedGraph = DefaultDirectedGraph<String, DefaultEdge>(DefaultEdge::class.java)
            graph.vertexSet().forEach { reversedGraph.addVertex(it) }
            graph.edgeSet().forEach { edge ->
                val source = graph.getEdgeSource(edge)
                val dest = graph.getEdgeTarget(edge)
                reversedGraph.addEdge(dest, source)
            }

            val reachableFromFinal = mutableSetOf<String>()
            val dfs = DepthFirstIterator(reversedGraph, finishState.name)
            while (dfs.hasNext()) {
                reachableFromFinal += dfs.next()
            }
            val inescapable = reachable.filter {
                it !in reachableFromFinal && it != finishState.name && it != startState.name
            }
            if (inescapable.isNotEmpty()) {
                results.add(ValidationResult(false, R.string.editor_dialog_invalid_inescapable, false, inescapable.joinToString()))
            }
        }

        return results
    }

    fun serialize(): ByteArray {
        val stateIdMap = createStateMap()
        val buffer = ByteBuffer.allocate(byteSize())
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        buffer.put(stateIdMap[startState?.id]!!.toUByte().toByte()) // Starting state id
        buffer.put(stateIdMap[finishState?.id]!!.toUByte().toByte()) // Final state id
        buffer.put(states.size.toUByte().toByte()) // Number of states
        states.forEach {
            it.serialize(stateIdMap, buffer, it.id == finishState?.id)
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
            answers.foldIndexed("$text\n") { i, acc, transition -> acc + "${i + 1}) ${transition.answer}\n" }
        } else {
            "$text\n"
        }.toByteArray() + 0.toByte()
    }

    fun validate(isFinal: Boolean): List<ValidationResult> {
        val results = mutableListOf<ValidationResult>()
        if (text.isEmpty()) {
            results.add(ValidationResult(true, R.string.editor_state_invalid_empty, true, name))
        }
        if (getQuestionByteArray().size > 512) {
            results.add(ValidationResult(false, R.string.editor_state_text_invalid_message, false, name))
        }
        if (isFinal && type != StateType.MESSAGE) {
            results.add(ValidationResult(false, R.string.editor_state_final_not_message, false, name))
        }
        if (!isFinal) {
            var warningIncluded = false
            var errorIncluded = false
            answers.forEach {
                results.addAll(it.validate(this).filter {
                    var include = true
                    if (it.errorStringResource == R.string.editor_transition_empty_answer) {
                        if (warningIncluded) {
                            include = false
                        }
                        warningIncluded = true
                    }
                    include
                }.filter {
                    var include = true
                    if (it.errorStringResource == R.string.editor_transition_empty_to_state) {
                        if (errorIncluded) {
                            include = false
                        }
                        errorIncluded = true
                    }
                    include
                })
            }
        }
        return results
    }

    fun byteSize(): Int {
        return 6 + getQuestionByteArray().size + when(type) {
            StateType.MESSAGE -> 0
            StateType.OPEN_QUESTION -> answers.sumOf { it.byteSize() }
            StateType.CLOSED_QUESTION -> answers.size * 4
        }
    }

    fun serialize(stateIdMap: Map<Long, UInt>, buffer: ByteBuffer, isFinal: Boolean) {
        buffer.put(stateIdMap[id]!!.toByte()) // State id
        val text = getQuestionByteArray()
        buffer.putShort(text.size.toUShort().toShort()) // Text size
        buffer.put(text) // Text ByteArray
        if (type == StateType.MESSAGE && !isFinal) { // Direct next id
            buffer.put(stateIdMap[answers[0].toState?.id]!!.toUByte().toByte())
        } else {
            buffer.put(255.toUByte().toByte())
        }

        if (type == StateType.CLOSED_QUESTION) {
            buffer.put(1) // Is closed question
            buffer.put(answers.size.toUByte().toByte()) // Answer count
            answers.forEachIndexed { i, it ->
                buffer.put(2) // Answer text size
                buffer.put((i + 1).toString().toByteArray() + 0.toByte()) // Answer text
                buffer.put(stateIdMap[it.toState?.id]!!.toUByte().toByte()) // Target state
            }
        } else if (type == StateType.OPEN_QUESTION) {
            buffer.put(0) // Is open question
            buffer.put(answers.size.toUByte().toByte()) // Answer count
            answers.forEach {
                it.serialize(stateIdMap, buffer)
            }
        } else {
            buffer.put(0) // Is open question
            buffer.put(0) // Answer count
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
        buffer.put((answer.toByteArray().size + 1).toUByte().toByte()) // Answer size
        buffer.put(answer.toByteArray() + 0.toByte()) // Answer
        buffer.put(stateIdMap[toState?.id]!!.toUByte().toByte()) // Target state
    }

    fun validate(fromState: StateData): List<ValidationResult> {
        val results = mutableListOf<ValidationResult>()
        if (fromState.type != StateType.MESSAGE && answer.isEmpty()) {
            results.add(ValidationResult(true, R.string.editor_transition_empty_answer, true, fromState.name))
        }
        if (toState == null) {
            results.add(ValidationResult(false, R.string.editor_transition_empty_to_state, false, fromState.name))
        }
        return results
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

enum class ValidDialogStatus(val value: Int) {
    VALID(0),
    INVALID(1),
    WARNING(2),
    UNCHECKED(3);

    companion object {
        fun fromInt(value: Int): ValidDialogStatus = ValidDialogStatus.entries.find { it.value == value } ?: UNCHECKED
    }
}

fun ValidDialogStatus.toStringResource(): Int {
    return when (this.value) {
        ValidDialogStatus.VALID.value -> R.string.editor_dialog_valid
        ValidDialogStatus.INVALID.value -> R.string.editor_dialog_invalid
        ValidDialogStatus.WARNING.value -> R.string.editor_dialog_warning
        else -> R.string.editor_dialog_unchecked
    }
}

@Composable
fun ValidDialogStatus.toColor(): Color {
    return when (this.value) {
        ValidDialogStatus.WARNING.value -> Color(255, 165, 0, 255)
        ValidDialogStatus.VALID.value -> Color.Green
        ValidDialogStatus.INVALID.value -> Color.Red
        else -> MaterialTheme.colorScheme.primary
    }
}