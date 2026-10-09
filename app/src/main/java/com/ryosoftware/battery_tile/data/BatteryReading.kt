package com.ryosoftware.battery_tile.data

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "battery_readings")
@Serializable
data class BatteryReading @Ignore constructor(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val batteryLevel: Int,
    val batteryCharge: Long,
    val batteryStatus: Int,
    val temperatureCelsius: Float,
    val voltage: Int,
    val health: Int,
    val isCharging: Boolean,
    val plugType: Int,
    val deepSleepPercentSinceBoot: Float? = null,
    val deepSleepPercentSinceLastStatsReset: Float? = null,
    val lastBootTime: Long? = null,
    val lastStatsResetTime: Long? = null,
    @Ignore val timestampString: String? = null
) {
    constructor(
        id: Long,
        timestamp: Long,
        batteryLevel: Int,
        batteryCharge: Long,
        batteryStatus: Int,
        temperatureCelsius: Float,
        voltage: Int,
        health: Int,
        isCharging: Boolean,
        plugType: Int,
        deepSleepPercentSinceBoot: Float?,
        deepSleepPercentSinceLastStatsReset: Float?,
        lastBootTime: Long?,
        lastStatsResetTime: Long?
    ) : this(
        id, timestamp, batteryLevel, batteryCharge, batteryStatus,
        temperatureCelsius, voltage, health, isCharging, plugType, deepSleepPercentSinceBoot, deepSleepPercentSinceLastStatsReset, lastBootTime, lastStatsResetTime, null
    )
}
