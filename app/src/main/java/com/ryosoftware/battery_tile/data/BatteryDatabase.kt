package com.ryosoftware.battery_tile.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [BatteryReading::class, ChargingSession::class, ScreenState::class, DischargeSession::class], version = 11, exportSchema = false)
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

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE discharge_sessions ADD COLUMN screenOnSpeed REAL")
                db.execSQL("ALTER TABLE discharge_sessions ADD COLUMN screenOffSpeed REAL")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE discharge_sessions ADD COLUMN screenOnDelta INTEGER")
                db.execSQL("ALTER TABLE discharge_sessions ADD COLUMN screenOffDelta INTEGER")
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS discharge_sessions_v2 (
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
                db.execSQL("""
                    INSERT INTO discharge_sessions_v2
                        (id, startTime, endTime, startLevel, endLevel, durationMinutes,
                         screenOnTimeMinutes, avgTemperatureCelsius, maxTemperatureCelsius, minTemperatureCelsius)
                    SELECT id, startTime, endTime, startLevel, endLevel, durationMinutes,
                           screenOnTimeMinutes, avgTemperatureCelsius, maxTemperatureCelsius, minTemperatureCelsius
                    FROM discharge_sessions
                """)
                db.execSQL("DROP TABLE IF EXISTS discharge_sessions")
                db.execSQL("ALTER TABLE discharge_sessions_v2 RENAME TO discharge_sessions")
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE battery_readings ADD COLUMN batteryCharge INTEGER NOT NULL DEFAULT -1")
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE charging_sessions ADD COLUMN startCharge INTEGER NOT NULL DEFAULT -1")
                db.execSQL("ALTER TABLE charging_sessions ADD COLUMN endCharge INTEGER")
                db.execSQL("ALTER TABLE discharge_sessions ADD COLUMN startCharge INTEGER NOT NULL DEFAULT -1")
                db.execSQL("ALTER TABLE discharge_sessions ADD COLUMN endCharge INTEGER")
            }
        }

        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    UPDATE charging_sessions
                    SET durationMinutes = CASE
                        WHEN chargedTimeStamp IS NOT NULL THEN (chargedTimeStamp - startTime) / 60000
                        ELSE durationMinutes
                    END
                    WHERE endTime IS NOT NULL
                """)
            }
        }

        fun getDatabase(context: Context): BatteryDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BatteryDatabase::class.java,
                    "battery_database"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
