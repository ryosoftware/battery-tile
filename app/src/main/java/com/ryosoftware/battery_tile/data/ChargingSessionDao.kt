package com.ryosoftware.battery_tile.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.descriptors.PrimitiveKind

@Dao
interface ChargingSessionDao {
    @Query("SELECT * FROM charging_sessions ORDER BY startTime DESC")
    fun getAll(): Flow<List<ChargingSession>>

    @Query("SELECT * FROM charging_sessions WHERE startTime >= :startTime ORDER BY startTime DESC")
    fun getFromTime(startTime: Long): Flow<List<ChargingSession>>

    @Query("SELECT * FROM charging_sessions ORDER BY startTime DESC LIMIT 1")
    suspend fun getLatest(): ChargingSession?

    @Query("SELECT * FROM charging_sessions WHERE endTime IS NULL ORDER BY startTime DESC LIMIT 1")
    suspend fun getOpenSession(): ChargingSession?

    @Insert
    suspend fun insert(session: ChargingSession): Long

    @Update
    suspend fun update(session: ChargingSession)

    @Query("DELETE FROM charging_sessions WHERE startTime < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long)

    @Query("DELETE FROM charging_sessions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM charging_sessions")
    suspend fun deleteAll()
}
