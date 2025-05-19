package com.xzmitk01.geobeacon.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.xzmitk01.geobeacon.data.ConversationData
import com.xzmitk01.geobeacon.data.db.ChatRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(private val repository: ChatRepository) : ViewModel() {
    val conversations = repository.conversationsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf())

    private val _conversation = MutableStateFlow<ConversationData?>(null)
    private val _conversationState: StateFlow<ConversationData?> = _conversation.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    var conversation: StateFlow<ConversationData?> = _conversationState
        .flatMapLatest { repository.getConversationFlow(it?.id ?: -1) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun setConversation(newConversation: ConversationData?) {
        _conversation.value = newConversation
    }

    fun deleteConversation(conversationId: Long) {
        viewModelScope.launch {
            _conversation.value = null
            repository.deleteConversation(conversationId)
        }
    }

    companion object {
        fun Factory(repository: ChatRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                HistoryViewModel(repository)
            }
        }
    }
}