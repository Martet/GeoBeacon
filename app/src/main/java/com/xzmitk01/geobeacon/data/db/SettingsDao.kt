package com.xzmitk01.geobeacon.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SettingsDao {
    @Query("SELECT * FROM settings WHERE id = 0")
    suspend fun getSettings(): SettingsEntity?

    @Query("SELECT * FROM settings WHERE id = 0")
    fun getSettingsFlow(): Flow<SettingsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateSettings(settings: SettingsEntity)
}