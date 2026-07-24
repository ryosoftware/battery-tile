package com.ryosoftware.battery_tile

import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import androidx.annotation.ArrayRes
import androidx.annotation.StringRes
import com.ryosoftware.battery_tile.NotificationService.Companion.MIN_RECENT_INTERVAL
import com.ryosoftware.battery_tile.NotificationService.Companion.MIN_RECENT_READINGS
import com.ryosoftware.battery_tile.NotificationServiceUIBuilder.NotificationField.Companion.getLabel
import com.ryosoftware.battery_tile.Utils.Companion.getStringPercent
import com.ryosoftware.battery_tile.Utils.Companion.getStringTimeAndPercentFromInterval
import com.ryosoftware.battery_tile.Utils.Companion.getStringTimeFromInterval
import com.ryosoftware.battery_tile.Utils.Companion.isImperceptible
import com.ryosoftware.battery_tile.data.BatteryReading
import com.ryosoftware.battery_tile.data.ScreenState

class NotificationServiceUIBuilder(
    context: Context,
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
    context,
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
        DATA_SINCE_BOOT(key = "DATA-SINCE-BOOT", isSupported = true, label = R.string.data_since, labelLong = R.string.data_since_long, labelModifier = R.string.since_boot, comments = R.string.data_since_notification_comments, defaultsRes = R.array.data_since_boot_data_for_notification_default),
        DATA_SINCE_LAST_STATS_RESET(key = "DATA-SINCE-LAST-STATS-RESET", isSupported = true, label = R.string.data_since, labelLong = R.string.data_since_long, labelModifier = R.string.since_last_stats_reset, comments = R.string.data_since_notification_comments, defaultsRes = R.array.data_since_last_stats_reset_data_for_notification_default),
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

    private val recentConsumptionValues:DischargeRateStats? by lazy {
        if ((!isCharging) && (recentReadings.size >= MIN_RECENT_READINGS)) {
            val updatableRecentReadings = recentReadings as MutableList<BatteryReading>
            val updatableRecentScreenStates = recentScreenStates as MutableList<ScreenState>

            NotificationService.addToRecentBuffers(updatableRecentReadings, updatableRecentScreenStates, this, screenOn)

            val recentConsumptionNumbers = calculateDischargeRates(updatableRecentReadings, updatableRecentScreenStates)
            if ((recentConsumptionNumbers != null) && (recentConsumptionNumbers.endTime - recentConsumptionNumbers.startTime > MIN_RECENT_INTERVAL)) {
                recentConsumptionNumbers
            } else null
        } else null
    }

    fun isVisible(notificationField: NotificationField, prefs: NotificationPreferences): Boolean =
        when (notificationField) {
            NotificationField.DATA_SINCE_LAST_STATS_RESET -> (!prefs.isFieldVisible(NotificationField.DATA_SINCE_BOOT)) || (lastStatsResetReason != LastStatsResetReason.DEVICE_REBOOT)
            NotificationField.BATTERY_CHARGING_TIME -> isCharging && lastBatteryEventTime != 0L
            else -> true
        }

    fun toStringLabel(context: Context, notificationField: NotificationField, prefs: NotificationPreferences, appPrefs: AppPreferences): String? =
        when (notificationField) {
            NotificationField.BATTERY_RECENT_CONSUMPTION -> {
                val recents = recentConsumptionValues
                if ((recents != null) && (recents.overallDischargePercent != null) && (recents.overallDischargePercent > 0)) {
                    context.getString(R.string.recent_consumption_from_interval, getStringPercent(context, recents.overallDischargePercent), getStringTimeFromInterval(context, now - recents.startTime))
                } else null
            }
            NotificationField.DATA_SINCE_BOOT -> {
                if (timeSinceBoot > 0L) {
                    val uptime = getStringTimeFromInterval(context, timeSinceBoot)
                    context.getString(R.string.data_since_boot_label, uptime)
                }
                else null
            }
            NotificationField.DATA_SINCE_LAST_STATS_RESET -> {
                if (timeSinceLastStatsReset > 0L) {
                    val uptime = getStringTimeFromInterval(context, timeSinceLastStatsReset)
                    context.getString(R.string.data_since_last_stats_reset_label, uptime)
                }
                else null
            }
            else -> {
                notificationField.getLabel(context, true)
            }
        }

    fun toStringValue(context: Context, notificationField: NotificationField, prefs: NotificationPreferences, appPrefs: AppPreferences): String? =
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
                val recents = recentConsumptionValues
                if (recents != null) {
                    val overAllSpeed = recents.overallSpeed?.takeIf { it != 0f }?.let { getStringPercent(context, it, R.string.consumption_value_float, R.string.consumption_value_integer) }
                    val screenOnSpeed = recents.screenOnSpeed?.takeIf { it != 0f }?.let { getStringPercent(context, it, R.string.consumption_value_float, R.string.consumption_value_integer) }
                    val screenOffSpeed = recents.screenOffSpeed?.takeIf { it != 0f }?.let { getStringPercent(context, it, R.string.consumption_value_float, R.string.consumption_value_integer) }

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
            NotificationField.DATA_SINCE_BOOT -> {
                if (timeSinceBoot > 0L) {
                    val lines = mutableListOf<String>()

                    val screenOnTimeString = when {
                        isImperceptible(screenOnTimeSinceBoot, timeSinceBoot) -> null
                        else -> getStringTimeAndPercentFromInterval(context, screenOnTimeSinceBoot, timeSinceBoot, true, R.string.interval_value_with_percent)
                    }

                    screenOnTimeString?.let {
                        lines.add(context.getString(R.string.screen_on_data_value, it))
                    }

                    val deepSleepTimeString = when {
                        deepSleepTimeSinceBoot > 0L && timeSinceBoot > 0L -> getStringTimeAndPercentFromInterval(context, deepSleepTimeSinceBoot, timeSinceBoot, true, R.string.interval_value_with_percent)
                        else -> null
                    }

                    deepSleepTimeString?.let {
                        lines.add(context.getString(R.string.cpu_deep_sleep_value, it))
                    }

                    return lines.takeIf { it.isNotEmpty() }?.joinToString(context.getString(R.string.data_values_separator))
                } else null
            }
            NotificationField.DATA_SINCE_LAST_STATS_RESET -> {
                if (timeSinceLastStatsReset > 0L) {
                    val lines = mutableListOf<String>()

                    val screenOnTimeString = when {
                        isImperceptible(screenOnTimeSinceLastStatsReset, timeSinceLastStatsReset) -> null
                        else -> getStringTimeAndPercentFromInterval(context, screenOnTimeSinceLastStatsReset, timeSinceLastStatsReset, true, R.string.interval_value_with_percent)
                    }

                    screenOnTimeString?.let {
                        lines.add(context.getString(R.string.screen_on_data_value, it))
                    }

                    val deepSleepTimeString = when {
                        deepSleepTimeSinceLastStatsReset > 0L && timeSinceLastStatsReset > 0L -> getStringTimeAndPercentFromInterval(context, deepSleepTimeSinceLastStatsReset, timeSinceLastStatsReset, true, R.string.interval_value_with_percent)
                        else -> null
                    }

                    deepSleepTimeString?.let {
                        lines.add(context.getString(R.string.cpu_deep_sleep_value, it))
                    }

                    return lines.takeIf { it.isNotEmpty() }?.joinToString(context.getString(R.string.data_values_separator))
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
            else -> toString(context, notificationField.key, appPrefs, false)
        }
}
