package com.xzmitk01.geobeacon.data.db

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.Relation

/*
    Chat history
 */
@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Long = 0,
    @ColumnInfo(name = "address") val address: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "timestamp") val timestamp: Long,
    @ColumnInfo(name = "finished") val finished: Boolean,
)

@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversation_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Long = 0,
    @ColumnInfo(name = "conversation_id", index = true) val conversationId: Long,
    @ColumnInfo(name = "text") val text: String,
    @ColumnInfo(name = "timestamp") val timestamp: Long,
    @ColumnInfo(name = "is_closed_question") val isClosedQuestion: Boolean = false
)

@Entity(
    tableName = "answers",
    foreignKeys = [
        ForeignKey(
            entity = MessageEntity::class,
            parentColumns = ["id"],
            childColumns = ["message_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class AnswerEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Long = 0,
    @ColumnInfo(name = "message_id", index = true) val messageId: Long,
    @ColumnInfo(name = "text") val text: String,
    @ColumnInfo(name = "status") val status: Int,
)

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

/*
    Settings
 */
@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey val id: Int = 0,
    val respectSystemTheme: Boolean = true,
    val darkMode: Boolean = false,
    val devMode: Boolean = false,
)

/*
    Dialog editor
 */
@Entity(
    tableName = "dialogs",
    foreignKeys = [
        ForeignKey(
            entity = StateEntity::class,
            parentColumns = ["state_id"],
            childColumns = ["start_state"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = StateEntity::class,
            parentColumns = ["state_id"],
            childColumns = ["end_state"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class DialogEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "dialog_id") val id: Long = 0,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "timestamp") val timestamp: Long,
    @ColumnInfo(name = "start_state", index = true) val startState: Long?,
    @ColumnInfo(name = "end_state", index = true) val endState: Long?,
    @ColumnInfo(name = "validation_status") val validationStatus: Int = 3
)

@Entity(
    tableName = "states",
    foreignKeys = [
        ForeignKey(
            entity = DialogEntity::class,
            parentColumns = ["dialog_id"],
            childColumns = ["owner_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class StateEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "state_id") val id: Long = 0,
    @ColumnInfo(name = "owner_id", index = true) val dialogId: Long,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "text") val text: String,
    @ColumnInfo(name = "type") val type: Int,
)

@Entity(
    tableName = "transitions",
    foreignKeys = [
        ForeignKey(
            entity = StateEntity::class,
            parentColumns = ["state_id"],
            childColumns = ["from_state"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = StateEntity::class,
            parentColumns = ["state_id"],
            childColumns = ["to_state"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class TransitionEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "transition_id") val id: Long = 0,
    @ColumnInfo(name = "from_state", index = true) val fromState: Long?,
    @ColumnInfo(name = "to_state", index = true) val toState: Long?,
    @ColumnInfo(name = "answer") val answer: String,
)

data class StateWithTransitions(
    @Embedded val state: StateEntity,
    @Relation(
        entity = TransitionEntity::class,
        parentColumn = "state_id",
        entityColumn = "from_state"
    )
    val transitions: List<TransitionEntity>
)