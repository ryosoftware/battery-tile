package com.ryosoftware.battery_tile

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.ryosoftware.battery_tile.TemperatureUnit.Companion.fromCelsius
import com.ryosoftware.battery_tile.TemperatureUnit.Companion.toCelsius

class NotificationPreferences(context: Context) {
    companion object {
        private const val FILENAME = "notification_prefs"
        const val KEY_NOTIFICATION_ENABLED = "notification-enabled"
        const val KEY_FIELD_POSITION_PREFIX = "notification-field-position-"
        const val KEY_FIELD_VISIBLE_PREFIX = "notification-field-visible-"
        const val KEY_BATTERY_CHARGED_PERCENT = "battery-charged-percent"
        const val KEY_BATTERY_CHARGED_INTERVAL_MINUTES = "battery-charged-minutes"
        const val KEY_BATTERY_CHARGED_REPEAT_INTERVAL_MINUTES = "battery-charged-repeat-minutes"

        const val KEY_BATTERY_LOW_CHARGE_PERCENT = "battery-low-charge-percent"
        const val KEY_BATTERY_LOW_CHARGE_REPEAT_INTERVAL_MINUTES = "battery-low-charge-repeat-minutes"

        const val KEY_BLOCK_POWER_DISCONNECT_NOTIFICATION_WHEN_BATTERY_CHARGED = "block-power-disconnect-notification-when-battery-charged"
        const val KEY_AUTO_RESET_STATS_ENABLED = "auto-reset-stats-enabled"
        const val KEY_STATS_RESET_AFTER_BATTERY_THRESHOLD_PERCENT = "stats-reset-after-battery-threshold-percent"
        const val KEY_STATS_RESET_AFTER_BATTERY_CHARGED_MINUTES = "stats-reset-after-battery-charge-minutes"
        const val KEY_BATTERY_TEMPERATURE_THRESHOLD_IN_CELSIUS = "battery-temperature-threshold-in-celsius"
    }

    private val resources = context.resources

    private val prefs: SharedPreferences =
        context.getSharedPreferences(FILENAME, Context.MODE_PRIVATE)

    private fun getKey(prefix: String, field: NotificationServiceUIBuilder.NotificationField) = "$prefix${field.key.lowercase()}"

    private fun getStringFromNotificationField(field: NotificationServiceUIBuilder.NotificationField, index: Int): String? =
        if (field.defaultsRes == 0) null else resources.getStringArray(field.defaultsRes).getOrNull(index)

    private fun isFieldVisibleDefault(notificationField: NotificationServiceUIBuilder.NotificationField): Boolean {
        val defaultValue = getStringFromNotificationField(notificationField, 1)

        return defaultValue?.toBoolean() ?: false
    }

    fun isFieldVisible(notificationField: NotificationServiceUIBuilder.NotificationField): Boolean =
        prefs.getBoolean(getKey(KEY_FIELD_VISIBLE_PREFIX, notificationField), isFieldVisibleDefault(notificationField))

    fun setFieldVisible(notificationField: NotificationServiceUIBuilder.NotificationField, visible: Boolean) =
        prefs.edit { putBoolean(getKey(KEY_FIELD_VISIBLE_PREFIX, notificationField), visible) }

    private fun getFieldPositionDefault(notificationField: NotificationServiceUIBuilder.NotificationField): Int {
        val defaultValue = getStringFromNotificationField(notificationField, 0)

        return defaultValue?.toInt() ?: Int.MAX_VALUE
    }

    fun getFieldPosition(notificationField: NotificationServiceUIBuilder.NotificationField): Int =
        prefs.getInt(getKey(KEY_FIELD_POSITION_PREFIX, notificationField), getFieldPositionDefault(notificationField))

    fun setFieldPosition(notificationField: NotificationServiceUIBuilder.NotificationField, position: Int) =
        prefs.edit { putInt(getKey(KEY_FIELD_POSITION_PREFIX, notificationField), position) }

    var isNotificationEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIFICATION_ENABLED, resources.getBoolean(R.bool.notification_enabled_default))
        set(enabled) { prefs.edit { putBoolean(KEY_NOTIFICATION_ENABLED, enabled) } }

    var notificationChargedPercent: Int
        get() = prefs.getInt(KEY_BATTERY_CHARGED_PERCENT, resources.getInteger(R.integer.battery_charged_percent_default))
        set(percent) { prefs.edit { putInt(KEY_BATTERY_CHARGED_PERCENT, percent) } }

    var notificationChargedInterval: Int
        get() = prefs.getInt(KEY_BATTERY_CHARGED_INTERVAL_MINUTES, resources.getInteger(R.integer.battery_charged_delay_default))
        set(minutes) { prefs.edit { putInt(KEY_BATTERY_CHARGED_INTERVAL_MINUTES, minutes) } }

    var notificationChargedRepeatInterval: Int
        get() = prefs.getInt(KEY_BATTERY_CHARGED_REPEAT_INTERVAL_MINUTES, resources.getInteger(R.integer.battery_charged_repeat_interval_default))
        set(minutes) { prefs.edit { putInt(KEY_BATTERY_CHARGED_REPEAT_INTERVAL_MINUTES, minutes) } }

    var notificationLowChargePercent: Int
        get() = prefs.getInt(KEY_BATTERY_LOW_CHARGE_PERCENT, resources.getInteger(R.integer.battery_low_charge_percent_default))
        set(percent) { prefs.edit { putInt(KEY_BATTERY_LOW_CHARGE_PERCENT, percent) } }

    var notificationLowChargeRepeatInterval: Int
        get() = prefs.getInt(KEY_BATTERY_LOW_CHARGE_REPEAT_INTERVAL_MINUTES, resources.getInteger(R.integer.battery_low_charge_repeat_interval_default))
        set(minutes) { prefs.edit { putInt(KEY_BATTERY_LOW_CHARGE_REPEAT_INTERVAL_MINUTES, minutes) } }

    var isBlockingPowerDisconnectNotificationWhenBatteryIsCharged: Boolean
        get() = prefs.getBoolean(KEY_BLOCK_POWER_DISCONNECT_NOTIFICATION_WHEN_BATTERY_CHARGED, resources.getBoolean(R.bool.block_power_disconnect_notification_when_battery_is_charged_default))
        set(enabled) { prefs.edit { putBoolean(KEY_BLOCK_POWER_DISCONNECT_NOTIFICATION_WHEN_BATTERY_CHARGED, enabled) } }

    var isAutoResetStatsEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_RESET_STATS_ENABLED, resources.getBoolean(R.bool.auto_reset_stats_enabled_default))
        set(enabled) { prefs.edit { putBoolean(KEY_AUTO_RESET_STATS_ENABLED, enabled) } }

    var resetStatsBatteryThresholdPercent: Int
        get() = prefs.getInt(KEY_STATS_RESET_AFTER_BATTERY_THRESHOLD_PERCENT, resources.getInteger(R.integer.stats_reset_after_charge_threshold_percent_default))
        set(percent) { prefs.edit { putInt(KEY_STATS_RESET_AFTER_BATTERY_THRESHOLD_PERCENT, percent) } }

    var resetStatsBatteryChargeTime: Int
        get() = prefs.getInt(KEY_STATS_RESET_AFTER_BATTERY_CHARGED_MINUTES, resources.getInteger(R.integer.stats_reset_after_charge_time_minutes_default))
        set(minutes) { prefs.edit { putInt(KEY_STATS_RESET_AFTER_BATTERY_CHARGED_MINUTES, minutes) } }

    fun getBatteryTemperatureThreshold(temperatureUnit: TemperatureUnit): Float {
        val temperature = prefs.getFloat(KEY_BATTERY_TEMPERATURE_THRESHOLD_IN_CELSIUS, resources.getString(R.string.battery_temperature_threshold_in_celsius_default).toFloat())

        return temperatureUnit.fromCelsius(temperature)
    }

    fun setBatteryTemperatureThreshold(temperature: Float, temperatureUnit: TemperatureUnit) {
        val temperatureCelsius = temperatureUnit.toCelsius(temperature)

        prefs.edit { putFloat(KEY_BATTERY_TEMPERATURE_THRESHOLD_IN_CELSIUS, temperatureCelsius) }
    }
}
