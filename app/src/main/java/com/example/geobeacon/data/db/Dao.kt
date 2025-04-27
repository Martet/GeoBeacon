package com.example.geobeacon.data.db

import androidx.room.Dao
import androidx.room.DatabaseView
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update

@Dao
interface ChatDao {
    @Transaction
    @Query("SELECT * FROM conversations WHERE address = :address ORDER BY timestamp DESC LIMIT 1")
    fun getLastConversation(address: String): ConversationWithMessagesAndAnswers?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: ConversationEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessage(message: MessageEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnswer(answer: AnswerEntity): Long

    @Update(onConflict = OnConflictStrategy.REPLACE, entity = AnswerEntity::class)
    suspend fun updateAnswer(answer: AnswerEntity)
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