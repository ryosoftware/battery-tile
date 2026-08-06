package com.ryosoftware.battery_tile

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.edit
import kotlin.math.roundToInt

enum class BatteryOverlayMode(val key: String) {
    BAR("BAR"),
    RING("RING");

    companion object {
        private val map = BatteryOverlayMode.entries.associateBy { it.key.uppercase() }

        fun fromKey(key: String?): BatteryOverlayMode? = map[key?.uppercase()]
    }
}

class BatteryOverlayPreferences(context: Context): Preferences(context, FILENAME) {
    companion object {
        private const val FILENAME = "battery_overlay_prefs"

        const val KEY_OVERLAY_MODE = "overlay-mode"

        const val KEY_BATTERY_CHARGING_COLOR = "battery-charging-color"

        const val KEY_BATTERY_OVERLAY_ENABLED = "battery-overlay-enabled"

        const val KEY_SEGMENT_START_LEVEL_PREFIX = "segment-start-level-"
        const val KEY_SEGMENT_COLOR_PREFIX = "segment-color-"

        const val KEY_BAR_HEIGHT = "bar-height"

        const val KEY_RING_RADIUS = "ring-radius"
        const val KEY_RING_THICKNESS = "ring-thickness"
        const val KEY_RING_OFFSET_X = "ring-offset-x"
        const val KEY_RING_OFFSET_Y = "ring-offset-y"

        const val KEY_NOTCH_CENTER_X = "notch-center-x"
        const val KEY_NOTCH_CENTER_Y = "notch-center-y"
        const val KEY_NOTCH_WIDTH = "notch-width"

        private const val RING_RADIUS_MARGIN = 2f
    }

    private val resources = context.resources

    var batteryOverlayEnabled: Boolean
        get() = prefs.getBoolean(KEY_BATTERY_OVERLAY_ENABLED, context.resources.getBoolean(R.bool.overlay_enabled_default))
        set(value) { prefs.edit { putBoolean(KEY_BATTERY_OVERLAY_ENABLED, value) } }

    private fun getOverlayModeDefault(): BatteryOverlayMode {
        val value = resources.getString(R.string.overlay_mode_default)
        val overlayMode = BatteryOverlayMode.fromKey(value)

        return overlayMode ?: BatteryOverlayMode.RING
    }

    var overlayMode: BatteryOverlayMode
        get() = prefs.getString(KEY_OVERLAY_MODE, null)
            ?.let(BatteryOverlayMode::fromKey)
            ?: getOverlayModeDefault()
        set(mode) { prefs.edit { putString(KEY_OVERLAY_MODE, mode.key) } }

    var chargingColor: Color
        get() {
            val raw = prefs.all[KEY_BATTERY_CHARGING_COLOR]
            val color = when (raw) {
                is Long -> raw
                is Int -> raw.toLong()
                else -> context.getColor(R.color.battery_charging).toLong()
            }
            return Color(color)
        }
        set(color) = prefs.edit { putLong(KEY_BATTERY_CHARGING_COLOR, color.toArgb().toLong()) }

    val segmentsCount: Int
        get() = 4

    private fun getSegmentKey(prefix: String, index: Int): String = "$prefix$index"

    private fun getSegmentStartLevelDefault(index: Int): Int =
        when (index) {
            0 -> resources.getIntArray(R.array.bar_overlay_battery_high_default)[0]
            1 -> resources.getIntArray(R.array.bar_overlay_battery_good_default)[0]
            2 -> resources.getIntArray(R.array.bar_overlay_battery_low_default)[0]
            else -> 0
        }

    fun getSegmentStartLevel(index: Int): Int =
        prefs.getInt(getSegmentKey(KEY_SEGMENT_START_LEVEL_PREFIX, index), getSegmentStartLevelDefault(index))

    fun setSegmentStartLevel(index: Int, level: Int) {
        if (index < segmentsCount - 1) {
            prefs.edit {
                putInt(getSegmentKey(KEY_SEGMENT_START_LEVEL_PREFIX, index), level)
            }
        }
    }

    fun getSegmentEndLevel(index: Int): Int =
        when (index) {
            0 -> 100
            else -> getSegmentStartLevel(index - 1)
        }

    private fun getSegmentColorDefault(index: Int): Int =
        when (index) {
            0 -> resources.getIntArray(R.array.bar_overlay_battery_high_default)[1]
            1 -> resources.getIntArray(R.array.bar_overlay_battery_good_default)[1]
            2 -> resources.getIntArray(R.array.bar_overlay_battery_low_default)[1]
            else -> context.getColor(R.color.battery_critical)
        }

    fun getSegmentColor(index: Int): Color {
        val argb = prefs.getLong(getSegmentKey(KEY_SEGMENT_COLOR_PREFIX, index), getSegmentColorDefault(index).toLong())
        return Color(argb)
    }

    fun setSegmentColor(index: Int, color: Color) =
        prefs.edit {
            putLong(getSegmentKey(KEY_SEGMENT_COLOR_PREFIX, index), color.toArgb().toLong())
        }

    var barHeightPx: Int
        get() = prefs.getInt(KEY_BAR_HEIGHT, resources.getInteger(R.integer.bar_overlay_height_default))
        set(height: Int) = prefs.edit { putInt(KEY_BAR_HEIGHT, height) }

    private fun getDefaultRingRadiusPx(): Int {
        val notchWidth = prefs.getInt(KEY_NOTCH_WIDTH, -1)
        if (notchWidth >= 0) {
            return (notchWidth / 2f + ringThicknessPx / 2f + RING_RADIUS_MARGIN).roundToInt()
        }
        return resources.getInteger(R.integer.ring_overlay_radius_default)
    }

    var ringRadiusPx: Int
        get() = prefs.getInt(KEY_RING_RADIUS, getDefaultRingRadiusPx())
        set(radius: Int) = prefs.edit { putInt(KEY_RING_RADIUS, radius) }

    var ringThicknessPx: Int
        get() = prefs.getInt(KEY_RING_THICKNESS, resources.getInteger(R.integer.ring_overlay_thickness_default))
        set(thickness: Int) = prefs.edit { putInt(KEY_RING_THICKNESS, thickness) }

    private fun getDefaultRingOffsetXPx(): Int {
        val notchCenterX = prefs.getInt(KEY_NOTCH_CENTER_X, -1)
        if (notchCenterX >= 0) return notchCenterX

        return resources.displayMetrics.widthPixels / 2
    }

    private fun getDefaultRingOffsetYPx(): Int {
        val notchCenterY = prefs.getInt(KEY_NOTCH_CENTER_Y, -1)
        if (notchCenterY >= 0) return notchCenterY

        @SuppressLint("DiscouragedApi", "InternalInsetResource")
        val statusBarHeight = runCatching {
            val res = resources
            val id = res.getIdentifier("status_bar_height", "dimen", "android")
            res.getDimensionPixelSize(id).toFloat()
        }.getOrDefault(0f)

        return (statusBarHeight / 2f).roundToInt()
    }

    var ringOffsetXPx: Int
        get() = prefs.getInt(KEY_RING_OFFSET_X, getDefaultRingOffsetXPx())
        set(offset: Int) = prefs.edit { putInt(KEY_RING_OFFSET_X, offset) }

    var ringOffsetYPx: Int
        get() = prefs.getInt(KEY_RING_OFFSET_Y, getDefaultRingOffsetYPx())
        set(offset: Int) = prefs.edit { putInt(KEY_RING_OFFSET_Y, offset) }

    var notchCenterX: Int
        get() = prefs.getInt(KEY_NOTCH_CENTER_X, -1)
        set(value: Int) = prefs.edit { putInt(KEY_NOTCH_CENTER_X, value) }

    var notchCenterY: Int
        get() = prefs.getInt(KEY_NOTCH_CENTER_Y, -1)
        set(value: Int) = prefs.edit { putInt(KEY_NOTCH_CENTER_Y, value) }

    var notchWidthPx: Int
        get() = prefs.getInt(KEY_NOTCH_WIDTH, -1)
        set(value: Int) = prefs.edit { putInt(KEY_NOTCH_WIDTH, value) }
}
