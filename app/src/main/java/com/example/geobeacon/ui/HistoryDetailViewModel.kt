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

class HistoryDetailViewModel(private val repository: AppRepository, private val conversationId: Long) : ViewModel() {
    private val _conversation = MutableStateFlow<ConversationData>(ConversationData())
    val conversation: StateFlow<ConversationData> = _conversation.asStateFlow()

    init {
        loadConversation(conversationId)
    }

    private fun loadConversation(conversationId: Long) {
        viewModelScope.launch {
            try {
                val loadedConversation = repository.getConversation(conversationId)
                _conversation.value = loadedConversation
            } catch (e: Exception) {
                Log.e("GeoBeacon", "Failed to load conversation", e)
            }
        }
    }

    fun deleteConversation() {
        viewModelScope.launch {
            repository.deleteConversation(conversationId)
        }
    }

    companion object {
        fun Factory(repository: AppRepository, conversationId: Long): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                HistoryDetailViewModel(repository, conversationId)
            }
        }
    }
}