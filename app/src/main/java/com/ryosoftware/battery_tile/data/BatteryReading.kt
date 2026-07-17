package com.ryosoftware.battery_tile.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "battery_readings")
@Serializable
data class BatteryReading(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val batteryLevel: Int,
    val batteryCharge: Int = 0,
    val batteryStatus: Int,
    val temperatureCelsius: Float,
    val voltage: Int,
    val health: Int,
    val isCharging: Boolean,
    val plugType: Int
)
