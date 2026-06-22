package com.ryosoftware.battery_tile.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BatteryReadingDao {
    @Query("SELECT * FROM battery_readings ORDER BY timestamp DESC")
    fun getAll(): Flow<List<BatteryReading>>

    @Query("SELECT * FROM battery_readings WHERE timestamp >= :timestamp ORDER BY timestamp DESC")
    fun getFromTime(timestamp: Long): Flow<List<BatteryReading>>

    @Query("SELECT * FROM battery_readings ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatest(): BatteryReading?

    @Insert
    suspend fun insert(reading: BatteryReading)

    @Query("DELETE FROM battery_readings WHERE timestamp < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long)

    @Query("DELETE FROM battery_readings")
    suspend fun deleteAll()
}
