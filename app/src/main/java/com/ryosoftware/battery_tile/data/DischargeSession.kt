package com.ryosoftware.battery_tile.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "discharge_sessions")
@Serializable
data class DischargeSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startTime: Long,
    val endTime: Long?,
    val startLevel: Int,
    val endLevel: Int?,
    val durationMinutes: Long?,
    val screenOnTimeMinutes: Long?,
    val avgTemperatureCelsius: Float?,
    val maxTemperatureCelsius: Float?,
    val minTemperatureCelsius: Float?,
    val screenOnSpeed: Float? = null,
    val screenOffSpeed: Float? = null,
    val screenOnDelta: Int? = null,
    val screenOffDelta: Int? = null
)
