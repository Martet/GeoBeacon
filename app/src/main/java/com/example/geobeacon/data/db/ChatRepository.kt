package com.example.geobeacon.data.db

import com.example.geobeacon.data.AnswerStatus
import com.example.geobeacon.data.ConversationData
import com.example.geobeacon.data.MessageAnswer
import com.example.geobeacon.data.MessageData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date

class ChatRepository(private val dao: ChatDao) {
    private fun mapConversation(conversation: ConversationWithMessagesAndAnswers): ConversationData {
        return ConversationData(
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
                    last = !conversation.conversation.finished && messageWithAnswers.message.id == conversation.messagesWithAnswers.last().message.id,
                    closedQuestion = messageWithAnswers.message.isClosedQuestion
                )
            }
        )
    }

    suspend fun getLastConversation(address: String, name: String): ConversationData {
        val conversation = dao.getLastActiveConversation(address)
        if (conversation == null) {
            var conversationId: Long = 0
            if (address != "" && name != "")
                conversationId = dao.insertConversation(
                    ConversationEntity(
                        address = address,
                        timestamp = System.currentTimeMillis(),
                        finished = false,
                        name = name
                    )
                )
            return ConversationData(name = name, id = conversationId)
        }

        return mapConversation(conversation)
    }

    suspend fun getConversations(): List<ConversationData> {
        return dao.getConversations().map { conversationEntity ->
            ConversationData(
                name = conversationEntity.name,
                date = Date(conversationEntity.timestamp),
                finished = conversationEntity.finished,
                id = conversationEntity.id
            )
        }
    }

    suspend fun getConversation(id: Long): ConversationData {
        val conversation = dao.getConversation(id)
        if (conversation == null) {
            throw Exception("Conversation not found")
        }
        return mapConversation(conversation)
    }

    suspend fun insertMessage(message: MessageData, address: String) {
        val conversation = dao.getLastActiveConversation(address)
        val conversationId = conversation?.conversation?.id ?: throw Exception("Insert message failed: Conversation not found")

        val messageId = dao.insertChatMessage(MessageEntity(
            conversationId = conversationId,
            text = message.question,
            timestamp = System.currentTimeMillis(),
            isClosedQuestion = message.closedQuestion
        ))
        message.answers.forEach { answer ->
            dao.insertAnswer(AnswerEntity(messageId = messageId, text = answer.text, status = answer.status.value))
        }
    }

    suspend fun insertAnswer(answer: MessageAnswer, messageId: Long) {
        dao.insertAnswer(AnswerEntity(messageId = messageId, text = answer.text, status = answer.status.value))
    }

    suspend fun updateAnswer(answer: MessageAnswer, messageId: Long) {
        dao.updateAnswer(AnswerEntity(messageId = messageId, id = answer.id, text = answer.text, status = answer.status.value))
    }

    suspend fun finishConversation(id: Long) {
        val conversation = dao.getConversation(id)
        if (conversation != null) {
            dao.setConversationFinished(conversation.conversation.id)
        }
    }

    suspend fun deleteConversation(conversationId: Long) {
        val conversation = dao.getConversation(conversationId)
        if (conversation != null) {
            dao.deleteConversation(conversation.conversation)
        }
    }
}