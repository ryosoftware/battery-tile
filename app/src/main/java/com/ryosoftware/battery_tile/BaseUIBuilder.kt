package com.ryosoftware.battery_tile

import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.os.SystemClock

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
        protected fun getStringPercentFromInterval(context: Context, interval: Long, total: Long, resFloat: Int, resInt: Int): String {
            val percent = if (total == 0L) 0f else (interval * 100f / total).coerceAtLeast(0f)
            val hasNoDecimals = percent % 1f == 0f
            return if (hasNoDecimals) context.getString(resInt, percent.toInt())
            else context.getString(resFloat, percent)
        }

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
        protected fun getStringTimeAndPercentFromInterval(context: Context, interval: Long, total: Long): String =
            context.getString(R.string.time_and_percent,
                getStringTimeFromInterval(context, interval),
                getStringPercentFromInterval(context, interval, total))

    }
}