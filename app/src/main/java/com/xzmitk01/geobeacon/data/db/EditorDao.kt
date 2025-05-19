package com.xzmitk01.geobeacon.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface EditorDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDialog(dialog: DialogEntity): Long

    @Query("SELECT * FROM dialogs")
    fun getDialogsFlow(): Flow<List<DialogEntity>>

    @Query("SELECT * FROM dialogs WHERE dialog_id = :id")
    suspend fun getDialog(id: Long): DialogEntity?

    @Query("SELECT * FROM dialogs WHERE dialog_id = :id")
    fun getDialogFlow(id: Long): Flow<DialogEntity?>

    @Update(entity = DialogEntity::class)
    suspend fun updateDialog(dialog: DialogEntity)

    @Query("DELETE FROM dialogs WHERE dialog_id = :id")
    suspend fun deleteDialog(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertState(state: StateEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransition(transition: TransitionEntity): Long

    @Query("SELECT * FROM states WHERE state_id = :id")
    suspend fun getState(id: Long?): StateEntity?

    @Transaction
    @Query("SELECT * FROM states WHERE state_id = :id")
    suspend fun getStateWithTransitions(id: Long): StateWithTransitions?

    @Transaction
    @Query("SELECT * FROM states WHERE owner_id = :dialogId")
    suspend fun getStatesWithTransitions(dialogId: Long): List<StateWithTransitions>

    @Transaction
    @Query("SELECT * FROM states WHERE owner_id = :dialogId")
    fun getStatesWithTransitionsFlow(dialogId: Long): Flow<List<StateWithTransitions>>

    @Query("DELETE FROM states WHERE state_id = :id")
    suspend fun deleteState(id: Long)

    @Update(entity = StateEntity::class)
    suspend fun updateState(state: StateEntity)

    @Query("DELETE FROM transitions WHERE from_state = :stateId")
    suspend fun deleteTransitionsFor(stateId: Long)
}