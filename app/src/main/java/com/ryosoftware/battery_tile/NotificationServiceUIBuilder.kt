package com.ryosoftware.battery_tile

import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import androidx.annotation.ArrayRes
import androidx.annotation.StringRes
import com.ryosoftware.battery_tile.NotificationService.Companion.MIN_RECENT_INTERVAL
import com.ryosoftware.battery_tile.NotificationService.Companion.MIN_RECENT_READINGS
import com.ryosoftware.battery_tile.Utils.Companion.getStringPercent
import com.ryosoftware.battery_tile.Utils.Companion.getStringTimeAndPercentFromInterval
import com.ryosoftware.battery_tile.Utils.Companion.getStringTimeFromInterval
import com.ryosoftware.battery_tile.Utils.Companion.isImperceptible
import com.ryosoftware.battery_tile.data.BatteryReading
import com.ryosoftware.battery_tile.data.ScreenState

class NotificationServiceUIBuilder(
    intent: Intent,
    lastStatsResetTime: Long,
    private val lastStatsResetReason: LastStatsResetReason?,
    deepSleepTimeAtLastStatsReset: Long,
    screenOnTimeSinceBoot: Long,
    screenOnTimeSinceLastStatsReset: Long,
    private val lastBatteryEventTime: Long,
    batteryManager: BatteryManager,
    private val screenOn: Boolean,
    private val recentReadings: List<BatteryReading>,
    private val recentScreenStates: List<ScreenState>
) : BaseUIBuilder(
    intent,
    lastStatsResetTime,
    deepSleepTimeAtLastStatsReset,
    screenOnTimeSinceBoot,
    screenOnTimeSinceLastStatsReset,
    batteryManager) {
    enum class NotificationField(val key: String, val isSupported: Boolean, @param:StringRes val label: Int = 0, @param:StringRes val labelLong: Int = 0, @param:StringRes val labelModifier: Int = 0, @param:StringRes val comments: Int = 0, @param:ArrayRes val defaultsRes: Int) {
        BATTERY_LEVEL(key = BatteryIntentHelper.BATTERY_LEVEL, isSupported = isSupported(BatteryIntentHelper.BATTERY_LEVEL), comments = R.string.battery_level_notification_comments, defaultsRes = R.array.level_data_for_notification_default),
        BATTERY_CURRENT_CONSUMPTION(key = BatteryIntentHelper.BATTERY_CURRENT_CONSUMPTION, isSupported = isSupported(BatteryIntentHelper.BATTERY_CURRENT_CONSUMPTION), label = R.string.current_consumption, labelLong = R.string.current_consumption_long, defaultsRes = R.array.current_consumption_data_for_notification_default),
        BATTERY_STATUS(key = BatteryIntentHelper.BATTERY_STATUS, isSupported = isSupported(BatteryIntentHelper.BATTERY_STATUS), defaultsRes = R.array.status_data_for_notification_default),
        BATTERY_TEMPERATURE(key = BatteryIntentHelper.BATTERY_TEMPERATURE, isSupported = isSupported(BatteryIntentHelper.BATTERY_TEMPERATURE), defaultsRes = R.array.temperature_data_for_notification_default),
        BATTERY_VOLTAGE(key = BatteryIntentHelper.BATTERY_VOLTAGE, isSupported = isSupported(BatteryIntentHelper.BATTERY_VOLTAGE), defaultsRes = R.array.voltage_data_for_notification_default),
        BATTERY_HEALTH(key = BatteryIntentHelper.BATTERY_HEALTH, isSupported = isSupported(BatteryIntentHelper.BATTERY_HEALTH), defaultsRes = R.array.health_data_for_notification_default),
        BATTERY_CYCLES_COUNT(key = BatteryIntentHelper.BATTERY_CYCLES_COUNT, isSupported = isSupported(BatteryIntentHelper.BATTERY_CYCLES_COUNT), comments = R.string.battery_cycles_notification_comments, defaultsRes = R.array.cycles_count_data_for_notification_default),
        BATTERY_RECENT_CONSUMPTION(key = "BATTERY-RECENT-CONSUMPTION", isSupported = true, label = R.string.recent_consumption, labelLong = R.string.recent_consumption_long, comments = R.string.recent_consumption_notification_comments, defaultsRes = R.array.recent_consumption_data_for_notification_default),
        CPU_DEEP_SLEEP_TIME_SINCE_BOOT(key = "CPU-DEEP-SLEEP-TIME-SINCE-BOOT", isSupported = true, label = R.string.cpu_deep_sleep_time, labelLong = R.string.cpu_deep_sleep_time_long, labelModifier = R.string.since_boot, comments = R.string.cpu_deep_sleep_time_notification_comments, defaultsRes = R.array.deep_sleep_time_since_boot_data_for_notification_default),
        CPU_DEEP_SLEEP_TIME_SINCE_LAST_STATS_RESET(key = "CPU-DEEP-SLEEP-TIME-SINCE-LAST-STATS-RESET", isSupported = true, label = R.string.cpu_deep_sleep_time, labelLong = R.string.cpu_deep_sleep_time_long, labelModifier = R.string.since_last_stats_reset, comments = R.string.cpu_deep_sleep_time_notification_comments, defaultsRes = R.array.deep_sleep_time_since_last_stats_reset_data_for_notification_default),
        UPTIME_SINCE_BOOT(key = "UPTIME-SINCE-BOOT", isSupported = true, label = R.string.uptime, labelLong = R.string.uptime_long, labelModifier = R.string.since_boot, comments = R.string.uptime_notification_comments, defaultsRes = R.array.uptime_since_boot_data_for_notification_default),
        UPTIME_SINCE_LAST_STATS_RESET(key = "UPTIME-SINCE-LAST-STATS-RESET", isSupported = true, label = R.string.uptime, labelLong = R.string.uptime_long, labelModifier = R.string.since_last_stats_reset, comments = R.string.uptime_notification_comments, defaultsRes = R.array.uptime_since_last_stats_reset_data_for_notification_default),
        BATTERY_CHARGING_TIME(key = "BATTERY-CHARGING-TIME", isSupported = true, label = R.string.time_charging, labelLong = R.string.time_charging_long, comments = R.string.time_charging_notification_comments, defaultsRes = R.array.charging_time_data_for_notification_default);

        companion object {
            private val map = entries.associateBy { it.key.uppercase() }

            fun fromKey(key: String?): NotificationField? = map[key?.uppercase()]

            fun NotificationField.getLabel(context: Context, small: Boolean): String =
                when {
                    label == 0 -> getLabel(context, key)
                    labelModifier == 0 -> context.getString(if (small) label else labelLong)
                    else -> context.getString(if (small) label else labelLong, context.getString(labelModifier))
                }

            fun NotificationField.getComments(context: Context): String? =
                when {
                    comments == 0 -> null
                    else -> context.getString(comments)
                }
        }
    }

    fun isVisible(notificationField: NotificationField, prefs: NotificationPreferences): Boolean =
        when (notificationField) {
            NotificationField.UPTIME_SINCE_LAST_STATS_RESET -> (!prefs.isFieldVisible(NotificationField.UPTIME_SINCE_BOOT)) || (lastStatsResetReason != LastStatsResetReason.DEVICE_REBOOT)
            NotificationField.CPU_DEEP_SLEEP_TIME_SINCE_LAST_STATS_RESET -> (!prefs.isFieldVisible(NotificationField.CPU_DEEP_SLEEP_TIME_SINCE_BOOT)) || (lastStatsResetReason != LastStatsResetReason.DEVICE_REBOOT)
            NotificationField.BATTERY_CHARGING_TIME -> isCharging && lastBatteryEventTime != 0L
            else -> true
        }

    fun toString(context: Context, notificationField: NotificationField, prefs: NotificationPreferences, appPrefs: AppPreferences): String? =
        when(notificationField) {
            NotificationField.BATTERY_LEVEL -> {
                when {
                    level < 0 -> null
                    else -> {
                        val level = toString(context, BATTERY_LEVEL, appPrefs, false)

                        if (charge <= 0) {
                            level
                        } else {
                            context.getString(R.string.battery_level_and_charge, level, toString(context, BATTERY_CHARGE, appPrefs, false))
                        }
                    }
                }
            }
            NotificationField.BATTERY_RECENT_CONSUMPTION -> {
                if ((!isCharging) && (recentReadings.size >= MIN_RECENT_READINGS) && (recentReadings.last().timestamp - recentReadings.first().timestamp > MIN_RECENT_INTERVAL)) {
                    val updatedRecentReadings = recentReadings.toMutableList()
                    val updatedRecentScreenStates = recentScreenStates.toMutableList()

                    NotificationService.addToRecentBuffers(updatedRecentReadings, updatedRecentScreenStates, this, screenOn)

                    val endTime = updatedRecentReadings.last().timestamp
                    val startTime = updatedRecentScreenStates.first().timestamp

                    val numbers = calculateDischargeRates(updatedRecentReadings, updatedRecentScreenStates, startTime, endTime)

                    val overAllSpeed = numbers.overallSpeed?.takeIf { it != 0f }?.let { getStringPercent(context, numbers.overallSpeed, R.string.consumption_value_float, R.string.consumption_value_integer) }
                    val screenOnSpeed = numbers.screenOnSpeed?.takeIf { it != 0f }?.let { getStringPercent(context, it, R.string.consumption_value_float, R.string.consumption_value_integer) }
                    val screenOffSpeed = numbers.screenOffSpeed?.takeIf { it != 0f }?.let { getStringPercent(context, it, R.string.consumption_value_float, R.string.consumption_value_integer) }

                    if (overAllSpeed == null) {
                        return null
                    }

                    if ((screenOnSpeed != null) && (screenOffSpeed != null)) {
                        context.getString(R.string.recent_consumption_format, overAllSpeed, screenOnSpeed, screenOffSpeed)
                    } else {
                        context.getString(R.string.recent_consumption_format_no_screen, overAllSpeed)
                    }
                } else null
            }
            NotificationField.CPU_DEEP_SLEEP_TIME_SINCE_BOOT -> {
                if ((deepSleepTimeSinceBoot > 0L) && (timeSinceBoot > 0L)) {
                    getStringTimeAndPercentFromInterval(context, deepSleepTimeSinceBoot, timeSinceBoot, true)
                } else null
            }
            NotificationField.CPU_DEEP_SLEEP_TIME_SINCE_LAST_STATS_RESET -> {
                if ((deepSleepTimeSinceLastStatsReset > 0L) && (timeSinceLastStatsReset > 0L)) {
                    getStringTimeAndPercentFromInterval(context, deepSleepTimeSinceLastStatsReset, timeSinceLastStatsReset, true)
                } else null
            }
            NotificationField.UPTIME_SINCE_BOOT -> {
                if (timeSinceBoot > 0L) {
                    val uptime = getStringTimeFromInterval(context, timeSinceBoot)

                    if (isImperceptible(screenOnTimeSinceBoot, timeSinceBoot)) {
                        context.getString(R.string.uptime_value, uptime)
                    } else {
                        val screenOnTime = getStringTimeAndPercentFromInterval(context, screenOnTimeSinceBoot, timeSinceBoot, true, R.string.screen_time_with_percent)
                        context.getString(R.string.uptime_value_with_screen_on, uptime, screenOnTime)
                    }
                } else null
            }
            NotificationField.UPTIME_SINCE_LAST_STATS_RESET -> {
                if (timeSinceLastStatsReset > 0L) {
                    val uptime = getStringTimeFromInterval(context, timeSinceLastStatsReset)

                    if (isImperceptible(screenOnTimeSinceLastStatsReset, timeSinceLastStatsReset)) {
                        context.getString(R.string.uptime_value, uptime)
                    } else {
                        val screenOnTime = getStringTimeAndPercentFromInterval(context, screenOnTimeSinceLastStatsReset, timeSinceLastStatsReset, true, R.string.screen_time_with_percent)
                        context.getString(R.string.uptime_value_with_screen_on, uptime, screenOnTime)
                    }
                } else null
            }
            NotificationField.BATTERY_CHARGING_TIME -> {
                if (isCharging) {
                    val timeCharging = timeSinceBoot - lastBatteryEventTime

                    if (! isImperceptible(timeCharging)) {
                        getStringTimeFromInterval(context, timeCharging)
                    } else null
                } else null
            }
            else -> super.toString(context, notificationField.key, appPrefs, false)
        }
}
