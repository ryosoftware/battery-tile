package com.ryosoftware.battery_tile

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.graphics.Canvas
import androidx.core.graphics.createBitmap
import android.graphics.Paint
import android.graphics.Color
import android.os.BatteryManager
import android.os.SystemClock
import androidx.annotation.ArrayRes
import com.ryosoftware.battery_tile.NotificationBatteryIntentHelper.NotificationField
import com.ryosoftware.battery_tile.TemperatureUnit.Companion.fromCelsius
import kotlin.math.roundToInt

class BatteryTileBatteryIntentHelper(
    intent: Intent,
    private val lastStatsResetTime: Long,
    private val deepSleepTimeAtLastStatsReset: Long,
    private val screenOnTimeSinceBoot: Long,
    private val screenOnTimeSinceLastStatsReset: Long,
) : BatteryIntentHelper(intent) {
    enum class BatteryTileField(val key: String, val iconizable: Boolean, val textualizable: Boolean, val isSupported: Boolean, val requiresBackgroundService: Boolean, @param:ArrayRes val defaultsRes: Int) {
        BATTERY_LEVEL(key = BatteryIntentHelper.BATTERY_LEVEL, iconizable = true, textualizable = true, isSupported = BatteryIntentHelper.isSupported(BatteryIntentHelper.BATTERY_LEVEL), requiresBackgroundService = false, defaultsRes = R.array.level_data_for_tile_default),
        BATTERY_LEVEL_ICON(key = "BATTERY-LEVEL-ICON", iconizable = true, textualizable = false, isSupported = BatteryIntentHelper.isSupported(BatteryIntentHelper.BATTERY_LEVEL), requiresBackgroundService = false, defaultsRes = 0),
        BATTERY_STATUS(key = BatteryIntentHelper.BATTERY_STATUS, iconizable = true, textualizable = true, isSupported = BatteryIntentHelper.isSupported(BatteryIntentHelper.BATTERY_STATUS), requiresBackgroundService = false, defaultsRes = R.array.status_data_for_tile_default),
        BATTERY_TEMPERATURE(key = BatteryIntentHelper.BATTERY_TEMPERATURE, iconizable = true, textualizable = true, isSupported = BatteryIntentHelper.isSupported(BatteryIntentHelper.BATTERY_TEMPERATURE), requiresBackgroundService = false, defaultsRes = R.array.temperature_data_for_tile_default),
        BATTERY_VOLTAGE(key = BatteryIntentHelper.BATTERY_VOLTAGE, iconizable = false, textualizable = true, isSupported = BatteryIntentHelper.isSupported(BatteryIntentHelper.BATTERY_VOLTAGE), requiresBackgroundService = false, defaultsRes = R.array.voltage_data_for_tile_default),
        BATTERY_HEALTH(key = BatteryIntentHelper.BATTERY_HEALTH, iconizable = false, textualizable = true, isSupported = BatteryIntentHelper.isSupported(BatteryIntentHelper.BATTERY_HEALTH), requiresBackgroundService = false, defaultsRes = R.array.health_data_for_tile_default),
        BATTERY_CYCLES_COUNT(key = BatteryIntentHelper.BATTERY_CYCLES_COUNT, iconizable = false, textualizable = true, isSupported = BatteryIntentHelper.isSupported(BatteryIntentHelper.BATTERY_CYCLES_COUNT), requiresBackgroundService = false, defaultsRes = R.array.cycles_count_data_for_tile_default),
        CPU_DEEP_SLEEP_PERCENT_SINCE_BOOT(key = "CPU-DEEP-SLEEP-PERCENT-SINCE-BOOT", iconizable = false, textualizable = true, isSupported = true, requiresBackgroundService = false, defaultsRes = R.array.deep_sleep_percent_since_boot_data_for_tile_default),
        CPU_DEEP_SLEEP_PERCENT_SINCE_LAST_STATS_RESET(key = "CPU-DEEP-SLEEP-PERCENT-SINCE-LAST-STATS-RESET", iconizable = false, textualizable = true, isSupported = true, requiresBackgroundService = true, defaultsRes = R.array.deep_sleep_percent_since_last_stats_reset_data_for_tile_default),
        SCREEN_ON_PERCENT_SINCE_BOOT(key = "SCREEN-ON-PERCENT-SINCE-BOOT", iconizable = false, textualizable = true, isSupported = true, requiresBackgroundService = true, defaultsRes = R.array.screen_on_percent_since_boot_data_for_tile_default),
        SCREEN_ON_PERCENT_SINCE_LAST_STATS_RESET(key = "SCREEN-ON-PERCENT-SINCE-LAST-STATS-RESET", iconizable = false, textualizable = true, isSupported = true, requiresBackgroundService = true, defaultsRes = R.array.screen_on_percent_since_last_stats_reset_data_for_tile_default);

        companion object {
            private val map = entries.associateBy { it.key.uppercase() }

            fun fromKey(key: String?): BatteryTileField? = map[key?.uppercase()]
            fun BatteryTileField.getLabel(context: Context): String =
                when (this) {
                    BATTERY_LEVEL_ICON -> context.getString(R.string.battery_level_icon)
                    CPU_DEEP_SLEEP_PERCENT_SINCE_BOOT -> context.getString(R.string.cpu_deep_sleep_percent_long, context.getString(R.string.since_boot))
                    CPU_DEEP_SLEEP_PERCENT_SINCE_LAST_STATS_RESET ->  context.getString(R.string.cpu_deep_sleep_percent_long, context.getString(R.string.since_last_stats_reset))
                    SCREEN_ON_PERCENT_SINCE_BOOT -> context.getString(R.string.screen_on_percent_long, context.getString(R.string.since_boot))
                    SCREEN_ON_PERCENT_SINCE_LAST_STATS_RESET ->  context.getString(R.string.screen_on_percent_long, context.getString(R.string.since_last_stats_reset))
                    else -> getLabel(context, key)
                }
        }
    }

    companion object {
        private fun getStringPercentFromInterval(context: Context, interval: Long, total: Long): String {
            val percent = if (total == 0L) 0f else (interval * 100f / total).coerceIn(0f, 100f)

            val hasNoDecimals = percent % 1f == 0f

            return if (hasNoDecimals) context.getString(R.string.percent_value_integer, percent.toInt())
            else context.getString(R.string.percent_value_float, percent)
        }
    }

    val smallIcon = intent.getIntExtra(BatteryManager.EXTRA_ICON_SMALL, 0)

    val timeSinceBoot: Long by lazy { SystemClock.elapsedRealtime() }
    val deepSleepTimeSinceBoot: Long by lazy { SystemClock.elapsedRealtime() - SystemClock.uptimeMillis() }

    val timeSinceLastStatsReset: Long by lazy { System.currentTimeMillis() - lastStatsResetTime }

    val deepSleepTimeSinceLastStatsReset: Long by lazy { deepSleepTimeSinceBoot - deepSleepTimeAtLastStatsReset }

    fun isValid(batteryTileField: BatteryTileField): Boolean {
        if (! batteryTileField.isSupported) return false

        return when (batteryTileField) {
            BatteryTileField.BATTERY_LEVEL_ICON -> isValid(BatteryIntentHelper.BATTERY_LEVEL, level)
            BatteryTileField.CPU_DEEP_SLEEP_PERCENT_SINCE_BOOT -> deepSleepTimeSinceBoot >= 0L
            BatteryTileField.CPU_DEEP_SLEEP_PERCENT_SINCE_LAST_STATS_RESET -> deepSleepTimeSinceLastStatsReset >= 0L
            BatteryTileField.SCREEN_ON_PERCENT_SINCE_BOOT -> screenOnTimeSinceBoot >= 0L
            BatteryTileField.SCREEN_ON_PERCENT_SINCE_LAST_STATS_RESET -> screenOnTimeSinceLastStatsReset >= 0L
            else -> isValid(batteryTileField.key)
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

    fun toString(context: Context, batteryTileField: BatteryTileField, appPrefs: AppPreferences): String =
        when(batteryTileField) {
            BatteryTileField.CPU_DEEP_SLEEP_PERCENT_SINCE_BOOT -> {
                getStringPercentFromInterval(
                    context,
                    deepSleepTimeSinceBoot,
                    timeSinceBoot
                )
            }
            BatteryTileField.CPU_DEEP_SLEEP_PERCENT_SINCE_LAST_STATS_RESET -> {
                getStringPercentFromInterval(
                    context,
                    deepSleepTimeSinceLastStatsReset,
                    timeSinceLastStatsReset
                )
            }
            BatteryTileField.SCREEN_ON_PERCENT_SINCE_BOOT -> {
                getStringPercentFromInterval(
                    context,
                    screenOnTimeSinceBoot,
                    timeSinceBoot
                )
            }
            BatteryTileField.SCREEN_ON_PERCENT_SINCE_LAST_STATS_RESET -> {
                getStringPercentFromInterval(
                    context,
                    screenOnTimeSinceLastStatsReset,
                    timeSinceLastStatsReset
                )
            }
            else -> super.toString(context, batteryTileField.key, appPrefs, true)
        }
}

