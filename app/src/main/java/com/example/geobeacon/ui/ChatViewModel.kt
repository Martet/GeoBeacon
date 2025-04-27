package com.example.geobeacon.ui

import android.util.Log
import androidx.compose.foundation.layout.size
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.geobeacon.data.AnswerStatus
import com.example.geobeacon.data.MessageAnswer
import com.example.geobeacon.data.db.AppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.example.geobeacon.data.MessageData

class ChatViewModel(private val repository: AppRepository) : ViewModel() {
    private val _messages = MutableStateFlow<List<MessageData>>(emptyList())
    val messages: StateFlow<List<MessageData>> = _messages.asStateFlow()
    private val _address = MutableStateFlow("")
    val address: StateFlow<String> = _address.asStateFlow()

    init {
        loadMessages()
    }

    fun setAddress(address: String) {
        _address.value = address
        loadMessages()
    }

    fun addMessage(message: MessageData) {
        viewModelScope.launch {
            try {
                if (_messages.value.lastOrNull()?.question != message.question) {
                    val newId = repository.insertMessage(message, _address.value)
                    loadMessages()
                }
            } catch (e: Exception) {
                Log.e("GeoBeacon", "Failed to add message", e)
            }
        }
    }

    fun addAnswer(answer: String) {
        viewModelScope.launch {
            try {
                val lastMessage = _messages.value.lastOrNull()
                if (lastMessage != null) {
                    val newAnswer = MessageAnswer(answer, AnswerStatus.ANSWER_PENDING)
                    repository.insertAnswer(newAnswer, lastMessage.id)
                    loadMessages()
                }
            } catch (e: Exception) {
                Log.e("GeoBeacon", "Failed to add answer", e)
            }
        }
    }

    fun updateLastAnswer(newState: AnswerStatus) {
        viewModelScope.launch {
            try {
                Log.d("GeoBeacon", "Updating last answer to $newState")
                val lastMessage = _messages.value.lastOrNull()
                if (lastMessage != null) {

                    repository.updateAnswer(
                        MessageAnswer(
                            lastMessage.answers.last().text,
                            newState,
                            lastMessage.answers.last().id
                        ),
                        lastMessage.id
                    )
                    loadMessages()
                }
            } catch (e: Exception) {
                Log.e("GeoBeacon", "Failed to update last answer", e)
            }
        }
    }

    private fun loadMessages() {
        viewModelScope.launch {
            try {
                val loadedMessages = repository.getLastConversation(_address.value)
                _messages.value = loadedMessages
                Log.d("GeoBeacon", "Loaded ${loadedMessages.size} messages")

            } catch (e: Exception) {
                Log.e("GeoBeacon", "Failed to load messages", e)
            }
        }
    }


    companion object {
        fun Factory(repository: AppRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                ChatViewModel(repository)
            }
        }
    }
}