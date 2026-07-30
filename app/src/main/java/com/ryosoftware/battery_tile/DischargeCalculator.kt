package com.ryosoftware.battery_tile

import android.text.format.DateUtils
import com.ryosoftware.battery_tile.data.BatteryReading
import com.ryosoftware.battery_tile.data.ScreenState
import kotlin.math.max

data class DischargeRateStats(
    val startTime: Long,
    val endTime: Long,
    val overallSpeed: Float?,
    val screenOnSpeed: Float?,
    val screenOffSpeed: Float?,
    val overallDischargePercent: Float?,
    val overallDischargeMaH: Float?,
    val screenOnDischargePercent: Float?,
    val screenOffDischargePercent: Float?
)

internal fun calculateDischargeRates(readings: List<BatteryReading>, screenStates: List<ScreenState>, startTime: Long, endTime: Long): DischargeRateStats {
    val sortedScreenStates = screenStates.sortedBy { it.timestamp }

    val analysisStartTime = max(startTime, sortedScreenStates.firstOrNull()?.timestamp ?: startTime)

    val sortedReadings = readings
        .filter { it.timestamp in analysisStartTime..endTime }
        .sortedBy { it.timestamp }

    if (sortedReadings.size < 2) return DischargeRateStats(analysisStartTime, endTime, null, null, null, null, null, null, null)
    if (sortedReadings.any { it.batteryCharge <= 0 }) {
        return DischargeRateStats(analysisStartTime, endTime, null, null, null, null, null, null, null)
    }

    val firstLevel = sortedReadings.first().batteryLevel
    val lastLevel = sortedReadings.last().batteryLevel
    val firstCharge = sortedReadings.first().batteryCharge
    val lastCharge = sortedReadings.last().batteryCharge
    val overallDischargePercent = (firstLevel - lastLevel).coerceAtLeast(0)
    val overallDischargeMaH = ((firstCharge - lastCharge).coerceAtLeast(0)) / 1000f

    var wasScreenOn = sortedScreenStates.lastOrNull { it.timestamp < startTime }?.screenOn ?: false

    var screenStateIndex = sortedScreenStates.indexOfFirst { it.timestamp >= startTime }
    if (screenStateIndex < 0) screenStateIndex = sortedScreenStates.size

    var screenOnDischargeMicroAh = 0.0f
    var screenOnDurationMs = 0L
    var screenOffDischargeMicroAh = 0.0f
    var screenOffDurationMs = 0L
    var totalDischargePercent = 0.0f
    var totalDischargeDurationMs = 0L

    fun addDuration(screenOn: Boolean, durationMs: Long) {
        if (screenOn) screenOnDurationMs += durationMs
        else screenOffDurationMs += durationMs
    }

    fun addDelta(screenOn: Boolean, microAh: Float) {
        if (screenOn) screenOnDischargeMicroAh += microAh
        else screenOffDischargeMicroAh += microAh
    }

    for (i in 0 until sortedReadings.size - 1) {
        val current = sortedReadings[i]
        val next = sortedReadings[i + 1]

        while ((screenStateIndex < sortedScreenStates.size) && (sortedScreenStates[screenStateIndex].timestamp <= current.timestamp)) {
            wasScreenOn = sortedScreenStates[screenStateIndex].screenOn
            screenStateIndex++
        }

        val intervalDuration = next.timestamp - current.timestamp
        if (intervalDuration <= 0L) continue
        val intervalDeltaPercent = current.batteryLevel - next.batteryLevel
        val intervalDeltaMicroAh = current.batteryCharge - next.batteryCharge
        val hasDischarge = intervalDeltaMicroAh > 0

        val firstChangeIdx = screenStateIndex
        var changeCount = 0
        while ((screenStateIndex + changeCount < sortedScreenStates.size) && (sortedScreenStates[screenStateIndex + changeCount].timestamp < next.timestamp)) {
            changeCount++
        }

        var cursorTime = current.timestamp
        var cursorCharge = current.batteryCharge.toFloat()
        var currentScreenOn = wasScreenOn

        for (j in 0 until changeCount) {
            val change = sortedScreenStates[firstChangeIdx + j]
            val fraction = (change.timestamp - current.timestamp).toFloat() / intervalDuration
            val charge = current.batteryCharge.toFloat() + (next.batteryCharge - current.batteryCharge) * fraction

            addDuration(currentScreenOn, change.timestamp - cursorTime)
            if (hasDischarge) addDelta(currentScreenOn, cursorCharge - charge)

            cursorTime = change.timestamp
            cursorCharge = charge
            currentScreenOn = change.screenOn
        }

        addDuration(currentScreenOn, next.timestamp - cursorTime)
        if (hasDischarge) addDelta(currentScreenOn, cursorCharge - next.batteryCharge)

        if (hasDischarge) {
            totalDischargePercent += max(intervalDeltaPercent, 0)
            totalDischargeDurationMs += intervalDuration
        }
        screenStateIndex += changeCount
        wasScreenOn = currentScreenOn
    }

    val totalDischargeMicroAh = screenOnDischargeMicroAh + screenOffDischargeMicroAh
    val percentPerMicroAh = if (totalDischargeMicroAh > 0f) { (totalDischargePercent / totalDischargeMicroAh).coerceAtLeast(0f) } else 1f

    val screenOnDischargePercent = screenOnDischargeMicroAh * percentPerMicroAh
    val screenOffDischargePercent = screenOffDischargeMicroAh * percentPerMicroAh

    val overallSpeed = if (totalDischargeDurationMs > 0) {
        totalDischargePercent / (totalDischargeDurationMs.toFloat() / DateUtils.HOUR_IN_MILLIS)
    } else null

    val screenOnSpeed = if (screenOnDurationMs > 0) {
        screenOnDischargePercent / (screenOnDurationMs.toFloat() / DateUtils.HOUR_IN_MILLIS)
    } else null

    val screenOffSpeed = if (screenOffDurationMs > 0) {
        screenOffDischargePercent / (screenOffDurationMs.toFloat() / DateUtils.HOUR_IN_MILLIS)
    } else null

    return DischargeRateStats(analysisStartTime, endTime, overallSpeed, screenOnSpeed, screenOffSpeed, overallDischargePercent.toFloat(), overallDischargeMaH, screenOnDischargePercent, screenOffDischargePercent)
}

internal fun calculateDischargeRates(readings: List<BatteryReading>, screenStates: List<ScreenState>): DischargeRateStats? =
    if (readings.isEmpty())
        null
    else
        calculateDischargeRates(readings, screenStates, readings.first().timestamp, readings.last().timestamp)
