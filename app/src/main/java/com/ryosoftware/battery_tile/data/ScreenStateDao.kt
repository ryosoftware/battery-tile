package com.ryosoftware.battery_tile.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScreenStateDao {
    @Query("SELECT * FROM screen_states ORDER BY timestamp ASC")
    fun getAll(): Flow<List<ScreenState>>

    @Query("SELECT * FROM screen_states WHERE timestamp >= :startTime ORDER BY timestamp ASC")
    fun getFromTime(startTime: Long): Flow<List<ScreenState>>

    @Query("SELECT * FROM screen_states ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatest(): ScreenState?

    @Query("SELECT * FROM screen_states WHERE timestamp >= :start AND timestamp <= :end ORDER BY timestamp ASC")
    suspend fun getBetween(start: Long, end: Long): List<ScreenState>

    @Query("SELECT * FROM screen_states WHERE timestamp <= :timestamp ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestBefore(timestamp: Long): ScreenState?

    @Insert
    suspend fun insert(state: ScreenState): Long

    @Query("DELETE FROM screen_states WHERE timestamp < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long)

    @Query("DELETE FROM screen_states")
    suspend fun deleteAll()
}
