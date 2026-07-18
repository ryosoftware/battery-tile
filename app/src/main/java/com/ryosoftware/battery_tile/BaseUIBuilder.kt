package com.ryosoftware.battery_tile

import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.os.SystemClock
import androidx.annotation.StringRes

abstract class BaseUIBuilder(
    intent: Intent,
    protected val lastStatsResetTime: Long,
    protected val deepSleepTimeAtLastStatsReset: Long,
    protected val screenOnTimeSinceBoot: Long,
    protected val screenOnTimeSinceLastStatsReset: Long,
    batteryManager: BatteryManager?
) : BatteryIntentHelper(intent, batteryManager) {
    val timeSinceBoot: Long by lazy { SystemClock.elapsedRealtime() }
    val deepSleepTimeSinceBoot: Long by lazy { timeSinceBoot - SystemClock.uptimeMillis() }
    val timeSinceLastStatsReset: Long by lazy { System.currentTimeMillis() - lastStatsResetTime }
    val deepSleepTimeSinceLastStatsReset: Long by lazy { deepSleepTimeSinceBoot - deepSleepTimeAtLastStatsReset }

    companion object {
        @JvmStatic
        private fun getPercentFromInterval(interval: Long, total: Long): Float =
            if (total == 0L) 0f else (interval * 100f / total).coerceAtLeast(0f)

        @JvmStatic
        protected fun getStringPercentFromInterval(context: Context, interval: Long, total: Long, resFloat: Int, resInt: Int): String =
            getStringPercent(context, getPercentFromInterval(interval, total), resFloat, resInt)

        @JvmStatic
        protected fun getStringPercentFromInterval(context: Context, interval: Long, total: Long): String =
            getStringPercentFromInterval(context, interval, total, R.string.percent_value_float, R.string.percent_value_integer)

        @JvmStatic
        protected fun getStringTimeFromInterval(context: Context, interval: Long): String {
            val totalMinutes = interval / 60_000L
            val days = totalMinutes / (24 * 60)
            val hours = (totalMinutes % (24 * 60)) / 60
            val minutes = totalMinutes % 60

            return if (days > 0) {
                context.getString(R.string.days_and_hours_and_minutes, days, hours, minutes)
            } else if (hours > 0) {
                context.getString(R.string.hours_and_minutes, hours, minutes)
            } else {
                context.getString(R.string.minutes, minutes)
            }
        }

        @JvmStatic
        private fun isImperceptible(interval: Long, total: Long, checkTime: Boolean, checkPercent: Boolean): Boolean {
            if (checkTime && (interval / 60_000L == 0L)) return true
            if (checkPercent && (getPercentFromInterval(interval, total) == 0f)) return true
            return false
        }

        @JvmStatic
        protected fun isImperceptible(interval: Long, total: Long) =
            isImperceptible(interval, total, checkTime = true, checkPercent = true)

        @JvmStatic
        protected fun isImperceptible(interval: Long) =
            isImperceptible(interval, 0, checkTime = true, checkPercent = false)

        @JvmStatic
        protected fun getStringTimeAndPercentFromInterval(context: Context, interval: Long, total: Long, excludeImperceptibleValues: Boolean = false, @StringRes resource: Int = R.string.time_and_percent): String =
            if (excludeImperceptibleValues && isImperceptible(interval, total, checkTime = true, checkPercent = false)) {
                getStringPercentFromInterval(context, interval, total)
            } else {
                context.getString(resource, getStringTimeFromInterval(context, interval), getStringPercentFromInterval(context, interval, total))
            }
    }
}