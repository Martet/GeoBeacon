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

    init {
        loadConversations()
    }

    private fun loadConversations() {
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


    companion object {
        fun Factory(repository: AppRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                HistoryViewModel(repository)
            }
        }
    }
}