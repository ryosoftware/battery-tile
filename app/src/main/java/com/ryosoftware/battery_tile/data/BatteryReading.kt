package com.ryosoftware.battery_tile.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "battery_readings")
data class BatteryReading(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val batteryLevel: Int,
    val batteryStatus: Int,
    val temperatureCelsius: Float,
    val voltage: Int,
    val health: Int,
    val isCharging: Boolean,
    val plugType: Int
)
