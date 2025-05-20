package com.xzmitk01.geobeacon.data.db

import com.xzmitk01.geobeacon.data.DialogData
import com.xzmitk01.geobeacon.data.StateData
import com.xzmitk01.geobeacon.data.StateType
import com.xzmitk01.geobeacon.data.TransitionData
import com.xzmitk01.geobeacon.data.ValidDialogStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Date

class EditorRepository(private val dao: EditorDao) {
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
            },
            validationStatus = ValidDialogStatus.fromInt(dialog.validationStatus)
        )
    }

    private suspend fun mapState(state: StateWithTransitions): StateData {
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

    val dialogsFlow: Flow<List<DialogData>> = dao.getDialogsFlow().map { dialogEntities ->
        dialogEntities.map { dialogEntity ->
            DialogData(
                dialogEntity.id,
                dialogEntity.name,
                Date(dialogEntity.timestamp),
                validationStatus = ValidDialogStatus.fromInt(dialogEntity.validationStatus)
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

    suspend fun insertFullDialog(dialog: DialogData) {
        val oldToNewStateIdMap = mutableMapOf<Long, Long>()

        val newDialogId = insertDialog(dialog.copy(id = 0, time = Date(), startState = null, finishState = null))

        dialog.states.forEach { state ->
            val newStateId = insertState(state.copy(id = 0), newDialogId)
            oldToNewStateIdMap[state.id] = newStateId
        }

        val newStartStateId = dialog.startState?.id?.let { oldToNewStateIdMap[it] }
        val newFinishStateId = dialog.finishState?.id?.let { oldToNewStateIdMap[it] }

        if (newStartStateId != null) {
            val startStateData = dialog.states.find { it.id == dialog.startState.id }
            if (startStateData != null) {
                setStartingState(startStateData.copy(id = newStartStateId), newDialogId)
            }
        }
        if (newFinishStateId != null) {
            val finishStateData = dialog.states.find { it.id == dialog.finishState.id }
            if (finishStateData != null) {
                setFinishState(finishStateData.copy(id = newFinishStateId), newDialogId)
            }
        }

        dialog.states.forEach { state ->
            val newFromStateId = oldToNewStateIdMap[state.id] ?: return@forEach
            state.answers.forEach {
                val newToStateId = it.toState?.id?.let { oldId -> oldToNewStateIdMap[oldId] }
                dao.insertTransition(TransitionEntity(0, newFromStateId, newToStateId, it.answer))
            }
        }
    }

    suspend fun getDialog(id: Long): DialogData? {
        val dialog = dao.getDialog(id) ?: return null
        return mapDialog(dialog)
    }

    suspend fun getDialogWithStates(id: Long): DialogData? {
        val dialog = dao.getDialog(id) ?: return null
        return  mapDialog(dialog).copy(
            states = dao.getStatesWithTransitions(id).map { mapState(it) }
        )
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
        return mapState(state)
    }

    fun getDialogStatesFlow(dialogId: Long): Flow<List<StateData>> {
        return dao.getStatesWithTransitionsFlow(dialogId).map {
            it.map { state -> mapState(state) }
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

    suspend fun setDialogValidation(status: ValidDialogStatus, dialogId: Long) {
        val dialog = dao.getDialog(dialogId) ?: throw Exception("Dialog not found")
        dao.updateDialog(dialog.copy(validationStatus = status.ordinal))
    }
}