package com.example.geobeacon.data.db

import com.example.geobeacon.data.MessageData
import com.example.geobeacon.data.MessageAnswer
import com.example.geobeacon.data.AnswerStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppRepository(private val dao: ChatDao) {
    suspend fun getLastConversation(address: String): List<MessageData> = withContext(Dispatchers.IO) {
        val conversation = dao.getLastConversation(address)
        if (conversation == null) {
            if (address != "")
                dao.insertConversation(ConversationEntity(address = address, timestamp = System.currentTimeMillis()))
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
                id = messageWithAnswers.message.id
            )
        }
    }

    suspend fun insertMessage(message: MessageData, address: String) = withContext(Dispatchers.IO) {
        val conversation = dao.getLastConversation(address)
        val conversationId = conversation?.conversation?.id ?: dao.insertConversation(ConversationEntity(address = address, timestamp = System.currentTimeMillis()))

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
}