package com.example.geobeacon.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.geobeacon.data.AnswerStatus
import com.example.geobeacon.data.ConversationData
import com.example.geobeacon.data.MessageAnswer
import com.example.geobeacon.data.MessageData
import com.example.geobeacon.data.db.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ChatViewModel(private val repository: ChatRepository) : ViewModel() {
    private val _conversation = MutableStateFlow<ConversationData>(ConversationData())
    val conversation: StateFlow<ConversationData> = _conversation.asStateFlow()
    private var _lastQuestion = MutableStateFlow<MessageData>(MessageData())

    private val _mutex = Mutex()
    private var _address = ""
    private var _lastAnswerNum = 0


    init {
        loadMessages()
    }

    fun setAddressName(address: String, name: String) {
        _address = address
        _conversation.value = _conversation.value.copy(name = name)
        loadMessages()
        if (_lastQuestion.value.closedQuestion) {
            for (answer in _lastQuestion.value.answers) {
                if (answer.status == AnswerStatus.ANSWER_PENDING) {
                    viewModelScope.launch {
                        _mutex.withLock {
                            repository.updateAnswer(
                                answer.copy(status = AnswerStatus.ANSWER_UNANSWERED),
                                _lastQuestion.value.id
                            )
                        }
                    }
                }
            }
        }
    }

    fun addMessage(message: String) {
        viewModelScope.launch {
            _mutex.withLock {
                try {
                    if (_conversation.value.messages.lastOrNull()?.question != message) {
                        val splitQuestion = message.split("\n")
                        val isClosedQuestion = splitQuestion.count() > 1
                        val regex = Regex("\\d\\) ")
                        val answers = if (isClosedQuestion) {
                            splitQuestion.subList(1, splitQuestion.size).map { answer ->
                                MessageAnswer(answer.split(regex)[1], AnswerStatus.ANSWER_UNANSWERED)
                            }
                        } else {
                            emptyList()
                        }
                        val messageData = MessageData(
                            question = message,
                            closedQuestion = isClosedQuestion,
                            answers = answers
                        )
                        repository.insertMessage(messageData, _address)
                        if (messageData.isQuestion()) {
                            _lastQuestion.value = messageData
                        }
                        loadMessages()
                    }
                } catch (e: Exception) {
                    Log.e("GeoBeacon", "Failed to add message", e)
                }
            }
        }
    }

    fun addAnswer(answer: String) {
        viewModelScope.launch {
            _mutex.withLock {
                try {
                    val newAnswer = MessageAnswer(answer, AnswerStatus.ANSWER_PENDING)
                    repository.insertAnswer(newAnswer, _lastQuestion.value.id)
                    loadMessages()
                } catch (e: Exception) {
                    Log.e("GeoBeacon", "Failed to add answer", e)
                }
            }
        }
    }

    fun updateLastAnswer(newState: AnswerStatus, answerNum: Int = -1) {
        viewModelScope.launch {
            _mutex.withLock {
                try {
                    Log.d("GeoBeacon", "Updating last answer to $newState")
                    if (answerNum > -1) {
                        _lastAnswerNum = answerNum
                    }
                    val toUpdate = if (_lastQuestion.value.closedQuestion) {
                        _lastQuestion.value.answers[_lastAnswerNum]
                    } else {
                        _lastQuestion.value.answers.last()
                    }
                    repository.updateAnswer(
                        toUpdate.copy(status = newState),
                        _lastQuestion.value.id
                    )
                    loadMessages()
                } catch (e: Exception) {
                    Log.e("GeoBeacon", "Failed to update last answer", e)
                }
            }
        }
    }

    fun finishConversation() {
        viewModelScope.launch {
            _mutex.withLock {
                try {
                    _conversation.value = _conversation.value.copy(finished = true)
                    repository.finishConversation(_conversation.value.id)
                } catch (e: Exception) {
                    Log.e("GeoBeacon", "Failed to finish conversation", e)
                }
            }
        }
    }

    fun resetConversation() {
        viewModelScope.launch {
            _address = ""
            _conversation.value = ConversationData()
            _lastQuestion.value = MessageData()
            Log.d("GeoBeacon", "Resetting conversation")
        }
    }

    private fun loadMessages() {
        viewModelScope.launch {
            _mutex.withLock {
                try {
                    val loadedConversation = if (!_conversation.value.finished) {
                        repository.getLastConversation(_address, _conversation.value.name)
                    } else {
                        repository.getConversation(_conversation.value.id)
                    }
                    _conversation.value = loadedConversation
                    Log.d("GeoBeacon", "Loaded ${loadedConversation.messages.size} messages")

                    val lastQuestion = loadedConversation.getLastQuestion()
                    if (lastQuestion != null) {
                        _lastQuestion.value = lastQuestion
                    }
                } catch (e: Exception) {
                    Log.e("GeoBeacon", "Failed to load messages", e)
                }
            }
        }
    }


    companion object {
        fun Factory(repository: ChatRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                ChatViewModel(repository)
            }
        }
    }
}