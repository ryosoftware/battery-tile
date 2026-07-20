package com.ryosoftware.battery_tile

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.graphics.Canvas
import androidx.core.graphics.createBitmap
import android.graphics.Paint
import android.graphics.Color
import android.os.BatteryManager
import androidx.annotation.ArrayRes
import androidx.annotation.StringRes
import com.ryosoftware.battery_tile.TemperatureUnit.Companion.fromCelsius
import com.ryosoftware.battery_tile.Utils.Companion.getStringPercentFromInterval

class BatteryTileUIBuilder(
    intent: Intent,
    lastStatsResetTime: Long,
    deepSleepTimeAtLastStatsReset: Long,
    screenOnTimeSinceBoot: Long,
    screenOnTimeSinceLastStatsReset: Long,
    batteryManager: BatteryManager
) : BaseUIBuilder(
    intent,
    lastStatsResetTime,
    deepSleepTimeAtLastStatsReset,
    screenOnTimeSinceBoot,
    screenOnTimeSinceLastStatsReset,
    batteryManager) {
    private val smallIcon = intent.getIntExtra(BatteryManager.EXTRA_ICON_SMALL, 0)

    enum class BatteryTileField(val key: String, val iconizable: Boolean, val textualizable: Boolean, val isSupported: Boolean, @param:StringRes val label: Int = 0, @param:StringRes val labelModifier: Int = 0, @param:StringRes val comments: Int = 0, @param:ArrayRes val defaultsRes: Int) {
        BATTERY_LEVEL(key = BatteryIntentHelper.BATTERY_LEVEL, iconizable = true, textualizable = true, isSupported = isSupported(BatteryIntentHelper.BATTERY_LEVEL), defaultsRes = R.array.level_data_for_tile_default),
        BATTERY_LEVEL_ICON(key = "BATTERY-LEVEL-ICON", iconizable = true, textualizable = false, isSupported = isSupported(BatteryIntentHelper.BATTERY_LEVEL), label = R.string.battery_level_icon, defaultsRes = 0),
        BATTERY_CHARGE(key = BatteryIntentHelper.BATTERY_CHARGE, iconizable = false, textualizable = true, isSupported = isSupported(BatteryIntentHelper.BATTERY_CHARGE), defaultsRes = R.array.charge_data_for_tile_default),
        BATTERY_CONSUMPTION(key = BatteryIntentHelper.BATTERY_CURRENT_CONSUMPTION, iconizable = false, textualizable = true, isSupported = isSupported(BatteryIntentHelper.BATTERY_CURRENT_CONSUMPTION), defaultsRes = R.array.consumption_data_for_tile_default),
        BATTERY_STATUS(key = BatteryIntentHelper.BATTERY_STATUS, iconizable = true, textualizable = true, isSupported = isSupported(BatteryIntentHelper.BATTERY_STATUS), defaultsRes = R.array.status_data_for_tile_default),
        BATTERY_TEMPERATURE(key = BatteryIntentHelper.BATTERY_TEMPERATURE, iconizable = true, textualizable = true, isSupported = isSupported(BatteryIntentHelper.BATTERY_TEMPERATURE), defaultsRes = R.array.temperature_data_for_tile_default),
        BATTERY_VOLTAGE(key = BatteryIntentHelper.BATTERY_VOLTAGE, iconizable = false, textualizable = true, isSupported = isSupported(BatteryIntentHelper.BATTERY_VOLTAGE), defaultsRes = R.array.voltage_data_for_tile_default),
        BATTERY_HEALTH(key = BatteryIntentHelper.BATTERY_HEALTH, iconizable = false, textualizable = true, isSupported = isSupported(BatteryIntentHelper.BATTERY_HEALTH), defaultsRes = R.array.health_data_for_tile_default),
        BATTERY_CYCLES_COUNT(key = BatteryIntentHelper.BATTERY_CYCLES_COUNT, iconizable = false, textualizable = true, isSupported = isSupported(BatteryIntentHelper.BATTERY_CYCLES_COUNT), defaultsRes = R.array.cycles_count_data_for_tile_default),
        CPU_DEEP_SLEEP_PERCENT_SINCE_BOOT(key = "CPU-DEEP-SLEEP-PERCENT-SINCE-BOOT", iconizable = false, textualizable = true, isSupported = true, label = R.string.cpu_deep_sleep_percent_long, labelModifier = R.string.since_boot, defaultsRes = R.array.deep_sleep_percent_since_boot_data_for_tile_default),
        CPU_DEEP_SLEEP_PERCENT_SINCE_LAST_STATS_RESET(key = "CPU-DEEP-SLEEP-PERCENT-SINCE-LAST-STATS-RESET", iconizable = false, textualizable = true, isSupported = true, label = R.string.cpu_deep_sleep_percent_long, labelModifier = R.string.since_last_stats_reset, comments = R.string.requires_background_running, defaultsRes = R.array.deep_sleep_percent_since_last_stats_reset_data_for_tile_default),
        SCREEN_ON_PERCENT_SINCE_BOOT(key = "SCREEN-ON-PERCENT-SINCE-BOOT", iconizable = false, textualizable = true, isSupported = true, label = R.string.screen_on_percent_long, labelModifier = R.string.since_boot, comments = R.string.requires_background_running, defaultsRes = R.array.screen_on_percent_since_boot_data_for_tile_default),
        SCREEN_ON_PERCENT_SINCE_LAST_STATS_RESET(key = "SCREEN-ON-PERCENT-SINCE-LAST-STATS-RESET", iconizable = false, textualizable = true, isSupported = true, label = R.string.screen_on_percent_long, labelModifier = R.string.since_last_stats_reset, comments = R.string.requires_background_running, defaultsRes = R.array.screen_on_percent_since_last_stats_reset_data_for_tile_default);

        companion object {
            private val map = entries.associateBy { it.key.uppercase() }

            fun fromKey(key: String?): BatteryTileField? = map[key?.uppercase()]
            fun BatteryTileField.getLabel(context: Context): String =
                when {
                    label == 0 -> getLabel(context, key)
                    labelModifier == 0 -> context.getString(label)
                    else -> context.getString(label, context.getString(labelModifier))
                }

            fun BatteryTileField.getComments(context: Context): String? =
                when {
                    comments == 0 -> null
                    else -> context.getString(comments)
                }
        }
    }

    private fun getIconFromString(text: String): Icon {
        val bitmapSize = 48
        val bitmap = createBitmap(bitmapSize, bitmapSize)
        val canvas = Canvas(bitmap)
        val maxTextWidth = bitmapSize * 0.95f
        val maxTextHeight = bitmapSize * 0.95f
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
            textSize = 56f
            val measuredWidth = measureText(text)
            val fontMetrics = fontMetrics
            val measuredHeight = fontMetrics.descent - fontMetrics.ascent
            val widthScale = maxTextWidth / measuredWidth
            val heightScale = maxTextHeight / measuredHeight
            textSize *= minOf(widthScale, heightScale)
        }
        val x = bitmapSize / 2f
        val y = bitmapSize / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(text, x, y, textPaint)
        return Icon.createWithBitmap(bitmap)
    }

    fun getIcon(context: Context, batteryTileField: BatteryTileField, appPrefs: AppPreferences): Icon? =
        when (batteryTileField) {
            BatteryTileField.BATTERY_LEVEL -> {
                getIconFromString(level.toString())
            }
            BatteryTileField.BATTERY_LEVEL_ICON -> {
                if (smallIcon != 0) Icon.createWithResource(context, smallIcon)
                else getIconFromString(level.toString())
            }
            BatteryTileField.BATTERY_STATUS -> {
                val iconResource =
                    if (isFullCharged) R.drawable.ic_tile_battery_charged
                    else if (isCharging) R.drawable.ic_tile_battery_charging
                    else R.drawable.ic_tile_battery_discharging

                Icon.createWithResource(context, iconResource)
            }
            BatteryTileField.BATTERY_TEMPERATURE -> {
                val temperature = appPrefs.temperatureUnit.fromCelsius(temperatureCelsius)

                getIconFromString(temperature.toString())
            }
            else -> {
                null
            }
        }

    fun toString(context: Context, batteryTileField: BatteryTileField, appPrefs: AppPreferences): String? =
        when(batteryTileField) {
            BatteryTileField.CPU_DEEP_SLEEP_PERCENT_SINCE_BOOT -> {
                if ((deepSleepTimeSinceBoot > 0L) && (timeSinceBoot > 0L)) {
                    getStringPercentFromInterval(context, deepSleepTimeSinceBoot, timeSinceBoot)
                } else null
            }
            BatteryTileField.CPU_DEEP_SLEEP_PERCENT_SINCE_LAST_STATS_RESET -> {
                if ((deepSleepTimeSinceLastStatsReset > 0L) && (timeSinceLastStatsReset > 0L)) {
                    getStringPercentFromInterval(context, deepSleepTimeSinceLastStatsReset, timeSinceLastStatsReset)
                } else null
            }
            BatteryTileField.SCREEN_ON_PERCENT_SINCE_BOOT -> {
                if ((screenOnTimeSinceBoot > 0L) && (timeSinceBoot > 0L)) {
                    getStringPercentFromInterval(context, screenOnTimeSinceBoot, timeSinceBoot)
                } else null
            }
            BatteryTileField.SCREEN_ON_PERCENT_SINCE_LAST_STATS_RESET -> {
                if ((screenOnTimeSinceLastStatsReset > 0L) && (timeSinceLastStatsReset > 0L)) {
                    getStringPercentFromInterval(context, screenOnTimeSinceLastStatsReset, timeSinceLastStatsReset)
                } else null
            }
            else -> super.toString(context, batteryTileField.key, appPrefs, true)
        }
}

