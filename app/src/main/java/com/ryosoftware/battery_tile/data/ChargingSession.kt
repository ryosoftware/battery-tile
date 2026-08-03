package com.ryosoftware.battery_tile.data

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "charging_sessions")
@Serializable
data class ChargingSession @Ignore constructor(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startTime: Long,
    val endTime: Long?,
    val startLevel: Int,
    val endLevel: Int?,
    val startCharge: Long,
    val endCharge: Long?,
    val plugType: Int,
    val durationMinutes: Long?,
    val avgTemperatureCelsius: Float?,
    val maxTemperatureCelsius: Float?,
    val minTemperatureCelsius: Float?,
    val chargedTimeStamp: Long?,
    @Ignore val startTimeString: String? = null,
    @Ignore val endTimeString: String? = null
) {
    constructor(
        id: Long,
        startTime: Long,
        endTime: Long?,
        startLevel: Int,
        endLevel: Int?,
        startCharge: Long,
        endCharge: Long?,
        plugType: Int,
        durationMinutes: Long?,
        avgTemperatureCelsius: Float?,
        maxTemperatureCelsius: Float?,
        minTemperatureCelsius: Float?,
        chargedTimeStamp: Long?
    ) : this(
        id, startTime, endTime, startLevel, endLevel, startCharge, endCharge,
        plugType, durationMinutes, avgTemperatureCelsius, maxTemperatureCelsius,
        minTemperatureCelsius, chargedTimeStamp, null, null
    )
}
