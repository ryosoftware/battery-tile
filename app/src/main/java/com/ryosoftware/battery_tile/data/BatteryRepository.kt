package com.ryosoftware.battery_tile.data

import android.content.Context
import kotlinx.coroutines.flow.Flow

class BatteryRepository(context: Context) {
    private val batteryReadingDao = BatteryDatabase.getDatabase(context).batteryReadingDao()
    private val chargingSessionDao = BatteryDatabase.getDatabase(context).chargingSessionDao()
    private val screenStateDao = BatteryDatabase.getDatabase(context).screenStateDao()
    private val dischargeSessionDao = BatteryDatabase.getDatabase(context).dischargeSessionDao()

    fun getAllBatteryReadings(): Flow<List<BatteryReading>> = batteryReadingDao.getAll()
    suspend fun insertBatteryReading(reading: BatteryReading) = batteryReadingDao.insert(reading)
    suspend fun deleteBatteryReadingsOlderThan(timestamp: Long) = batteryReadingDao.deleteOlderThan(timestamp)
    suspend fun getBatteryReadingsBetween(start: Long, end: Long): List<BatteryReading> =
        batteryReadingDao.getBetween(start, end)
    suspend fun deleteAllBatteryReadings() = batteryReadingDao.deleteAll()

    fun getAllChargingSessions(): Flow<List<ChargingSession>> = chargingSessionDao.getAll()
    suspend fun getOpenChargingSession(): ChargingSession? = chargingSessionDao.getOpenSession()
    suspend fun insertChargingSession(session: ChargingSession): Long =
        chargingSessionDao.insert(session)
    suspend fun updateChargingSession(session: ChargingSession) = chargingSessionDao.update(session)
    suspend fun deleteChargingSessionsOlderThan(timestamp: Long) =
        chargingSessionDao.deleteOlderThan(timestamp)
    suspend fun deleteChargingSession(id: Long) = chargingSessionDao.deleteById(id)
    suspend fun deleteAllChargingSessions() = chargingSessionDao.deleteAll()

    fun getAllScreenStates(): Flow<List<ScreenState>> = screenStateDao.getAll()
    suspend fun insertScreenState(state: ScreenState) = screenStateDao.insert(state)
    suspend fun deleteScreenStatesOlderThan(timestamp: Long) = screenStateDao.deleteOlderThan(timestamp)
    suspend fun deleteAllScreenStates() = screenStateDao.deleteAll()
    suspend fun getScreenStatesBetween(start: Long, end: Long): List<ScreenState> =
        screenStateDao.getBetween(start, end)
    suspend fun getLatestScreenStateBefore(timestamp: Long): ScreenState? =
        screenStateDao.getLatestBefore(timestamp)

    fun getAllDischargeSessions(): Flow<List<DischargeSession>> = dischargeSessionDao.getAll()
    suspend fun getOpenDischargeSession(): DischargeSession? = dischargeSessionDao.getOpenSession()
    suspend fun insertDischargeSession(session: DischargeSession): Long =
        dischargeSessionDao.insert(session)
    suspend fun updateDischargeSession(session: DischargeSession) = dischargeSessionDao.update(session)
    suspend fun deleteDischargeSessionsOlderThan(timestamp: Long) =
        dischargeSessionDao.deleteOlderThan(timestamp)
    suspend fun deleteDischargeSession(id: Long) = dischargeSessionDao.deleteById(id)
    suspend fun deleteAllDischargeSessions() = dischargeSessionDao.deleteAll()

    suspend fun deleteAll() {
        deleteAllBatteryReadings()
        deleteAllChargingSessions()
        deleteAllScreenStates()
        deleteAllChargingSessions()
        deleteAllDischargeSessions()
    }

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
