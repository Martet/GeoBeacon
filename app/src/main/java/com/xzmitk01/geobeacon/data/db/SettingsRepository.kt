package com.xzmitk01.geobeacon.data.db

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull

class SettingsRepository(private val dao: SettingsDao) {
    val settingsFlow: Flow<SettingsEntity> = dao.getSettingsFlow()
        .filterNotNull() // Only emit non-null settings
        .distinctUntilChanged()

    suspend fun ensureDefaultSettings() {
        if (dao.getSettings() == null) {
            dao.updateSettings(SettingsEntity()) // Insert defaults
        }
    }

    suspend fun updateDevMode(isDevMode: Boolean) {
        val current = dao.getSettings() ?: SettingsEntity()
        dao.updateSettings(current.copy(devMode = isDevMode))
    }

    suspend fun updateDarkMode(isDarkMode: Boolean) {
        val current = dao.getSettings() ?: SettingsEntity()
        dao.updateSettings(current.copy(darkMode = isDarkMode))
    }

    suspend fun updateRespectSystemTheme(respectSystemTheme: Boolean) {
        val current = dao.getSettings() ?: SettingsEntity()
        dao.updateSettings(current.copy(respectSystemTheme = respectSystemTheme))
    }
}