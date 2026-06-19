package com.ryosoftware.battery_tile

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.os.Build
import com.ryosoftware.battery_tile.TemperatureUnit.Companion.fromCelsius
import com.ryosoftware.battery_tile.TemperatureUnit.Companion.toString

open class BatteryIntentHelper(intent: Intent) {
    val level: Int
    val status: Int
    val health: Int
    val temperatureCelsius: Float
    val voltage: Int
    val plugType: Int
    val isPlugged: Boolean
    val present: Boolean
    val technology: String?
    val isCharging: Boolean
    val isFullCharged: Boolean
    val cyclesCount: Int
    val capacity: Int

    companion object {
        const val BATTERY_LEVEL = "BATTERY-LEVEL"
        const val BATTERY_STATUS = "BATTERY-STATUS"
        const val BATTERY_HEALTH = "BATTERY-HEALTH"
        const val BATTERY_TEMPERATURE = "BATTERY-TEMPERATURE"
        const val BATTERY_VOLTAGE = "BATTERY-VOLTAGE"
        const val BATTERY_PLUG_TYPE = "BATTERY-PLUG-TYPE"
        const val BATTERY_TECHNOLOGY = "BATTERY-TECHNOLOGY"
        const val BATTERY_CYCLES_COUNT = "BATTERY-CYCLES-COUNT"
        const val BATTERY_CAPACITY = "BATTERY-CAPACITY"
        fun isSupported(key: String): Boolean =
            when (key) {
                BATTERY_CYCLES_COUNT -> Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
                BATTERY_CAPACITY -> Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA
                else -> true
            }

        fun isValid(key: String, value: Any?): Boolean =
            when (key) {
                BATTERY_LEVEL -> (value is Int) && (value >= 0)
                BATTERY_STATUS -> (value is Int) && (value != BatteryManager.BATTERY_STATUS_UNKNOWN)
                BATTERY_HEALTH -> (value is Int) && (value != BatteryManager.BATTERY_HEALTH_UNKNOWN)
                BATTERY_TEMPERATURE -> (value is Float) && (value >= 0)
                BATTERY_VOLTAGE -> (value is Int) && (value >= 0)
                BATTERY_PLUG_TYPE -> (value is Int) && (value > 0)
                BATTERY_TECHNOLOGY -> (value is String) && (value.isNotEmpty())
                BATTERY_CYCLES_COUNT -> (value is Int) && (value >= 0)
                BATTERY_CAPACITY -> (value is Int) && (value >= 0)
                else -> false
            }

        fun getLabel(context: Context, key: String): String =
            when (key) {
                BATTERY_LEVEL -> context.getString(R.string.battery_level)
                BATTERY_STATUS -> context.getString(R.string.battery_status)
                BATTERY_PLUG_TYPE -> context.getString(R.string.battery_plug_type)
                BATTERY_TEMPERATURE -> context.getString(R.string.battery_temperature)
                BATTERY_VOLTAGE -> context.getString(R.string.battery_voltage)
                BATTERY_HEALTH -> context.getString(R.string.battery_health)
                BATTERY_TECHNOLOGY -> context.getString(R.string.battery_technology)
                BATTERY_CYCLES_COUNT -> context.getString(R.string.battery_cycles)
                BATTERY_CAPACITY -> context.getString(R.string.battery_capacity)
                else -> ""
            }
    }

    init {
        val intentLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val intentLevelScale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)

        level = if (intentLevel >= 0 && intentLevelScale > 0) intentLevel * 100 / intentLevelScale else -1

        status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)

        plugType = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
        isPlugged = (plugType != 0)

        isCharging = when (status) {
            BatteryManager.BATTERY_STATUS_CHARGING,
            BatteryManager.BATTERY_STATUS_FULL -> isPlugged
            else -> false
        }
        isFullCharged = status == BatteryManager.BATTERY_STATUS_FULL

        health = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN)

        val intentTemperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)
        temperatureCelsius = if (intentTemperature < 0) -1f else (intentTemperature / 10f)

        voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)

        present = intent.getBooleanExtra(BatteryManager.EXTRA_PRESENT, false)

        val intentTechnology = intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY)
        technology = if (intentTechnology.isNullOrEmpty()) null else intentTechnology

        @SuppressLint("InlinedApi")
        cyclesCount = if (isSupported(BATTERY_CYCLES_COUNT)) intent.getIntExtra(BatteryManager.EXTRA_CYCLE_COUNT, -1) else -1

        @SuppressLint("InlinedApi")
        capacity = if (isSupported(BATTERY_CAPACITY)) intent.getIntExtra(BatteryManager.EXTRA_CAPACITY_LEVEL, BatteryManager.BATTERY_CAPACITY_LEVEL_UNKNOWN) else -1
    }

    open fun isValid(key: String): Boolean =
        when (key) {
            BATTERY_LEVEL -> isValid(BATTERY_LEVEL, level)
            BATTERY_STATUS -> isValid(BATTERY_STATUS, status)
            BATTERY_HEALTH -> isValid(BATTERY_HEALTH, health)
            BATTERY_TEMPERATURE -> isValid(BATTERY_TEMPERATURE, temperatureCelsius)
            BATTERY_VOLTAGE -> isValid(BATTERY_VOLTAGE, voltage)
            BATTERY_PLUG_TYPE -> isValid(BATTERY_PLUG_TYPE, plugType)
            BATTERY_TECHNOLOGY -> isValid(BATTERY_TECHNOLOGY, technology)
            BATTERY_CYCLES_COUNT -> isValid(BATTERY_CYCLES_COUNT, cyclesCount)
            BATTERY_CAPACITY -> isValid(BATTERY_CAPACITY, capacity)
            else -> false
        }

    open fun toString(context: Context, key: String, appPrefs: AppPreferences, small: Boolean): String =
        when (key) {
            BATTERY_LEVEL -> {
                when {
                    level < 0 -> context.getString(R.string.not_available)
                    else -> context.getString(R.string.percent_value_integer, level)
                }
            }
            BATTERY_STATUS -> {
                when (status) {
                    BatteryManager.BATTERY_STATUS_CHARGING -> {
                        val batteryStatusChargingWithAC = if (small) R.string.battery_status_charging_with_plug_ac_short else R.string.battery_status_charging_with_plug_ac
                        val batteryStatusChargingWithDock = if (small) R.string.battery_status_charging_with_plug_dock_short else R.string.battery_status_charging_with_plug_dock
                        val batteryStatusChargingWithUsb = if (small) R.string.battery_status_charging_with_plug_usb_short else R.string.battery_status_charging_with_plug_usb
                        val batteryStatusChargingWithWireless = if (small) R.string.battery_status_charging_with_plug_wireless_short else R.string.battery_status_charging_with_plug_wireless
                        when (plugType) {
                            BatteryManager.BATTERY_PLUGGED_AC -> context.getString(batteryStatusChargingWithAC)
                            BatteryManager.BATTERY_PLUGGED_DOCK -> context.getString(batteryStatusChargingWithDock)
                            BatteryManager.BATTERY_PLUGGED_USB -> context.getString(batteryStatusChargingWithUsb)
                            BatteryManager.BATTERY_PLUGGED_WIRELESS -> context.getString(batteryStatusChargingWithWireless)
                            else -> context.getString(R.string.battery_status_charging)
                        }
                    }
                    BatteryManager.BATTERY_STATUS_DISCHARGING -> context.getString(R.string.battery_status_discharging)
                    BatteryManager.BATTERY_STATUS_NOT_CHARGING -> context.getString(R.string.battery_status_not_charging)
                    BatteryManager.BATTERY_STATUS_FULL -> context.getString(R.string.battery_status_full)
                    else -> context.getString(R.string.battery_status_unknown)
                }
            }
            BATTERY_PLUG_TYPE -> {
                when (plugType) {
                    BatteryManager.BATTERY_PLUGGED_AC -> context.getString(R.string.battery_plug_ac)
                    BatteryManager.BATTERY_PLUGGED_DOCK -> context.getString(R.string.battery_plug_dock)
                    BatteryManager.BATTERY_PLUGGED_USB -> context.getString(R.string.battery_plug_usb)
                    BatteryManager.BATTERY_PLUGGED_WIRELESS -> context.getString(R.string.battery_plug_wireless)
                    else -> context.getString(R.string.not_available)
                }
            }
            BATTERY_TEMPERATURE -> {
                when {
                    temperatureCelsius < 0 -> context.getString(R.string.not_available)
                    else -> {
                        val temperature = appPrefs.temperatureUnit.fromCelsius(temperatureCelsius)

                        appPrefs.temperatureUnit.toString(context, temperature)
                    }
                }
            }
            BATTERY_VOLTAGE -> {
                when {
                    voltage < 0 -> context.getString(R.string.not_available)
                    else -> context.getString(R.string.voltage_value, voltage)
                }
            }
            BATTERY_HEALTH -> {
                when (health) {
                    BatteryManager.BATTERY_HEALTH_GOOD -> context.getString(R.string.battery_health_good)
                    BatteryManager.BATTERY_HEALTH_OVERHEAT -> context.getString(R.string.battery_health_overheat)
                    BatteryManager.BATTERY_HEALTH_DEAD -> context.getString(R.string.battery_health_dead)
                    BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> context.getString(R.string.battery_health_over_voltage)
                    BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> context.getString(R.string.battery_health_failure)
                    BatteryManager.BATTERY_HEALTH_COLD -> context.getString(R.string.battery_health_cold)
                    else -> context.getString(R.string.battery_health_unknown)
                }
            }
            BATTERY_TECHNOLOGY -> if (technology.isNullOrEmpty()) context.getString(R.string.not_available) else technology
            BATTERY_CYCLES_COUNT -> {
                when {
                    cyclesCount < 0 -> context.getString(R.string.not_available)
                    else -> context.getString(R.string.battery_cycles_count, cyclesCount)
                }
            }
            BATTERY_CAPACITY -> {
                when (capacity) {
                    BatteryManager.BATTERY_CAPACITY_LEVEL_FULL -> context.getString(R.string.battery_capacity_full)
                    BatteryManager.BATTERY_CAPACITY_LEVEL_HIGH -> context.getString(R.string.battery_capacity_high)
                    BatteryManager.BATTERY_CAPACITY_LEVEL_NORMAL -> context.getString(R.string.battery_capacity_normal)
                    BatteryManager.BATTERY_CAPACITY_LEVEL_LOW -> context.getString(R.string.battery_capacity_low)
                    BatteryManager.BATTERY_CAPACITY_LEVEL_CRITICAL -> context.getString(R.string.battery_capacity_critical)
                    BatteryManager.BATTERY_CAPACITY_LEVEL_UNKNOWN -> context.getString(R.string.battery_capacity_unknown)
                    else -> context.getString(R.string.battery_capacity_unsupported)
                }
            }
            else -> ""
        }
    }
