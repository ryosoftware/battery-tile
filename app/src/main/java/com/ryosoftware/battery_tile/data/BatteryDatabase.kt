package com.ryosoftware.battery_tile.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [BatteryReading::class, ChargingSession::class], version = 2, exportSchema = false)
abstract class BatteryDatabase : RoomDatabase() {
    abstract fun batteryReadingDao(): BatteryReadingDao
    abstract fun chargingSessionDao(): ChargingSessionDao

    companion object {
        @Volatile
        private var INSTANCE: BatteryDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS charging_sessions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        startTime INTEGER NOT NULL,
                        endTime INTEGER,
                        startLevel INTEGER NOT NULL,
                        endLevel INTEGER,
                        plugType INTEGER NOT NULL,
                        durationMinutes INTEGER,
                        avgTemperatureCelsius REAL,
                        maxTemperatureCelsius REAL,
                        minTemperatureCelsius REAL
                    )
                """)
            }
        }

        fun getDatabase(context: Context): BatteryDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BatteryDatabase::class.java,
                    "battery_database"
                ).addMigrations(MIGRATION_1_2).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
