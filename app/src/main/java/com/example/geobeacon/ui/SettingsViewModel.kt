package com.example.geobeacon.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.geobeacon.data.db.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {
    val settings = repository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        viewModelScope.launch {
            repository.ensureDefaultSettings()
        }
    }

    fun switchDevMode(toMode: Boolean) {
        viewModelScope.launch {
            repository.updateDevMode(toMode)
        }
    }

    fun switchRespectSystemTheme(toMode: Boolean) {
        viewModelScope.launch {
            repository.updateRespectSystemTheme(toMode)
        }
    }

    fun switchDarkMode(toMode: Boolean) {
        viewModelScope.launch {
            repository.updateDarkMode(toMode)
        }
    }

    companion object {
        fun Factory(repository: SettingsRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                SettingsViewModel(repository)
            }
        }
    }
}