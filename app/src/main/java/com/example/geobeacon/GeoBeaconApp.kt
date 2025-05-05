package com.example.geobeacon

import android.app.Application
import com.example.geobeacon.data.BluetoothConnectionManager
import com.example.geobeacon.data.db.AppDatabase
import com.example.geobeacon.data.db.ChatRepository
import com.example.geobeacon.data.db.EditorRepository
import com.example.geobeacon.data.db.SettingsRepository

class GeoBeaconApp : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val chatRepository by lazy { ChatRepository(database.chatDao()) }
    val settingsRepository by lazy { SettingsRepository(database.settingsDao()) }
    val editorRepository by lazy { EditorRepository(database.editorDao()) }
    val bluetoothManager by lazy { BluetoothConnectionManager(this) }
}