package com.ryosoftware.battery_tile.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DischargeSessionDao {
    @Query("SELECT * FROM discharge_sessions ORDER BY startTime DESC")
    fun getAll(): Flow<List<DischargeSession>>

    @Query("SELECT * FROM discharge_sessions WHERE startTime >= :startTime ORDER BY startTime DESC")
    fun getFromTime(startTime: Long): Flow<List<DischargeSession>>

    @Query("SELECT * FROM discharge_sessions ORDER BY startTime DESC LIMIT 1")
    suspend fun getLatest(): DischargeSession?

    @Query("SELECT * FROM discharge_sessions WHERE endTime IS NULL ORDER BY startTime DESC LIMIT 1")
    suspend fun getOpenSession(): DischargeSession?

    @Insert
    suspend fun insert(session: DischargeSession): Long

    @Update
    suspend fun update(session: DischargeSession)

    @Query("DELETE FROM discharge_sessions WHERE startTime < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long)

    @Query("DELETE FROM discharge_sessions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM discharge_sessions")
    suspend fun deleteAll()
}
