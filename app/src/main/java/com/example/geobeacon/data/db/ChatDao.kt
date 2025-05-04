package com.example.geobeacon.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Transaction
    @Query("SELECT * FROM conversations WHERE address = :address AND finished = 0 ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastActiveConversation(address: String): ConversationWithMessagesAndAnswers?

    @Query("SELECT * FROM conversations")
    fun getConversationsFlow(): Flow<List<ConversationEntity>>

    @Transaction
    @Query("SELECT * FROM conversations WHERE id = :id")
    suspend fun getConversation(id: Long): ConversationWithMessagesAndAnswers?

    @Transaction
    @Query("SELECT * FROM conversations WHERE id = :id")
    fun getConversationFlow(id: Long): Flow<ConversationWithMessagesAndAnswers?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: ConversationEntity): Long

    @Delete
    suspend fun deleteConversation(conversation: ConversationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessage(message: MessageEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnswer(answer: AnswerEntity): Long

    @Query("SELECT * FROM answers WHERE id = :id")
    suspend fun getAnswer(id: Long): AnswerEntity?

    @Update(onConflict = OnConflictStrategy.REPLACE, entity = AnswerEntity::class)
    suspend fun updateAnswer(answer: AnswerEntity)

    @Query("UPDATE conversations SET finished = 1 WHERE id = :id")
    suspend fun setConversationFinished(id: Long)
}