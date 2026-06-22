package com.ryosoftware.battery_tile.data

import android.content.Context
import com.ryosoftware.battery_tile.Main
import kotlinx.coroutines.flow.Flow

class BatteryRepository(context: Context) {
    private val batteryReadingDao = BatteryDatabase.getDatabase(context).batteryReadingDao()

    fun getAll(): Flow<List<BatteryReading>> = batteryReadingDao.getAll()

    fun getFromTime(timestamp: Long): Flow<List<BatteryReading>> =
        batteryReadingDao.getFromTime(timestamp)

    suspend fun getLatest(): BatteryReading? = batteryReadingDao.getLatest()

    suspend fun insert(reading: BatteryReading) = batteryReadingDao.insert(reading)

    suspend fun deleteOlderThan(timestamp: Long) = batteryReadingDao.deleteOlderThan(timestamp)

    suspend fun deleteAll() = batteryReadingDao.deleteAll()

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
