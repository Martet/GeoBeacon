package com.example.geobeacon.data.db

import com.example.geobeacon.data.AnswerStatus
import com.example.geobeacon.data.ConversationData
import com.example.geobeacon.data.MessageAnswer
import com.example.geobeacon.data.MessageData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date

class AppRepository(private val dao: ChatDao) {
    suspend fun getLastConversation(address: String, name: String): List<MessageData> = withContext(Dispatchers.IO) {
        val conversation = dao.getLastActiveConversation(address)
        if (conversation == null) {
            if (address != "")
                dao.insertConversation(
                    ConversationEntity(
                        address = address,
                        timestamp = System.currentTimeMillis(),
                        finished = false,
                        name = name
                    )
                )
            return@withContext emptyList()
        }
        return@withContext conversation.messagesWithAnswers.map { messageWithAnswers ->
            MessageData(
                question = messageWithAnswers.message.text,
                answers = messageWithAnswers.answers.map { answerEntity ->
                    MessageAnswer(
                        text = answerEntity.text,
                        status = AnswerStatus.fromInt(answerEntity.status)!!,
                        id = answerEntity.id
                    )
                },
                id = messageWithAnswers.message.id,
                last = false
            )
        }.also { messages ->
            if (!conversation.conversation.finished) {
                messages.last().last = true
            }
        }
    }

    suspend fun getConversations(): List<ConversationData> = withContext(Dispatchers.IO) {
        return@withContext dao.getConversations().map { conversationEntity ->
            ConversationData(
                name = conversationEntity.name,
                date = Date(conversationEntity.timestamp),
                finished = conversationEntity.finished,
                id = conversationEntity.id
            )
        }
    }

    suspend fun getConversation(id: Long): ConversationData = withContext(Dispatchers.IO) {
        val conversation = dao.getConversation(id)
        if (conversation == null) {
            throw Exception("Conversation not found")
        }
        return@withContext ConversationData(
            name = conversation.conversation.name,
            date = Date(conversation.conversation.timestamp),
            finished = conversation.conversation.finished,
            id = conversation.conversation.id,
            messages = conversation.messagesWithAnswers.map { messageWithAnswers ->
                MessageData(
                    question = messageWithAnswers.message.text,
                    answers = messageWithAnswers.answers.map { answerEntity ->
                        MessageAnswer(
                            text = answerEntity.text,
                            status = AnswerStatus.fromInt(answerEntity.status)!!,
                            id = answerEntity.id
                        )
                    },
                    id = messageWithAnswers.message.id,
                    last = false
                )
            }
        )
    }

    suspend fun insertMessage(message: MessageData, address: String) = withContext(Dispatchers.IO) {
        val conversation = dao.getLastActiveConversation(address)
        val conversationId = conversation?.conversation?.id ?: throw Exception("Insert message failed: Conversation not found")

        val messageId = dao.insertChatMessage(MessageEntity(conversationId = conversationId, text = message.question, timestamp = System.currentTimeMillis()))
        message.answers.forEach { answer ->
            dao.insertAnswer(AnswerEntity(messageId = messageId, text = answer.text, status = answer.status.value))
        }
    }

    suspend fun insertAnswer(answer: MessageAnswer, messageId: Long) = withContext(Dispatchers.IO) {
        dao.insertAnswer(AnswerEntity(messageId = messageId, text = answer.text, status = answer.status.value))
    }

    suspend fun updateAnswer(answer: MessageAnswer, messageId: Long) = withContext(Dispatchers.IO) {
        dao.updateAnswer(AnswerEntity(messageId = messageId, id = answer.id, text = answer.text, status = answer.status.value))
    }

    suspend fun finishConversation(address: String) = withContext(Dispatchers.IO) {
        val conversation = dao.getLastActiveConversation(address)
        if (conversation != null) {
            dao.setConversationFinished(conversation.conversation.id)
        }
    }

    suspend fun deleteConversation(conversationId: Long) = withContext(Dispatchers.IO) {
        val conversation = dao.getConversation(conversationId)
        if (conversation != null) {
            dao.deleteConversation(conversation.conversation)
        }
    }
}