package com.example.geobeacon

import android.app.Application
import com.example.geobeacon.data.db.AppDatabase
import com.example.geobeacon.data.db.AppRepository

class GeoBeaconApp : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { AppRepository(database.chatDao()) }
}