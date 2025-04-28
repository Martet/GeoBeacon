package com.example.geobeacon.ui

import android.util.Log
import androidx.compose.foundation.layout.size
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.geobeacon.data.AnswerStatus
import com.example.geobeacon.data.ConversationData
import com.example.geobeacon.data.MessageAnswer
import com.example.geobeacon.data.db.AppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.example.geobeacon.data.MessageData

class HistoryViewModel(private val repository: AppRepository) : ViewModel() {
    private val _conversations = MutableStateFlow<List<ConversationData>>(emptyList())
    val conversations: StateFlow<List<ConversationData>> = _conversations.asStateFlow()
    private val _conversation = MutableStateFlow<ConversationData?>(null)
    val conversation: StateFlow<ConversationData?> = _conversation.asStateFlow()

    init {
        loadConversations()
    }

    fun loadConversations() {
        viewModelScope.launch {
            try {
                val loadedConversations = repository.getConversations()
                _conversations.value = loadedConversations
                Log.d("GeoBeacon", "Loaded ${loadedConversations.size} conversations")
            } catch (e: Exception) {
                Log.e("GeoBeacon", "Failed to load conversations", e)
            }
        }
    }

    fun loadConversation(conversationId: Long) {
        viewModelScope.launch {
            try {
                val loadedConversation = repository.getConversation(conversationId)
                _conversation.value = loadedConversation
            } catch (e: Exception) {
                Log.e("GeoBeacon", "Failed to load conversation", e)
            }
        }
    }

    fun setConversation(conversation: ConversationData?) {
        _conversation.value = conversation
    }

    fun deleteConversation(conversationId: Long) {
        viewModelScope.launch {
            repository.deleteConversation(conversationId)
            _conversation.value = null
            loadConversations()
        }
    }

    companion object {
        fun Factory(repository: AppRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                HistoryViewModel(repository)
            }
        }
    }
}