package com.ryosoftware.battery_tile

import android.content.Context
import android.text.format.DateUtils
import androidx.annotation.StringRes

class Utils {
    companion object {
        fun getStringPercent(context: Context, percent: Float?, @StringRes resFloat: Int, @StringRes resInt: Int): String {
            if (percent == null) return context.getString(R.string.not_available)

            val hasNoDecimals = percent % 1f == 0f

            return if (hasNoDecimals) context.getString(resInt, percent.toInt())
            else context.getString(resFloat, percent)
        }

        fun getStringPercent(context: Context, percent: Float?): String =
            getStringPercent(context, percent, R.string.percent_value_float, R.string.percent_value_integer)

        fun getPercentFromInterval(interval: Long, total: Long): Float =
            if (total == 0L) 0f else (interval * 100f / total).coerceAtLeast(0f)

        fun getStringPercentFromInterval(context: Context, interval: Long, total: Long, @StringRes resFloat: Int, @StringRes resInt: Int): String =
            getStringPercent(context, getPercentFromInterval(interval, total), resFloat, resInt)

        fun getStringPercentFromInterval(context: Context, interval: Long, total: Long): String =
            getStringPercentFromInterval(context, interval, total, R.string.percent_value_float, R.string.percent_value_integer)

        fun getStringTimeFromInterval(context: Context, interval: Long): String {
            val totalMinutes = interval / DateUtils.MINUTE_IN_MILLIS
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

        private fun isImperceptible(interval: Long, total: Long, checkTime: Boolean, checkPercent: Boolean): Boolean {
            if (checkTime && (interval / DateUtils.MINUTE_IN_MILLIS == 0L)) return true
            if (checkPercent && (getPercentFromInterval(interval, total) == 0f)) return true
            return false
        }

        fun isImperceptible(interval: Long, total: Long) =
            isImperceptible(interval, total, checkTime = true, checkPercent = true)

        fun isImperceptible(interval: Long) =
            isImperceptible(interval, 0, checkTime = true, checkPercent = false)

        fun getStringTimeAndPercentFromInterval(context: Context, interval: Long, total: Long, excludeImperceptibleValues: Boolean = false, @StringRes resource: Int = R.string.time_and_percent): String =
            if (excludeImperceptibleValues && isImperceptible(interval, total, checkTime = true, checkPercent = false)) {
                getStringPercentFromInterval(context, interval, total)
            } else {
                context.getString(resource, getStringTimeFromInterval(context, interval), getStringPercentFromInterval(context, interval, total))
            }
    }
}