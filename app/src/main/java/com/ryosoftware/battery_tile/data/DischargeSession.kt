package com.ryosoftware.battery_tile.data

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "discharge_sessions")
@Serializable
data class DischargeSession @Ignore constructor(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startTime: Long,
    val endTime: Long?,
    val startLevel: Int,
    val endLevel: Int?,
    val startCharge: Long,
    val endCharge: Long?,
    val durationMinutes: Long?,
    val screenOnTimeMinutes: Long?,
    val avgTemperatureCelsius: Float?,
    val maxTemperatureCelsius: Float?,
    val minTemperatureCelsius: Float?,
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
        durationMinutes: Long?,
        screenOnTimeMinutes: Long?,
        avgTemperatureCelsius: Float?,
        maxTemperatureCelsius: Float?,
        minTemperatureCelsius: Float?
    ) : this(
        id, startTime, endTime, startLevel, endLevel, startCharge, endCharge,
        durationMinutes, screenOnTimeMinutes, avgTemperatureCelsius,
        maxTemperatureCelsius, minTemperatureCelsius, null, null
    )
}
