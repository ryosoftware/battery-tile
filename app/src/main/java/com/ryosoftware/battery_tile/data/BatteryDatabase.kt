package com.ryosoftware.battery_tile.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [BatteryReading::class, ChargingSession::class, ScreenState::class, DischargeSession::class], version = 5, exportSchema = false)
abstract class BatteryDatabase : RoomDatabase() {
    abstract fun batteryReadingDao(): BatteryReadingDao
    abstract fun chargingSessionDao(): ChargingSessionDao
    abstract fun screenStateDao(): ScreenStateDao
    abstract fun dischargeSessionDao(): DischargeSessionDao

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

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS screen_states (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        screenOn INTEGER NOT NULL
                    )
                """)
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE charging_sessions ADD COLUMN chargedTimeStamp INTEGER")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS discharge_sessions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        startTime INTEGER NOT NULL,
                        endTime INTEGER,
                        startLevel INTEGER NOT NULL,
                        endLevel INTEGER,
                        durationMinutes INTEGER,
                        screenOnTimeMinutes INTEGER,
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
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
