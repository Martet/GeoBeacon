package com.example.geobeacon.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
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

    @Query("SELECT * FROM conversations WHERE id = :id")
    suspend fun getConversation(id: Long): ConversationWithMessagesAndAnswers?

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

    @Update(onConflict = OnConflictStrategy.REPLACE, entity = AnswerEntity::class)
    suspend fun updateAnswer(answer: AnswerEntity)

    @Query("UPDATE conversations SET finished = 1 WHERE id = :id")
    suspend fun setConversationFinished(id: Long)
}

data class ConversationWithMessagesAndAnswers(
    @Embedded val conversation: ConversationEntity,
    @Relation(
        entity = MessageEntity::class,
        parentColumn = "id",
        entityColumn = "conversation_id"
    )
    val messagesWithAnswers: List<MessageWithAnswers>
)

data class MessageWithAnswers(
    @Embedded val message: MessageEntity,
    @Relation(
        entity = AnswerEntity::class,
        parentColumn = "id",
        entityColumn = "message_id"
    )
    val answers: List<AnswerEntity>
)