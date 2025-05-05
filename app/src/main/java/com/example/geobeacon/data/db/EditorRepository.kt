package com.example.geobeacon.data.db

import com.example.geobeacon.data.DialogData
import com.example.geobeacon.data.StateData
import com.example.geobeacon.data.StateType
import com.example.geobeacon.data.TransitionData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Date

class EditorRepository(private val dao: EditorDao) {
    /*private suspend fun mapDialog(dialog: DialogWithStatesAndTransitions): DialogData {
        val states = dialog.states.map { state ->
            StateData(
                state.state.id, state.state.name, state.state.text,
                StateType.fromInt(state.state.type)!!,
                state.transitions.map { transition ->
                    TransitionData(
                        transition.id, transition.answer,
                        dao.getState(transition.toState)?.let {
                            StateData(it.id, it.name, it.text, StateType.fromInt(it.type)!!)
                        }
                    )
                }
            )
        }
        return DialogData(
            dialog.dialog.id,
            dialog.dialog.name,
            Date(dialog.dialog.timestamp),
            states.firstOrNull { it.id == dialog.dialog.startState },
            states.firstOrNull { it.id == dialog.dialog.endState },
            states
        )
    }*/

    private suspend fun mapDialog(dialog: DialogEntity): DialogData {
        return DialogData(
            dialog.id,
            dialog.name,
            Date(dialog.timestamp),
            dao.getState(dialog.startState)?.let {
                StateData(it.id, it.name, it.text, StateType.fromInt(it.type)!!)
            },
            dao.getState(dialog.endState)?.let {
                StateData(it.id, it.name, it.text, StateType.fromInt(it.type)!!)
            }
        )
    }

    val dialogsFlow: Flow<List<DialogData>> = dao.getDialogsFlow().map { dialogEntities ->
        dialogEntities.map { dialogEntity ->
            DialogData(
                dialogEntity.id,
                dialogEntity.name,
                Date(dialogEntity.timestamp)
            )
        }
    }

    suspend fun insertDialog(dialog: DialogData): Long {
        return dao.insertDialog(DialogEntity(
            dialog.id,
            dialog.name,
            dialog.time.time,
            dialog.startState?.id,
            dialog.finishState?.id)
        )
    }

    suspend fun getDialog(id: Long): DialogData? {
        val dialog = dao.getDialog(id) ?: return null
        return mapDialog(dialog)
    }

    fun getDialogFlow(id: Long): Flow<DialogData?> {
        return dao.getDialogFlow(id).map { dialog ->
            if (dialog == null) {
                null
            } else {
                mapDialog(dialog)
            }
        }
    }

    suspend fun deleteDialog(dialogId: Long) {
        dao.deleteDialog(dialogId)
    }

    suspend fun updateDialogName(dialogId: Long, name: String) {
        val dialog = dao.getDialog(dialogId) ?: throw Exception("Dialog not found")
        dao.updateDialog(dialog.copy(name = name))
    }

    suspend fun deleteState(stateId: Long) {
        dao.deleteState(stateId)
    }

    suspend fun insertState(state: StateData, dialogId: Long): Long {
        return dao.insertState(StateEntity(
            state.id,
            dialogId,
            state.name,
            state.text,
            state.type.ordinal
        ))
    }

    suspend fun getState(id: Long): StateData? {
        val state = dao.getStateWithTransitions(id) ?: return null
        return StateData(
            state.state.id, state.state.name, state.state.text,
            StateType.fromInt(state.state.type)!!,
            state.transitions.map { transition ->
                TransitionData(
                    transition.id, transition.answer,
                    dao.getState(transition.toState)?.let {
                        StateData(it.id, it.name, it.text, StateType.fromInt(it.type)!!)
                    }
                )
            }
        )
    }

    fun getDialogStatesFlow(dialogId: Long): Flow<List<StateData>> {
        return dao.getStatesWithTransitionsFlow(dialogId).map {
            it.map { state ->
                StateData(
                    state.state.id, state.state.name, state.state.text,
                    StateType.fromInt(state.state.type)!!,
                    state.transitions.map {
                        TransitionData(
                            it.id, it.answer,
                            dao.getState(it.toState)?.let {
                                StateData(it.id, it.name, it.text, StateType.fromInt(it.type)!!)
                            }
                        )
                    }
                )
            }
        }
    }

    suspend fun updateState(state: StateData, dialogId: Long) {
        dao.updateState(StateEntity(
            state.id,
            dialogId,
            state.name,
            state.text,
            state.type.ordinal
        ))
        dao.deleteTransitionsFor(state.id)
        state.answers.forEach {
            dao.insertTransition(TransitionEntity(0, state.id, it.toState?.id, it.answer))
        }
    }

    suspend fun setStartingState(state: StateData, dialogId: Long) {
        val dialog = dao.getDialog(dialogId) ?: throw Exception("Dialog not found")
        dao.updateDialog(dialog.copy(startState = state.id))
    }

    suspend fun setFinishState(state: StateData, dialogId: Long) {
        val dialog = dao.getDialog(dialogId) ?: throw Exception("Dialog not found")
        dao.updateDialog(dialog.copy(endState = state.id))
    }
}