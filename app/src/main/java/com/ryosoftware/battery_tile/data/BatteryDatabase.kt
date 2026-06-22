package com.ryosoftware.battery_tile.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [BatteryReading::class], version = 1, exportSchema = false)
abstract class BatteryDatabase : RoomDatabase() {
    abstract fun batteryReadingDao(): BatteryReadingDao

    companion object {
        @Volatile
        private var INSTANCE: BatteryDatabase? = null

        fun getDatabase(context: Context): BatteryDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BatteryDatabase::class.java,
                    "battery_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
