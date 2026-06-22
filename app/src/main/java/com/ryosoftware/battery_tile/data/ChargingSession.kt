package com.ryosoftware.battery_tile.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "charging_sessions")
data class ChargingSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startTime: Long,
    val endTime: Long?,
    val startLevel: Int,
    val endLevel: Int?,
    val plugType: Int,
    val durationMinutes: Long?,
    val avgTemperatureCelsius: Float?,
    val maxTemperatureCelsius: Float?,
    val minTemperatureCelsius: Float?
)
