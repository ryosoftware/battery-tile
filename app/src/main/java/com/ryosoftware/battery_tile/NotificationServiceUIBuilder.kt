package com.ryosoftware.battery_tile

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.annotation.ArrayRes

class NotificationServiceUIBuilder(
    intent: Intent,
    lastStatsResetTime: Long,
    deepSleepTimeAtLastStatsReset: Long,
    screenOnTimeSinceBoot: Long,
    screenOnTimeSinceLastStatsReset: Long,
    private val lastBatteryEventTime: Long,
) : BaseBatteryIntentHelper(
    intent,
    lastStatsResetTime,
    deepSleepTimeAtLastStatsReset,
    screenOnTimeSinceBoot,
    screenOnTimeSinceLastStatsReset) {
    enum class NotificationField(val key: String, val isSupported: Boolean, @param:ArrayRes val defaultsRes: Int) {
        BATTERY_LEVEL(key = BatteryIntentHelper.BATTERY_LEVEL, isSupported = BatteryIntentHelper.isSupported(BatteryIntentHelper.BATTERY_LEVEL), defaultsRes = R.array.level_data_for_notification_default),
        BATTERY_STATUS(key = BatteryIntentHelper.BATTERY_STATUS, isSupported = BatteryIntentHelper.isSupported(BatteryIntentHelper.BATTERY_STATUS), defaultsRes = R.array.status_data_for_notification_default),
        BATTERY_TEMPERATURE(key = BatteryIntentHelper.BATTERY_TEMPERATURE, isSupported = BatteryIntentHelper.isSupported(BatteryIntentHelper.BATTERY_TEMPERATURE), defaultsRes = R.array.temperature_data_for_notification_default),
        BATTERY_VOLTAGE(key = BatteryIntentHelper.BATTERY_VOLTAGE, isSupported = BatteryIntentHelper.isSupported(BatteryIntentHelper.BATTERY_VOLTAGE), defaultsRes = R.array.voltage_data_for_notification_default),
        BATTERY_HEALTH(key = BatteryIntentHelper.BATTERY_HEALTH, isSupported = BatteryIntentHelper.isSupported(BatteryIntentHelper.BATTERY_HEALTH), defaultsRes = R.array.health_data_for_notification_default),
        BATTERY_CYCLES_COUNT(key = BatteryIntentHelper.BATTERY_CYCLES_COUNT, isSupported = BatteryIntentHelper.isSupported(BatteryIntentHelper.BATTERY_CYCLES_COUNT), defaultsRes = R.array.cycles_count_data_for_notification_default),
        CPU_DEEP_SLEEP_TIME_SINCE_BOOT(key = "CPU-DEEP-SLEEP-TIME-SINCE-BOOT", isSupported = true, defaultsRes = R.array.deep_sleep_time_since_boot_data_for_notification_default),
        CPU_DEEP_SLEEP_PERCENT_SINCE_BOOT(key = "CPU-DEEP-SLEEP-PERCENT-SINCE-BOOT", isSupported = true, defaultsRes = R.array.deep_sleep_percent_since_boot_data_for_notification_default),
        CPU_DEEP_SLEEP_TIME_SINCE_LAST_STATS_RESET(key = "CPU-DEEP-SLEEP-TIME-SINCE-LAST-STATS-RESET", isSupported = true, defaultsRes = R.array.deep_sleep_time_since_last_stats_reset_data_for_notification_default),
        CPU_DEEP_SLEEP_PERCENT_SINCE_LAST_STATS_RESET(key = "CPU-DEEP-SLEEP-PERCENT-SINCE-LAST-STATS-RESET", isSupported = true, defaultsRes = R.array.deep_sleep_percent_since_last_stats_reset_data_for_notification_default),
        SCREEN_ON_TIME_SINCE_BOOT(key = "SCREEN-ON-TIME-SINCE-BOOT", isSupported = true, defaultsRes = R.array.screen_on_time_since_boot_data_for_notification_default),
        SCREEN_ON_PERCENT_SINCE_BOOT(key = "SCREEN-ON-PERCENT-SINCE-BOOT", isSupported = true, defaultsRes = R.array.screen_on_percent_since_boot_data_for_notification_default),

        SCREEN_ON_TIME_SINCE_LAST_STATS_RESET(key = "SCREEN-ON-TIME-SINCE-LAST-STATS-RESET", isSupported = true, defaultsRes = R.array.screen_on_time_since_last_stats_reset_data_for_notification_default),
        SCREEN_ON_PERCENT_SINCE_LAST_STATS_RESET(key = "SCREEN-ON-PERCENT-SINCE-LAST-STATS-RESET", isSupported = true, defaultsRes = R.array.screen_on_percent_since_last_stats_reset_data_for_notification_default),
        UPTIME_SINCE_BOOT(key = "UPTIME-SINCE-BOOT", isSupported = true, defaultsRes = R.array.uptime_since_boot_data_for_notification_default),
        UPTIME_SINCE_LAST_STATS_RESET(key = "UPTIME-SINCE-LAST-STATS-RESET", isSupported = true, defaultsRes = R.array.uptime_since_last_stats_reset_data_for_notification_default),
        BATTERY_CHARGING_TIME(key = "BATTERY-CHARGING-TIME", isSupported = true, defaultsRes = R.array.charging_time_data_for_notification_default);

        companion object {
            private val map = entries.associateBy { it.key.uppercase() }

            fun fromKey(key: String?): NotificationField? = map[key?.uppercase()]

            fun NotificationField.getLabel(context: Context, small: Boolean): String =
                when (this) {
                    CPU_DEEP_SLEEP_TIME_SINCE_BOOT -> {
                        val resource = if (small) R.string.cpu_deep_sleep_time else R.string.cpu_deep_sleep_time_long

                        context.getString(resource, context.getString(R.string.since_boot))
                    }
                    CPU_DEEP_SLEEP_PERCENT_SINCE_BOOT -> {
                        val resource = if (small) R.string.cpu_deep_sleep_percent else R.string.cpu_deep_sleep_percent_long

                        context.getString(resource, context.getString(R.string.since_boot))
                    }
                    CPU_DEEP_SLEEP_TIME_SINCE_LAST_STATS_RESET -> {
                        val resource = if (small) R.string.cpu_deep_sleep_time else R.string.cpu_deep_sleep_time_long

                        context.getString(resource, context.getString(R.string.since_last_stats_reset))
                    }
                    CPU_DEEP_SLEEP_PERCENT_SINCE_LAST_STATS_RESET -> {
                        val resource = if (small) R.string.cpu_deep_sleep_percent else R.string.cpu_deep_sleep_percent_long

                        context.getString(resource, context.getString(R.string.since_last_stats_reset))
                    }
                    SCREEN_ON_TIME_SINCE_BOOT -> {
                        val resource = if (small) R.string.screen_on_time else R.string.screen_on_time_long

                        context.getString(resource, context.getString(R.string.since_boot))
                    }
                    SCREEN_ON_PERCENT_SINCE_BOOT -> {
                        val resource = if (small) R.string.screen_on_percent else R.string.screen_on_percent_long

                        context.getString(resource, context.getString(R.string.since_boot))
                    }
                    SCREEN_ON_TIME_SINCE_LAST_STATS_RESET -> {
                        val resource = if (small) R.string.screen_on_time else R.string.screen_on_time_long

                        context.getString(resource, context.getString(R.string.since_last_stats_reset))
                    }
                    SCREEN_ON_PERCENT_SINCE_LAST_STATS_RESET -> {
                        val resource = if (small) R.string.screen_on_percent else R.string.screen_on_percent_long

                        context.getString(resource, context.getString(R.string.since_last_stats_reset))
                    }
                    UPTIME_SINCE_BOOT -> {
                        val resource = if (small) R.string.uptime else R.string.uptime_long

                        context.getString(resource, context.getString(R.string.since_boot))
                    }
                    UPTIME_SINCE_LAST_STATS_RESET -> {
                        val resource = if (small) R.string.uptime else R.string.uptime_long

                        context.getString(resource, context.getString(R.string.since_last_stats_reset))
                    }
                    BATTERY_CHARGING_TIME -> {
                        val resource = if (small) R.string.time_charging else R.string.time_charging_long

                        context.getString(resource)
                    }
                    else -> getLabel(context, key)
                }
        }
    }

    fun isValid(notificationField: NotificationField): Boolean {
        if (!notificationField.isSupported) return false

        return when (notificationField) {
            NotificationField.CPU_DEEP_SLEEP_TIME_SINCE_BOOT,
            NotificationField.CPU_DEEP_SLEEP_PERCENT_SINCE_BOOT -> deepSleepTimeSinceBoot >= 0L

            NotificationField.CPU_DEEP_SLEEP_TIME_SINCE_LAST_STATS_RESET,
            NotificationField.CPU_DEEP_SLEEP_PERCENT_SINCE_LAST_STATS_RESET -> deepSleepTimeSinceLastStatsReset >= 0L

            NotificationField.SCREEN_ON_TIME_SINCE_BOOT,
            NotificationField.SCREEN_ON_PERCENT_SINCE_BOOT -> screenOnTimeSinceBoot >= 0L

            NotificationField.SCREEN_ON_TIME_SINCE_LAST_STATS_RESET,
            NotificationField.SCREEN_ON_PERCENT_SINCE_LAST_STATS_RESET -> screenOnTimeSinceLastStatsReset >= 0L

            NotificationField.UPTIME_SINCE_BOOT -> timeSinceBoot >= 0L
            NotificationField.UPTIME_SINCE_LAST_STATS_RESET -> timeSinceLastStatsReset >= 0L
            NotificationField.BATTERY_CHARGING_TIME -> isCharging && lastBatteryEventTime != 0L

            else -> isValid(notificationField.key)
        }
    }

    fun isVisible(notificationField: NotificationField, prefs: NotificationPreferences): Boolean =
        when (notificationField) {
            NotificationField.CPU_DEEP_SLEEP_PERCENT_SINCE_BOOT -> !prefs.isFieldVisible(NotificationField.CPU_DEEP_SLEEP_TIME_SINCE_BOOT)
            NotificationField.CPU_DEEP_SLEEP_PERCENT_SINCE_LAST_STATS_RESET -> !prefs.isFieldVisible(NotificationField.CPU_DEEP_SLEEP_TIME_SINCE_LAST_STATS_RESET)
            NotificationField.SCREEN_ON_PERCENT_SINCE_BOOT -> !prefs.isFieldVisible(NotificationField.SCREEN_ON_TIME_SINCE_BOOT)
            NotificationField.SCREEN_ON_PERCENT_SINCE_LAST_STATS_RESET -> !prefs.isFieldVisible(NotificationField.SCREEN_ON_TIME_SINCE_LAST_STATS_RESET)
            NotificationField.BATTERY_CHARGING_TIME -> isCharging && lastBatteryEventTime != 0L
            else -> true
        }

    fun toString(context: Context, notificationField: NotificationField, prefs: NotificationPreferences, appPrefs: AppPreferences): String =
        when(notificationField) {
            NotificationField.CPU_DEEP_SLEEP_TIME_SINCE_BOOT -> {
                if (prefs.isFieldVisible(NotificationField.CPU_DEEP_SLEEP_PERCENT_SINCE_BOOT))
                    getStringTimeAndPercentFromInterval(context, deepSleepTimeSinceBoot, timeSinceBoot)
                else
                    getStringTimeFromInterval(context, deepSleepTimeSinceBoot)
            }
            NotificationField.CPU_DEEP_SLEEP_PERCENT_SINCE_BOOT -> {
                getStringPercentFromInterval(context, deepSleepTimeSinceBoot, timeSinceBoot)
            }
            NotificationField.CPU_DEEP_SLEEP_TIME_SINCE_LAST_STATS_RESET -> {
                if (prefs.isFieldVisible(NotificationField.CPU_DEEP_SLEEP_PERCENT_SINCE_LAST_STATS_RESET))
                    getStringTimeAndPercentFromInterval(context, deepSleepTimeSinceLastStatsReset, timeSinceLastStatsReset)
                else
                    getStringTimeFromInterval(context, deepSleepTimeSinceLastStatsReset)
            }
            NotificationField.CPU_DEEP_SLEEP_PERCENT_SINCE_LAST_STATS_RESET -> {
                getStringPercentFromInterval(context, deepSleepTimeSinceLastStatsReset, timeSinceLastStatsReset)
            }
            NotificationField.SCREEN_ON_TIME_SINCE_BOOT -> {
                if (prefs.isFieldVisible(NotificationField.SCREEN_ON_PERCENT_SINCE_BOOT))
                    getStringTimeAndPercentFromInterval(context, screenOnTimeSinceBoot, timeSinceBoot)
                else
                    getStringTimeFromInterval(context, screenOnTimeSinceBoot)
            }
            NotificationField.SCREEN_ON_PERCENT_SINCE_BOOT -> {
                getStringPercentFromInterval(context, screenOnTimeSinceBoot, timeSinceBoot)
            }
            NotificationField.SCREEN_ON_TIME_SINCE_LAST_STATS_RESET -> {
                if (prefs.isFieldVisible(NotificationField.SCREEN_ON_PERCENT_SINCE_LAST_STATS_RESET))
                    getStringTimeAndPercentFromInterval(context, screenOnTimeSinceLastStatsReset, timeSinceLastStatsReset)
                else
                    getStringTimeFromInterval(context, screenOnTimeSinceLastStatsReset)
            }
            NotificationField.SCREEN_ON_PERCENT_SINCE_LAST_STATS_RESET -> {
                getStringPercentFromInterval(context, screenOnTimeSinceLastStatsReset, timeSinceLastStatsReset)
            }
            NotificationField.UPTIME_SINCE_BOOT -> {
                getStringTimeFromInterval(context, timeSinceBoot)
            }
            NotificationField.UPTIME_SINCE_LAST_STATS_RESET -> {
                getStringTimeFromInterval(context, timeSinceLastStatsReset)
            }
            NotificationField.BATTERY_CHARGING_TIME -> {
                val timeCharging = timeSinceBoot - lastBatteryEventTime

                getStringTimeFromInterval(context, timeCharging)
            }
            else -> super.toString(context, notificationField.key, appPrefs, false)
        }
}
