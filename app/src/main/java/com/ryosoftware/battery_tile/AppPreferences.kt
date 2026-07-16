package com.ryosoftware.battery_tile

import android.content.Context
import android.content.Intent
import androidx.core.content.edit

enum class TemperatureUnit(val key: String, val resIdName: Int, val resIdSymbol: Int, val resIdSymbolMin: Int) {
    CELSIUS("CELSIUS", R.string.celsius_unit_with_name, R.string.celsius_unit, R.string.celsius_unit_min),
    FAHRENHEIT("FAHRENHEIT", R.string.fahrenheit_unit_with_name, R.string.fahrenheit_unit, R.string.fahrenheit_unit_min),
    KELVIN("KELVIN", R.string.kelvin_unit_with_name, R.string.kelvin_unit, R.string.kelvin_unit_min);

    companion object {
        private val map = entries.associateBy { it.key.uppercase() }

        fun fromKey(key: String?): TemperatureUnit? = map[key?.uppercase()]

        fun TemperatureUnit.fromCelsius(temperature: Float): Float =
            when (this) {
                CELSIUS -> temperature
                FAHRENHEIT -> (temperature * 9f / 5f + 32f)
                KELVIN -> (temperature + 273.15f)
            }

        fun TemperatureUnit.toCelsius(temperature: Float): Float =
            when (this) {
                CELSIUS -> temperature
                FAHRENHEIT -> ((temperature - 32f) * 5f / 9f)
                KELVIN -> (temperature - 273.15f)
            }

        fun TemperatureUnit.toString(context: Context): String =
            context.getString(resIdName)

        fun TemperatureUnit.toString(context: Context, temperature: Float, small: Boolean): String {
            val symbol = context.getString(if (small) resIdSymbolMin else resIdSymbol)

            val hasNoDecimals = temperature % 1f == 0f

            return if (hasNoDecimals) context.getString(R.string.temperature_value_integer, temperature.toInt(), symbol)
                   else context.getString(R.string.temperature_value_float, temperature, symbol)
        }

        fun TemperatureUnit.toString(context: Context, temperature: Float): String = toString(context, temperature,false)
    }
}

enum class WhatAppOpens(val key: String, val resId: Int) {
    APP("APP", R.string.what_opens_app),
    APP_BATTERY_INFO("APP-BATTERY-INFO", R.string.what_opens_app_battery_info),
    SYSTEM_POWER_USAGE_SUMMARY("SYSTEM-POWER-USAGE-SUMMARY", R.string.what_opens_system_power_usage_summary);

    companion object {
        object AppIntentFactory {
            fun create(context: Context, type: WhatAppOpens): Intent =
                when (type) {
                    APP -> Intent(context, MainActivity::class.java)
                    APP_BATTERY_INFO -> Intent(context, BatteryInfoActivity::class.java)
                    SYSTEM_POWER_USAGE_SUMMARY -> Intent(Intent.ACTION_POWER_USAGE_SUMMARY)
                }
        }
        private val map = entries.associateBy { it.key.uppercase() }

        fun fromKey(key: String?): WhatAppOpens? =
            map[key?.uppercase()]

        fun WhatAppOpens.getIntent(context: Context): Intent =
            AppIntentFactory.create(context, this)

        fun WhatAppOpens.toString(context: Context): String =
            context.getString(resId)
    }
}

class AppPreferences(context: Context): Preferences(context, FILENAME) {
    companion object {
        private const val FILENAME = "app_prefs"

        const val KEY_FIRST_RUN = "first-run"

        const val KEY_BATTERY_CAPACITY_DESIGN = "battery-capacity-design"
        const val KEY_BATTERY_CAPACITY_CURRENT = "battery-capacity-current"
        const val KEY_TEMPERATURE_UNIT = "temperature-unit"

        const val KEY_WHAT_APP_OPENS = "what-app-opens"

        const val KEY_LOG_TO_FILE = "log-to-file"
        const val KEY_LOG_ONLY_WHILE_CHARGING = "log-only-while-charging"

        const val KEY_BATTERY_HISTORY_WINDOW = "battery-history-window"
        const val KEY_CHARGING_HISTORY_WINDOW = "charging-history-window"
        const val KEY_DISCHARGING_HISTORY_WINDOW = "discharging-history-window"
    }

    private val resources = context.resources

    var isFirstRun: Boolean
        get() = prefs.getBoolean(KEY_FIRST_RUN, true)
        set(value) { prefs.edit { putBoolean(KEY_FIRST_RUN, value) }
    }

    var batteryCapacityDesign: Int
        get() = prefs.getInt(KEY_BATTERY_CAPACITY_DESIGN, -1)
        set(value) { prefs.edit { putInt(KEY_BATTERY_CAPACITY_DESIGN, value) }}

    var batteryCapacityCurrent: Int
        get() = prefs.getInt(KEY_BATTERY_CAPACITY_CURRENT, 0)
        set(value) { prefs.edit { putInt(KEY_BATTERY_CAPACITY_CURRENT, value) }}

    private fun getTemperatureUnitDefault(): TemperatureUnit {
        val value = resources.getString(R.string.temperature_unit_default)
        val temperatureUnit = TemperatureUnit.fromKey(value)

        return temperatureUnit ?: TemperatureUnit.CELSIUS
    }

    var temperatureUnit: TemperatureUnit
        get() = prefs.getString(KEY_TEMPERATURE_UNIT, null)
            ?.let(TemperatureUnit::fromKey)
            ?: getTemperatureUnitDefault()
        set(unit) { prefs.edit { putString(KEY_TEMPERATURE_UNIT, unit.key) } }

    private fun getWhatAppOpensWhenUserClicksTileOrNotificationDefault(): WhatAppOpens {
        val value = resources.getString(R.string.what_opens_when_user_clicks_tile_or_notification_default)
        val whatAppOpens = WhatAppOpens.fromKey(value)

        return whatAppOpens ?: WhatAppOpens.APP
    }

    var whatAppOpensWhenUserClicksTileOrNotification: WhatAppOpens
        get() = prefs.getString(KEY_WHAT_APP_OPENS, null)
            ?.let(WhatAppOpens::fromKey)
            ?: getWhatAppOpensWhenUserClicksTileOrNotificationDefault()
        set(value) { prefs.edit { putString(KEY_WHAT_APP_OPENS, value.key) } }

    var isLoggingToFile: Boolean
        get() = prefs.getBoolean(KEY_LOG_TO_FILE, BuildConfig.DEBUG)
        set(value) { prefs.edit { putBoolean(KEY_LOG_TO_FILE, value) } }

    var isLoggingOnlyWhileCharging: Boolean
        get() = prefs.getBoolean(KEY_LOG_ONLY_WHILE_CHARGING, resources.getBoolean(R.bool.logging_only_while_charging_default))
        set(value) { prefs.edit { putBoolean(KEY_LOG_ONLY_WHILE_CHARGING, value) } }

    var batteryHistoryWindow: Int
        get() = prefs.getInt(KEY_BATTERY_HISTORY_WINDOW, resources.getInteger(R.integer.battery_history_window_in_days_default))
        set(value) { prefs.edit { putInt(KEY_BATTERY_HISTORY_WINDOW, value) } }

    var chargingHistoryWindow: Int
        get() = prefs.getInt(KEY_CHARGING_HISTORY_WINDOW, resources.getInteger(R.integer.charging_history_window_in_days_default))
        set(value) { prefs.edit { putInt(KEY_CHARGING_HISTORY_WINDOW, value) } }

    var dischargingHistoryWindow: Int
        get() = prefs.getInt(KEY_DISCHARGING_HISTORY_WINDOW, resources.getInteger(R.integer.discharging_history_window_in_days_default))
        set(value) { prefs.edit { putInt(KEY_DISCHARGING_HISTORY_WINDOW, value) } }
}
