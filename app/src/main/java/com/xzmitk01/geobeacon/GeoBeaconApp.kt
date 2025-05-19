package com.xzmitk01.geobeacon

import android.app.Application
import com.xzmitk01.geobeacon.data.BluetoothConnectionManager
import com.xzmitk01.geobeacon.data.db.AppDatabase
import com.xzmitk01.geobeacon.data.db.ChatRepository
import com.xzmitk01.geobeacon.data.db.EditorRepository
import com.xzmitk01.geobeacon.data.db.SettingsRepository

class GeoBeaconApp : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val chatRepository by lazy { ChatRepository(database.chatDao()) }
    val settingsRepository by lazy { SettingsRepository(database.settingsDao()) }
    val editorRepository by lazy { EditorRepository(database.editorDao()) }
    val bluetoothManager by lazy { BluetoothConnectionManager(this) }
}