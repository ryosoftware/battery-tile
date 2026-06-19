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
import com.ryosoftware.battery_tile.TemperatureUnit.Companion.fromCelsius
import kotlin.math.roundToInt

class BatteryTileBatteryIntentHelper(
    intent: Intent,
) : BatteryIntentHelper(intent) {
    enum class BatteryTileField(val key: String, val iconizable: Boolean, val textualizable: Boolean, val isSupported: Boolean, @param:ArrayRes val defaultsRes: Int) {
        BATTERY_LEVEL(key = BatteryIntentHelper.BATTERY_LEVEL, iconizable = true, textualizable = true, isSupported = BatteryIntentHelper.isSupported(BatteryIntentHelper.BATTERY_LEVEL), defaultsRes = R.array.level_data_for_tile_default),
        BATTERY_LEVEL_ICON(key = "BATTERY-LEVEL-ICON", iconizable = true, textualizable = false, isSupported = BatteryIntentHelper.isSupported(BatteryIntentHelper.BATTERY_LEVEL), defaultsRes = 0),
        BATTERY_STATUS(key = BatteryIntentHelper.BATTERY_STATUS, iconizable = true, textualizable = true, isSupported = BatteryIntentHelper.isSupported(BatteryIntentHelper.BATTERY_STATUS), defaultsRes = R.array.status_data_for_tile_default),
        BATTERY_TEMPERATURE(key = BatteryIntentHelper.BATTERY_TEMPERATURE, iconizable = true, textualizable = true, isSupported = BatteryIntentHelper.isSupported(BatteryIntentHelper.BATTERY_TEMPERATURE), defaultsRes = R.array.temperature_data_for_tile_default),
        BATTERY_VOLTAGE(key = BatteryIntentHelper.BATTERY_VOLTAGE, iconizable = false, textualizable = true, isSupported = BatteryIntentHelper.isSupported(BatteryIntentHelper.BATTERY_VOLTAGE), defaultsRes = R.array.voltage_data_for_tile_default),
        BATTERY_HEALTH(key = BatteryIntentHelper.BATTERY_HEALTH, iconizable = false, textualizable = true, isSupported = BatteryIntentHelper.isSupported(BatteryIntentHelper.BATTERY_HEALTH), defaultsRes = R.array.health_data_for_tile_default),
        BATTERY_CYCLES_COUNT(key = BatteryIntentHelper.BATTERY_CYCLES_COUNT, iconizable = false, textualizable = true, isSupported = BatteryIntentHelper.isSupported(BatteryIntentHelper.BATTERY_CYCLES_COUNT), defaultsRes = R.array.cycles_count_data_for_tile_default),
        CPU_DEEP_SLEEP_PERCENT_SINCE_BOOT(key = "CPU-DEEP-SLEEP-PERCENT-SINCE-BOOT", iconizable = true, textualizable = true, isSupported = true, defaultsRes = R.array.deep_sleep_data_for_tile_default);

        companion object {
            private val map = entries.associateBy { it.key.uppercase() }

            fun fromKey(key: String?): BatteryTileField? = map[key?.uppercase()]
            fun BatteryTileField.getLabel(context: Context): String =
                when (this) {
                    BATTERY_LEVEL_ICON -> context.getString(R.string.battery_level_icon)
                    CPU_DEEP_SLEEP_PERCENT_SINCE_BOOT -> context.getString(R.string.cpu_deep_sleep_percent, context.getString(R.string.since_boot))
                    else -> getLabel(context, key)
                }
        }
    }

    val smallIcon = intent.getIntExtra(BatteryManager.EXTRA_ICON_SMALL, 0)

    val deepSleepPercent: Float by lazy {
        val elapsed = SystemClock.elapsedRealtime()
        val uptime = SystemClock.uptimeMillis()
        val deepSleepTime = elapsed - uptime

        ((deepSleepTime.toFloat() / elapsed.toFloat()) * 100).coerceIn(0f, 100f)
    }

    fun isValid(batteryTileField: BatteryTileField): Boolean {
        if (! batteryTileField.isSupported) return false

        return when (batteryTileField) {
            BatteryTileField.BATTERY_LEVEL_ICON -> isValid(BatteryIntentHelper.BATTERY_LEVEL, level)
            BatteryTileField.CPU_DEEP_SLEEP_PERCENT_SINCE_BOOT -> deepSleepPercent >= 0
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
            BatteryTileField.CPU_DEEP_SLEEP_PERCENT_SINCE_BOOT -> {
                getIconFromString(deepSleepPercent.roundToInt().toString())
            }
            else -> {
                null
            }
        }

    fun toString(context: Context, batteryTileField: BatteryTileField, appPrefs: AppPreferences): String =
        when(batteryTileField) {
            BatteryTileField.CPU_DEEP_SLEEP_PERCENT_SINCE_BOOT -> {
                val hasNoDecimals = deepSleepPercent % 1f == 0f

                if (hasNoDecimals) context.getString(R.string.percent_value_integer, deepSleepPercent.toInt())
                else context.getString(R.string.percent_value_float, deepSleepPercent)
            }
            else -> super.toString(context, batteryTileField.key, appPrefs, true)
        }
}

