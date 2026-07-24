package com.ryosoftware.battery_tile

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.os.Build
import com.ryosoftware.battery_tile.TemperatureUnit.Companion.fromCelsius
import com.ryosoftware.battery_tile.TemperatureUnit.Companion.toString
import kotlin.math.abs

open class BatteryIntentHelper(context: Context, intent: Intent, batteryManager: BatteryManager?) {
    val level: Int
    val charge: Long
    val currentConsumption: Int
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

    companion object {
        const val BATTERY_LEVEL = "BATTERY-LEVEL"

        const val BATTERY_CHARGE = "BATTERY-CHARGE"

        const val BATTERY_CURRENT_CONSUMPTION = "BATTERY-CURRENT-CONSUMPTION"
        const val BATTERY_STATUS = "BATTERY-STATUS"
        const val BATTERY_HEALTH = "BATTERY-HEALTH"
        const val BATTERY_TEMPERATURE = "BATTERY-TEMPERATURE"
        const val BATTERY_VOLTAGE = "BATTERY-VOLTAGE"
        const val BATTERY_PLUG_TYPE = "BATTERY-PLUG-TYPE"
        const val BATTERY_TECHNOLOGY = "BATTERY-TECHNOLOGY"
        const val BATTERY_CYCLES_COUNT = "BATTERY-CYCLES-COUNT"
        fun isSupported(key: String): Boolean =
            when (key) {
                BATTERY_CYCLES_COUNT -> Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
                else -> true
            }

        fun getLabel(context: Context, key: String): String =
            when (key) {
                BATTERY_LEVEL -> context.getString(R.string.battery_level)
                BATTERY_CHARGE -> context.getString(R.string.remaining_charge)
                BATTERY_CURRENT_CONSUMPTION -> context.getString(R.string.current_consumption)
                BATTERY_STATUS -> context.getString(R.string.battery_status)
                BATTERY_PLUG_TYPE -> context.getString(R.string.battery_plug_type)
                BATTERY_TEMPERATURE -> context.getString(R.string.battery_temperature)
                BATTERY_VOLTAGE -> context.getString(R.string.battery_voltage)
                BATTERY_HEALTH -> context.getString(R.string.battery_health)
                BATTERY_TECHNOLOGY -> context.getString(R.string.battery_technology)
                BATTERY_CYCLES_COUNT -> context.getString(R.string.battery_cycles)
                else -> ""
            }

        private var batteryCapacityDesign: Int = -1

        private fun calculateBatteryCapacityDesign(context: Context): Int =
            runCatching {
                @SuppressLint("PrivateApi")
                val cls = Class.forName("com.android.internal.os.PowerProfile")
                val instance = try {
                    cls.getConstructor(Context::class.java).newInstance(context)
                } catch (e: NoSuchMethodException) {
                    cls.getDeclaredConstructor().newInstance()
                }
                when (val result = cls.getMethod("getBatteryCapacity").invoke(instance)) {
                    is Number -> result.toInt()
                    else -> 0
                }
            }.getOrDefault(0)

        fun getBatteryCapacityDesign(context: Context): Int {
            if (batteryCapacityDesign < 0) {
                batteryCapacityDesign = calculateBatteryCapacityDesign(context)
            }

            return batteryCapacityDesign
        }
    }
    init {
        fun getChargeMicroAh(context: Context, charge: Long, level: Int): Long {
            if ((charge == Long.MIN_VALUE) || (charge < 0) || (level < 0)) return -1L

            val batteryCapacityDesign = getBatteryCapacityDesign(context)
            if (batteryCapacityDesign <= 0) return -1L

            val fraction = level / 100.0

            val capacityIfMah = charge / fraction
            val chargeMahIfMicroAh = charge / 1_000.0
            val capacityIfMicroAh = chargeMahIfMicroAh / fraction

            val errorIfMah = abs(capacityIfMah - batteryCapacityDesign)
            val errorIfMicroAh = abs(capacityIfMicroAh - Companion.batteryCapacityDesign)

            return if (errorIfMah < errorIfMicroAh) { charge * 1_000L } else { charge }
        }

        val rawIntentLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val rawIntentLevelScale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)

        level = if (rawIntentLevel >= 0 && rawIntentLevelScale > 0) rawIntentLevel * 100 / rawIntentLevelScale else -1

        val rawCharge = batteryManager?.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER) ?: Long.MIN_VALUE
        charge = getChargeMicroAh(context, rawCharge, level)

        status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)

        plugType = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
        isPlugged = (plugType != 0)

        isCharging = when (status) {
            BatteryManager.BATTERY_STATUS_FULL -> isPlugged
            BatteryManager.BATTERY_STATUS_CHARGING -> true
            else -> false
        }
        isFullCharged = status == BatteryManager.BATTERY_STATUS_FULL

        health = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN)

        val rawTemperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)
        temperatureCelsius = if (rawTemperature < 0) -1f else (rawTemperature / 10f)

        voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)

        val rawCurrentConsumption = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) ?: Int.MIN_VALUE
        currentConsumption = if ((rawCurrentConsumption == Int.MIN_VALUE) || (voltage <= 0)) -1 else abs(rawCurrentConsumption) * voltage / 1_000_000

        present = intent.getBooleanExtra(BatteryManager.EXTRA_PRESENT, false)

        val rawIntentTechnology = intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY)
        technology = if (rawIntentTechnology.isNullOrEmpty()) null else rawIntentTechnology

        @SuppressLint("InlinedApi")
        cyclesCount = if (isSupported(BATTERY_CYCLES_COUNT)) intent.getIntExtra(BatteryManager.EXTRA_CYCLE_COUNT, -1) else -1
    }

    open fun toString(context: Context, key: String, appPrefs: AppPreferences, small: Boolean): String? =
        when (key) {
            BATTERY_LEVEL -> {
                when {
                    level < 0 -> null
                    else -> context.getString(R.string.percent_value_integer, level)
                }
            }
            BATTERY_CHARGE -> {
                when {
                    charge < 0 -> null
                    else -> context.getString(R.string.mah_value, charge / 1_000L)
                }
            }
            BATTERY_CURRENT_CONSUMPTION -> {
                when {
                    currentConsumption < 0 -> null
                    else -> context.getString(R.string.consumption_value, currentConsumption)
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
                    else -> null
                }
            }
            BATTERY_PLUG_TYPE -> {
                when (plugType) {
                    BatteryManager.BATTERY_PLUGGED_AC -> context.getString(R.string.battery_plug_ac)
                    BatteryManager.BATTERY_PLUGGED_DOCK -> context.getString(R.string.battery_plug_dock)
                    BatteryManager.BATTERY_PLUGGED_USB -> context.getString(R.string.battery_plug_usb)
                    BatteryManager.BATTERY_PLUGGED_WIRELESS -> context.getString(R.string.battery_plug_wireless)
                    else -> null
                }
            }
            BATTERY_TEMPERATURE -> {
                when {
                    temperatureCelsius < 0 -> null
                    else -> {
                        val temperature = appPrefs.temperatureUnit.fromCelsius(temperatureCelsius)

                        appPrefs.temperatureUnit.toString(context, temperature)
                    }
                }
            }
            BATTERY_VOLTAGE -> {
                when {
                    voltage < 0 -> null
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
                    else -> null
                }
            }
            BATTERY_TECHNOLOGY -> {
                when {
                    technology.isNullOrEmpty() -> null
                    else -> technology
                }
            }
            BATTERY_CYCLES_COUNT -> {
                when {
                    cyclesCount < 0 -> null
                    else -> context.getString(R.string.battery_cycles_count, cyclesCount)
                }
            }
            else -> null
        }
    }
