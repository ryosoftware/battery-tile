package com.ryosoftware.battery_tile.data

import android.content.Context
import com.ryosoftware.battery_tile.Main
import kotlinx.coroutines.flow.Flow

class BatteryRepository(context: Context) {
    private val batteryReadingDao = BatteryDatabase.getDatabase(context).batteryReadingDao()
    private val chargingSessionDao = BatteryDatabase.getDatabase(context).chargingSessionDao()

    fun getAll(): Flow<List<BatteryReading>> = batteryReadingDao.getAll()

    fun getFromTime(timestamp: Long): Flow<List<BatteryReading>> =
        batteryReadingDao.getFromTime(timestamp)

    suspend fun getLatest(): BatteryReading? = batteryReadingDao.getLatest()

    suspend fun insert(reading: BatteryReading) = batteryReadingDao.insert(reading)

    suspend fun deleteOlderThan(timestamp: Long) = batteryReadingDao.deleteOlderThan(timestamp)

    suspend fun getReadingsBetween(start: Long, end: Long): List<BatteryReading> =
        batteryReadingDao.getBetween(start, end)

    suspend fun deleteAll() = batteryReadingDao.deleteAll()

    fun getAllChargingSessions(): Flow<List<ChargingSession>> = chargingSessionDao.getAll()

    fun getChargingSessionsFromTime(startTime: Long): Flow<List<ChargingSession>> =
        chargingSessionDao.getFromTime(startTime)

    suspend fun getOpenChargingSession(): ChargingSession? = chargingSessionDao.getOpenSession()

    suspend fun insertChargingSession(session: ChargingSession): Long =
        chargingSessionDao.insert(session)

    suspend fun updateChargingSession(session: ChargingSession) = chargingSessionDao.update(session)

    suspend fun deleteOlderChargingSessions(timestamp: Long) =
        chargingSessionDao.deleteOlderThan(timestamp)

    suspend fun deleteChargingSession(id: Long) = chargingSessionDao.deleteById(id)

    suspend fun deleteAllChargingSessions() = chargingSessionDao.deleteAll()

    companion object {
        @Volatile
        private var INSTANCE: BatteryRepository? = null

        fun getInstance(context: Context): BatteryRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = BatteryRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
